package com.whitelistchecker.domain.model.statistics

data class CheckStatisticsSummary(
    val totalRuns: Int = 0,
    val successRuns: Int = 0,
    val partialFailureRuns: Int = 0,
    val failureRuns: Int = 0,
    val cancelledRuns: Int = 0,
    val unknownRuns: Int = 0,
    val successRate: Double? = null,
    val averageLatencyMs: Long? = null,
    val lastRunAt: Long? = null,
    val lastSuccessAt: Long? = null,
    val lastFailureAt: Long? = null,
    val consecutiveFailureCount: Int = 0,
    val updatedAt: Long = 0L,
    val latencySampleCount: Int = 0,
    val latencySumMs: Long = 0L,
)
