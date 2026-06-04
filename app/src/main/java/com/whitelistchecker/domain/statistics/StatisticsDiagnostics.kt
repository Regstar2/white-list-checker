package com.whitelistchecker.domain.statistics

data class StatisticsDiagnostics(
    val checkRunCount: Int,
    val targetResultCount: Int,
    val summaryExists: Boolean,
    val targetStatisticsCount: Int,
    val routeKindStatisticsCount: Int,
    val networkStatisticsCount: Int,
    val dailyStatisticsCount: Int,
    val lastCheckRunAt: Long?,
    val lastStatisticsUpdatedAt: Long?,
    val oldestCheckRunAt: Long?,
    val newestCheckRunAt: Long?,
    val lastRebuildAt: Long?,
    val lastCleanupAt: Long?,
    val consistencyReport: StatisticsConsistencyReport,
    val diagnosticsGeneratedAt: Long,
)
