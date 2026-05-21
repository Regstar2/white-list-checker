package com.whitelistchecker.domain.checker

import android.net.Network
import com.whitelistchecker.domain.model.CheckTarget
import com.whitelistchecker.domain.model.SiteCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MobileSiteChecker {

    suspend fun checkTarget(
        network: Network,
        target: CheckTarget,
    ): SiteCheckResult = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        val headResult = runCheck(network, target.url, REQUEST_HEAD)
        val raw = if (headResult.httpCode == HTTP_METHOD_NOT_ALLOWED) {
            runCheck(network, target.url, REQUEST_GET)
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
        return SiteCheckResult(
            target = target,
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
    }
}
