package com.whitelistchecker.domain.checker

import android.net.Network
import com.whitelistchecker.domain.model.CheckTarget
import com.whitelistchecker.domain.model.SiteCheckErrorType
import com.whitelistchecker.domain.model.SiteCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException

class MobileSiteChecker {

    fun createSession(
        network: Network,
        dns: Dns,
    ): Session {
        val client = OkHttpClient.Builder()
            .socketFactory(network.socketFactory)
            .dns(dns)
            .connectTimeout(CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .writeTimeout(READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
        return Session(client)
    }

    class Session internal constructor(
        private val client: OkHttpClient,
    ) {
        suspend fun checkTarget(target: CheckTarget): SiteCheckResult = withContext(Dispatchers.IO) {
            val startedAt = System.currentTimeMillis()
            val headResult = runCheck(target.url, REQUEST_HEAD)
            val raw = if (headResult.httpCode == HTTP_METHOD_NOT_ALLOWED) {
                runCheck(target.url, REQUEST_GET)
            } else {
                headResult
            }
            toSiteCheckResult(target, raw, startedAt)
        }

        private fun toSiteCheckResult(
            target: CheckTarget,
            raw: RawCheckResult,
            startedAt: Long,
        ): SiteCheckResult {
            val durationMs = System.currentTimeMillis() - startedAt
            val available = raw.httpCode != null && raw.httpCode in HTTP_AVAILABLE_RANGE
            val errorType = when {
                available -> SiteCheckErrorType.NONE
                raw.httpCode != null -> SiteCheckErrorType.HTTP
                else -> raw.errorType
            }
            return SiteCheckResult(
                target = target,
                available = available,
                httpCode = raw.httpCode,
                error = raw.error,
                errorType = errorType,
                durationMs = durationMs,
            )
        }

        private fun runCheck(url: String, method: String): RawCheckResult {
            return try {
                val requestBuilder = Request.Builder().url(url)
                val request = if (method == REQUEST_HEAD) {
                    requestBuilder.head().build()
                } else {
                    requestBuilder.get().build()
                }
                client.newCall(request).execute().use { response ->
                    RawCheckResult(
                        httpCode = response.code,
                        error = null,
                        errorType = SiteCheckErrorType.NONE,
                    )
                }
            } catch (exception: Exception) {
                val (message, errorType) = classifyException(exception)
                RawCheckResult(
                    httpCode = null,
                    error = message,
                    errorType = errorType,
                )
            }
        }

        private fun classifyException(exception: Exception): Pair<String, SiteCheckErrorType> {
            val errorType = mapExceptionToErrorType(exception)
            val message = exception.javaClass.simpleName +
                (exception.message?.let { text -> ": $text" } ?: "")
            return message to errorType
        }

        private fun mapExceptionToErrorType(exception: Throwable): SiteCheckErrorType {
            return when (exception) {
                is UnknownHostException -> SiteCheckErrorType.DNS
                is SocketTimeoutException -> SiteCheckErrorType.TIMEOUT
                is ConnectException -> SiteCheckErrorType.CONNECTION
                is SSLException -> SiteCheckErrorType.TLS
                else -> {
                    val cause = exception.cause
                    if (cause != null && cause !== exception) {
                        mapExceptionToErrorType(cause)
                    } else if (exception.message?.contains("timeout", ignoreCase = true) == true) {
                        SiteCheckErrorType.TIMEOUT
                    } else {
                        SiteCheckErrorType.UNKNOWN
                    }
                }
            }
        }
    }

    private data class RawCheckResult(
        val httpCode: Int?,
        val error: String?,
        val errorType: SiteCheckErrorType,
    )

    companion object {
        private const val REQUEST_HEAD = "HEAD"
        private const val REQUEST_GET = "GET"
        private const val CONNECT_TIMEOUT_MS = 4_000
        private const val READ_TIMEOUT_MS = 4_000
        private const val HTTP_METHOD_NOT_ALLOWED = 405
        val HTTP_AVAILABLE_RANGE = 200..399
    }
}
