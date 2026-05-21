package com.whitelistchecker.domain.telegram

import com.whitelistchecker.data.telegram.TelegramGetUpdatesParser
import com.whitelistchecker.domain.model.TelegramChatDiscoveryResult
import com.whitelistchecker.domain.model.TelegramSendResult
import com.whitelistchecker.domain.model.TelegramSettings
import com.whitelistchecker.domain.model.TelegramTestResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class TelegramWorkerClient(
    private val httpClient: OkHttpClient,
    private val workerUrlBuilder: WorkerUrlBuilder,
) {

    suspend fun getMe(settings: TelegramSettings): TelegramTestResult = withContext(Dispatchers.IO) {
        when (val validation = validateCanTest(settings)) {
            is ValidationResult.Failure -> return@withContext TelegramTestResult.Failure(validation.reason)
            ValidationResult.Ok -> Unit
        }
        executeWorkerRequest(
            settings = settings,
            method = WorkerUrlBuilder.METHOD_GET_ME,
            jsonBody = JSONObject(),
        ) { body, _ ->
            parseOkResponse(body, successMessage = null)
        }
    }

    suspend fun getUpdates(
        settings: TelegramSettings,
        offset: Long?,
    ): TelegramChatDiscoveryResult = withContext(Dispatchers.IO) {
        when (val validation = validateCanTest(settings)) {
            is ValidationResult.Failure -> return@withContext TelegramChatDiscoveryResult.Failure(validation.reason)
            ValidationResult.Ok -> Unit
        }
        val jsonBody = JSONObject()
        if (offset != null) {
            jsonBody.put("offset", offset)
        }
        when (
            val response = executeWorkerRequestRaw(
                settings = settings,
                method = WorkerUrlBuilder.METHOD_GET_UPDATES,
                jsonBody = jsonBody,
            )
        ) {
            is WorkerHttpResult.Failure -> TelegramChatDiscoveryResult.Failure(response.reason)
            is WorkerHttpResult.Success -> parseGetUpdatesBody(response.body)
        }
    }

    suspend fun sendMessage(
        settings: TelegramSettings,
        text: String,
    ): TelegramSendResult = withContext(Dispatchers.IO) {
        when (val validation = validateConfigured(settings)) {
            is ValidationResult.Failure -> return@withContext TelegramSendResult.Failure(validation.reason)
            ValidationResult.Ok -> Unit
        }
        val jsonBody = JSONObject()
            .put("chat_id", settings.chatId)
            .put("text", text)
        when (
            val response = executeWorkerRequestRaw(
                settings = settings,
                method = WorkerUrlBuilder.METHOD_SEND_MESSAGE,
                jsonBody = jsonBody,
            )
        ) {
            is WorkerHttpResult.Failure -> TelegramSendResult.Failure(response.reason)
            is WorkerHttpResult.Success -> {
                when (val parsed = parseOkResponse(response.body, successMessage = null)) {
                    is TelegramTestResult.Success -> TelegramSendResult.Success
                    is TelegramTestResult.Failure -> TelegramSendResult.Failure(parsed.reason)
                }
            }
        }
    }

    private fun parseGetUpdatesBody(body: String): TelegramChatDiscoveryResult {
        return when (val parsed = TelegramGetUpdatesParser.parseResponse(body)) {
            is TelegramGetUpdatesParser.ParseResult.Failure ->
                TelegramChatDiscoveryResult.Failure(
                    parsed.reason.replace(
                        "Не удалось разобрать ответ Telegram",
                        "Не удалось разобрать ответ Worker",
                    ),
                )
            is TelegramGetUpdatesParser.ParseResult.Success -> {
                val updates = parsed.updates
                val nextOffset = TelegramGetUpdatesParser.maxUpdateId(updates)?.plus(1)
                val candidates = TelegramGetUpdatesParser.toCandidates(updates)
                if (candidates.isEmpty()) {
                    TelegramChatDiscoveryResult.Empty(nextOffset = nextOffset)
                } else {
                    TelegramChatDiscoveryResult.Success(
                        candidates = candidates,
                        nextOffset = nextOffset,
                    )
                }
            }
        }
    }

    private suspend fun executeWorkerRequest(
        settings: TelegramSettings,
        method: String,
        jsonBody: JSONObject,
        onSuccessBody: (String, Int) -> TelegramTestResult,
    ): TelegramTestResult {
        return when (
            val response = executeWorkerRequestRaw(settings, method, jsonBody)
        ) {
            is WorkerHttpResult.Failure -> TelegramTestResult.Failure(response.reason)
            is WorkerHttpResult.Success -> onSuccessBody(response.body, response.httpCode)
        }
    }

    private suspend fun executeWorkerRequestRaw(
        settings: TelegramSettings,
        method: String,
        jsonBody: JSONObject,
    ): WorkerHttpResult {
        return try {
            val endpoint = workerUrlBuilder.buildEndpoint(settings.workerUrl, method)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = jsonBody.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url(endpoint)
                .post(body)
                .header("Content-Type", "application/json")
                .header("X-Relay-Secret", settings.relaySecret)
                .build()
            httpClient.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (response.code == 200) {
                    WorkerHttpResult.Success(responseBody, response.code)
                } else {
                    WorkerHttpResult.Failure(formatWorkerHttpError(response.code, responseBody))
                }
            }
        } catch (exception: IllegalArgumentException) {
            WorkerHttpResult.Failure(exception.message ?: "Некорректный Worker URL")
        } catch (exception: IOException) {
            WorkerHttpResult.Failure("${exception.javaClass.simpleName}: Worker недоступен")
        } catch (exception: Exception) {
            WorkerHttpResult.Failure("${exception.javaClass.simpleName}: Worker недоступен")
        }
    }

    private fun formatWorkerHttpError(httpCode: Int, responseBody: String): String {
        val details = responseBody.trim().takeIf { it.isNotBlank() }
        return if (details != null) {
            "Worker HTTP $httpCode: $details"
        } else {
            when (httpCode) {
                401 -> "Worker HTTP 401: Unauthorized"
                403 -> "Worker HTTP 403"
                404 -> "Worker HTTP 404: Not found"
                405 -> "Worker HTTP 405: Method not allowed"
                500 -> "Worker HTTP 500: BOT_TOKEN is not configured"
                else -> "Worker HTTP $httpCode"
            }
        }
    }

    private fun parseOkResponse(body: String, successMessage: String?): TelegramTestResult {
        return try {
            val root = JSONObject(body)
            if (root.optBoolean("ok", false)) {
                TelegramTestResult.Success
            } else {
                val description = root.optString("description").takeIf { it.isNotBlank() }
                TelegramTestResult.Failure(
                    description?.let { "Telegram Bot API вернул ошибку: $it" }
                        ?: "Не удалось разобрать ответ Worker/Telegram",
                )
            }
        } catch (_: Exception) {
            TelegramTestResult.Failure("Не удалось разобрать ответ Worker/Telegram")
        }
    }

    private fun validateCanTest(settings: TelegramSettings): ValidationResult {
        if (settings.workerUrl.isBlank()) {
            return ValidationResult.Failure("Worker URL не указан")
        }
        if (settings.relaySecret.isBlank()) {
            return ValidationResult.Failure("Relay Secret не указан")
        }
        return ValidationResult.Ok
    }

    private fun validateConfigured(settings: TelegramSettings): ValidationResult {
        when (val base = validateCanTest(settings)) {
            is ValidationResult.Failure -> return base
            ValidationResult.Ok -> Unit
        }
        if (settings.chatId.isBlank()) {
            return ValidationResult.Failure("Chat ID не указан")
        }
        return ValidationResult.Ok
    }

    private sealed interface ValidationResult {
        data object Ok : ValidationResult
        data class Failure(val reason: String) : ValidationResult
    }

    private sealed interface WorkerHttpResult {
        data class Success(val body: String, val httpCode: Int) : WorkerHttpResult
        data class Failure(val reason: String) : WorkerHttpResult
    }
}
