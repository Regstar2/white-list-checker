package com.whitelistchecker.domain.model.statistics

data class TargetStatistics(
    val targetId: String,
    val targetLabel: String,
    val targetHost: String,
    val totalChecks: Int = 0,
    val successChecks: Int = 0,
    val failureChecks: Int = 0,
    val timeoutChecks: Int = 0,
    val successRate: Double? = null,
    val averageLatencyMs: Long? = null,
    val lastCheckedAt: Long? = null,
    val lastSuccessAt: Long? = null,
    val lastFailureAt: Long? = null,
    val consecutiveFailureCount: Int = 0,
    val updatedAt: Long = 0L,
    val latencySampleCount: Int = 0,
    val latencySumMs: Long = 0L,
)
