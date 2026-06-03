package com.whitelistchecker.domain.model.statistics

data class RouteKindStatistics(
    val routeKind: String,
    val totalChecks: Int = 0,
    val successChecks: Int = 0,
    val failureChecks: Int = 0,
    val successRate: Double? = null,
    val averageLatencyMs: Long? = null,
    val lastCheckedAt: Long? = null,
    val updatedAt: Long = 0L,
    val latencySampleCount: Int = 0,
    val latencySumMs: Long = 0L,
)
