package com.whitelistchecker.domain.checker

import android.net.Network
import com.whitelistchecker.domain.model.SiteCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MobileSiteChecker {

    suspend fun check(
        network: Network,
        name: String,
        url: String,
    ): SiteCheckResult = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        val headResult = runCheck(network, url, REQUEST_HEAD)
        if (headResult.httpCode == HTTP_METHOD_NOT_ALLOWED) {
            val getResult = runCheck(network, url, REQUEST_GET)
            toSiteCheckResult(name, url, getResult, startedAt)
        } else {
            toSiteCheckResult(name, url, headResult, startedAt)
        }
    }

    private fun toSiteCheckResult(
        name: String,
        url: String,
        raw: RawCheckResult,
        startedAt: Long,
    ): SiteCheckResult {
        val durationMs = System.currentTimeMillis() - startedAt
        val available = raw.httpCode != null && raw.httpCode in HTTP_AVAILABLE_RANGE
        return SiteCheckResult(
            name = name,
            url = url,
            available = available,
            httpCode = raw.httpCode,
            error = raw.error,
            durationMs = durationMs,
        )
    }

    private fun runCheck(network: Network, url: String, method: String): RawCheckResult {
        var connection: HttpURLConnection? = null
        return try {
            connection = network.openConnection(URL(url)) as HttpURLConnection
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = true
            val code = connection.responseCode
            RawCheckResult(httpCode = code, error = null)
        } catch (exception: Exception) {
            RawCheckResult(
                httpCode = null,
                error = exception.javaClass.simpleName +
                    (exception.message?.let { message -> ": $message" } ?: ""),
            )
        } finally {
            connection?.disconnect()
        }
    }

    private data class RawCheckResult(
        val httpCode: Int?,
        val error: String?,
    )

    companion object {
        private const val REQUEST_HEAD = "HEAD"
        private const val REQUEST_GET = "GET"
        private const val CONNECT_TIMEOUT_MS = 4_000
        private const val READ_TIMEOUT_MS = 4_000
        private const val HTTP_METHOD_NOT_ALLOWED = 405
        val HTTP_AVAILABLE_RANGE = 200..399

        const val GOOGLE_NAME = "Google"
        const val GOOGLE_URL = "https://www.google.com/generate_204"
        const val YANDEX_NAME = "Yandex"
        const val YANDEX_URL = "https://ya.ru"
    }
}
