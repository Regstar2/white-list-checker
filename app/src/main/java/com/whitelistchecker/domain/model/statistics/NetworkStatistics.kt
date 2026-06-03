package com.whitelistchecker.domain.model.statistics

data class NetworkStatistics(
    val networkKey: String,
    val networkType: String,
    val operatorName: String?,
    val totalRuns: Int = 0,
    val successRuns: Int = 0,
    val failureRuns: Int = 0,
    val successRate: Double? = null,
    val averageLatencyMs: Long? = null,
    val lastRunAt: Long? = null,
    val updatedAt: Long = 0L,
    val latencySampleCount: Int = 0,
    val latencySumMs: Long = 0L,
)
