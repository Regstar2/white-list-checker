package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.history.CheckHistoryRepository

class LoadStatisticsDiagnosticsUseCase(
    private val checkHistoryRepository: CheckHistoryRepository,
    private val checkStatisticsRepository: CheckStatisticsRepository,
    private val diagnosticsMetaRepository: StatisticsDiagnosticsMetaRepository,
    private val consistencyChecker: StatisticsConsistencyChecker = StatisticsConsistencyChecker(),
) {

    suspend fun load(nowMillis: Long = System.currentTimeMillis()): StatisticsDiagnostics {
        val checkRunCount = checkHistoryRepository.countCheckRuns()
        val targetResultCount = checkHistoryRepository.countTargetResults()
        val timeRange = checkHistoryRepository.getCheckRunTimeRange()
        val summary = checkStatisticsRepository.getSummary()
        val summaryExists = checkStatisticsRepository.summaryExists()

        val consistencyReport = consistencyChecker.check(
            checkRunCount = checkRunCount,
            targetResultCount = targetResultCount,
            targetStatisticsCount = checkStatisticsRepository.countTargetStatistics(),
            summary = summary,
            summaryExists = summaryExists,
            nowMillis = nowMillis,
        )

        val meta = diagnosticsMetaRepository.getMeta()

        return StatisticsDiagnostics(
            checkRunCount = checkRunCount,
            targetResultCount = targetResultCount,
            summaryExists = summaryExists,
            targetStatisticsCount = checkStatisticsRepository.countTargetStatistics(),
            routeKindStatisticsCount = checkStatisticsRepository.countRouteKindStatistics(),
            networkStatisticsCount = checkStatisticsRepository.countNetworkStatistics(),
            dailyStatisticsCount = checkStatisticsRepository.countDailyStatistics(),
            lastCheckRunAt = timeRange?.newestAt,
            lastStatisticsUpdatedAt = summary.updatedAt.takeIf { it > 0L } ?: summary.lastRunAt,
            oldestCheckRunAt = timeRange?.oldestAt,
            newestCheckRunAt = timeRange?.newestAt,
            lastRebuildAt = meta.lastRebuildAtMillis,
            lastCleanupAt = meta.lastCleanupAtMillis,
            consistencyReport = consistencyReport,
            diagnosticsGeneratedAt = nowMillis,
        )
    }
}
