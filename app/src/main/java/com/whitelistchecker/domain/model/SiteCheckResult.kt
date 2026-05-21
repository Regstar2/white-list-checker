package com.whitelistchecker.domain.model

data class SiteCheckResult(
    val target: CheckTarget,
    val available: Boolean,
    val httpCode: Int?,
    val error: String?,
    val durationMs: Long,
)
