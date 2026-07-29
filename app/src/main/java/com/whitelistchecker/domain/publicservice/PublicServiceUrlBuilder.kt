package com.whitelistchecker.domain.publicservice

import java.net.URI

class PublicServiceUrlBuilder(
    rawBaseUrl: String,
) {
    val baseUrl: String = normalizeBaseUrl(rawBaseUrl)

    fun buildEndpoint(path: String): String {
        require(path.startsWith("/")) { "Public service path must start with /" }
        return baseUrl + path
    }

    private fun normalizeBaseUrl(value: String): String {
        val trimmed = value.trim().trimEnd('/')
        require(trimmed.isNotBlank()) { "Public service URL is empty" }
        val uri = URI(trimmed)
        require(uri.scheme == "https") { "Public service URL must use HTTPS" }
        require(!uri.host.isNullOrBlank()) { "Public service URL host is empty" }
        require(uri.rawQuery == null && uri.rawFragment == null) {
            "Public service URL must not contain query or fragment"
        }
        return trimmed
    }
}
