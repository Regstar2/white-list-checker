package com.whitelistchecker.domain.model.statistics

data class DailyCheckStatistics(
    val date: String,
    val totalRuns: Int = 0,
    val successRuns: Int = 0,
    val partialFailureRuns: Int = 0,
    val failureRuns: Int = 0,
    val totalTargetChecks: Int = 0,
    val successTargetChecks: Int = 0,
    val failureTargetChecks: Int = 0,
    val averageLatencyMs: Long? = null,
    val updatedAt: Long = 0L,
    val latencySampleCount: Int = 0,
    val latencySumMs: Long = 0L,
)
