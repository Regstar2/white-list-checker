package com.whitelistchecker.domain.model

data class SiteCheckResult(
    val name: String,
    val url: String,
    val available: Boolean,
    val httpCode: Int?,
    val error: String?,
    val durationMs: Long,
)
