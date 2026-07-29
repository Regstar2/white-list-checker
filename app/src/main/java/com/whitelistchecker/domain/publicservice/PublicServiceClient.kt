package com.whitelistchecker.domain.publicservice

import com.whitelistchecker.domain.model.PublicServiceLink
import com.whitelistchecker.domain.model.PublicServiceRemoteCommand
import com.whitelistchecker.domain.model.PublicServiceRemoteCommandType
import com.whitelistchecker.domain.model.PublicServiceSettings
import com.whitelistchecker.domain.model.PublicServiceCommandOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

class PublicServiceClient(
    private val httpClient: OkHttpClient,
    private val urlBuilder: PublicServiceUrlBuilder,
) {

    suspend fun register(appVersion: String): RegistrationResponse = withContext(Dispatchers.IO) {
        val requestId = UUID.randomUUID().toString()
        val body = baseBody(requestId)
            .put("platform", "ANDROID")
            .put("appVersion", appVersion)
        val response = execute(
            endpoint = urlBuilder.buildEndpoint("/api/v1/installations/register"),
            token = null,
            method = "POST",
            body = body,
        )
        RegistrationResponse(
            installationId = response.getString("installationId"),
            deviceToken = response.getString("deviceToken"),
            createdAtMillis = response.getLong("createdAt"),
        )
    }

    suspend fun saveSettings(settings: PublicServiceSettings, token: String) = withContext(Dispatchers.IO) {
        val body = baseBody()
            .put("shareReports", settings.shareReports)
            .put("allowRemoteChecks", settings.allowRemoteChecks)
            .put("regionCode", settings.regionCode)
            .putNullable("cityCode", settings.cityCode)
            .putNullable("customCityName", settings.customCityName)
            .put("operatorCode", settings.operatorCode)
            .put("areaSource", settings.areaSource.name)
            .put("operatorSource", settings.operatorSource.name)
            .put("deviceAlias", settings.deviceAlias)
        execute(
            endpoint = urlBuilder.buildEndpoint("/api/v1/installations/me/settings"),
            token = token,
            method = "PUT",
            body = body,
        )
        Unit
    }

    suspend fun uploadReport(settings: PublicServiceSettings, token: String, payloadJson: String): UploadResponse =
        withContext(Dispatchers.IO) {
            val response = execute(
                endpoint = urlBuilder.buildEndpoint("/api/v1/reports"),
                token = token,
                method = "POST",
                body = JSONObject(payloadJson),
            )
            UploadResponse(
                accepted = response.optBoolean("accepted", false),
                duplicate = response.optBoolean("duplicate", false),
            )
        }

    suspend fun createLinkCode(settings: PublicServiceSettings, token: String): LinkCodeResponse =
        withContext(Dispatchers.IO) {
            val response = execute(
                endpoint = urlBuilder.buildEndpoint("/api/v1/installations/me/link-codes"),
                token = token,
                method = "POST",
                body = baseBody(),
            )
            LinkCodeResponse(
                code = response.getString("code"),
                expiresAtMillis = response.getLong("expiresAt"),
            )
        }

    suspend fun getLinks(settings: PublicServiceSettings, token: String): List<PublicServiceLink> =
        withContext(Dispatchers.IO) {
            val response = execute(
                endpoint = urlBuilder.buildEndpoint("/api/v1/installations/me/links"),
                token = token,
                method = "GET",
                body = null,
            )
            val links = response.optJSONArray("links") ?: JSONArray()
            List(links.length()) { index ->
                val item = links.getJSONObject(index)
                PublicServiceLink(
                    linkId = item.getString("linkId"),
                    chatId = item.getString("chatId"),
                    deviceAlias = item.optString("deviceAlias"),
                )
            }
        }

    suspend fun revokeLink(settings: PublicServiceSettings, token: String, linkId: String) = withContext(Dispatchers.IO) {
        execute(
            endpoint = urlBuilder.buildEndpoint("/api/v1/installations/me/links/$linkId"),
            token = token,
            method = "DELETE",
            body = null,
        )
        Unit
    }

    suspend fun serviceSync(
        settings: PublicServiceSettings,
        token: String,
        serviceSessionId: String,
        serviceStartedAtMillis: Long,
        serviceState: String,
        checkInProgress: Boolean,
        appVersion: String,
    ): ServiceSyncResponse = withContext(Dispatchers.IO) {
        val response = execute(
            endpoint = urlBuilder.buildEndpoint("/api/v1/installations/me/service-sync"),
            token = token,
            method = "POST",
            body = baseBody()
                .put("serviceSessionId", serviceSessionId)
                .put("serviceStartedAt", serviceStartedAtMillis)
                .put("serviceState", serviceState)
                .put("checkInProgress", checkInProgress)
                .put("appVersion", appVersion),
        )
        val commandJson = response.optJSONObject("command")
        ServiceSyncResponse(
            serverTimeMillis = response.optLong("serverTime", System.currentTimeMillis()),
            nextPollAfterSeconds = response.optLong("nextPollAfterSeconds", 15L),
            command = commandJson?.let {
                PublicServiceRemoteCommand(
                    commandId = it.getString("commandId"),
                    type = PublicServiceRemoteCommandType.valueOf(it.getString("type")),
                    expiresAtMillis = it.getLong("expiresAt"),
                )
            },
        )
    }

    suspend fun sendCommandResult(
        settings: PublicServiceSettings,
        token: String,
        commandId: String,
        payload: CommandResultPayload,
    ) = withContext(Dispatchers.IO) {
        val body = baseBody()
            .put("serviceSessionId", payload.serviceSessionId)
            .put("outcome", payload.outcome.name)
            .put("checkedAt", payload.checkedAtMillis)
        payload.whitelistState?.let { body.put("whitelistState", it) }
        payload.errorCode?.let { body.put("errorCode", it) }
        payload.foreign?.let {
            body.put(
                "foreign",
                JSONObject()
                    .put("available", it.available)
                    .put("total", it.total),
            )
        }
        payload.local?.let {
            body.put(
                "local",
                JSONObject()
                    .put("available", it.available)
                    .put("total", it.total),
            )
        }
        execute(
            endpoint = urlBuilder.buildEndpoint("/api/v1/installations/me/commands/$commandId/result"),
            token = token,
            method = "POST",
            body = body,
        )
        Unit
    }

    suspend fun revokeInstallation(settings: PublicServiceSettings, token: String) = withContext(Dispatchers.IO) {
        execute(
            endpoint = urlBuilder.buildEndpoint("/api/v1/installations/me"),
            token = token,
            method = "DELETE",
            body = null,
        )
        Unit
    }

    private fun execute(
        endpoint: String,
        token: String?,
        method: String,
        body: JSONObject?,
    ): JSONObject {
        val requestBuilder = Request.Builder().url(endpoint)
            .header("Accept", "application/json")
        token?.let { requestBuilder.header("Authorization", "Bearer $it") }
        when (method) {
            "GET" -> requestBuilder.get()
            "DELETE" -> requestBuilder.delete()
            "POST" -> requestBuilder.post(body.toRequestBody())
            "PUT" -> requestBuilder.put(body.toRequestBody())
            else -> error("Unsupported method $method")
        }
        try {
            httpClient.newCall(requestBuilder.build()).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                val json = responseBody.takeIf { it.isNotBlank() }?.let { JSONObject(it) } ?: JSONObject()
                if (response.isSuccessful) return json
                val error = json.optJSONObject("error")
                throw PublicServiceException(
                    code = error?.optString("code").orEmpty().ifBlank { "HTTP_${response.code}" },
                    message = error?.optString("message").orEmpty().ifBlank { "Public service HTTP ${response.code}" },
                )
            }
        } catch (exception: PublicServiceException) {
            throw exception
        } catch (exception: IOException) {
            throw PublicServiceException("NETWORK_ERROR", "Общий сервис недоступен")
        } catch (exception: Exception) {
            throw PublicServiceException("CLIENT_ERROR", exception.message ?: exception.javaClass.simpleName)
        }
    }

    private fun JSONObject?.toRequestBody() =
        (this ?: JSONObject()).toString().toRequestBody(JSON_MEDIA_TYPE)

    private fun baseBody(requestId: String = UUID.randomUUID().toString()): JSONObject {
        return JSONObject()
            .put("schemaVersion", 1)
            .put("requestId", requestId)
    }

    private fun JSONObject.putNullable(name: String, value: String?): JSONObject {
        if (value.isNullOrBlank()) return put(name, JSONObject.NULL)
        return put(name, value)
    }

    data class RegistrationResponse(
        val installationId: String,
        val deviceToken: String,
        val createdAtMillis: Long,
    )

    data class UploadResponse(
        val accepted: Boolean,
        val duplicate: Boolean,
    )

    data class LinkCodeResponse(
        val code: String,
        val expiresAtMillis: Long,
    )

    data class ServiceSyncResponse(
        val serverTimeMillis: Long,
        val nextPollAfterSeconds: Long,
        val command: PublicServiceRemoteCommand?,
    )

    data class CountPayload(
        val available: Int,
        val total: Int,
    )

    data class CommandResultPayload(
        val serviceSessionId: String,
        val outcome: PublicServiceCommandOutcome,
        val checkedAtMillis: Long,
        val whitelistState: String? = null,
        val foreign: CountPayload? = null,
        val local: CountPayload? = null,
        val errorCode: String? = null,
    )

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
