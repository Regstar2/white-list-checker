package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.model.statistics.TargetStatistics

class LoadStatisticsDashboardUseCase(
    private val checkStatisticsRepository: CheckStatisticsRepository,
    private val staleResolver: StatisticsStaleResolver = StatisticsStaleResolver(),
) {

    suspend fun load(
        nowMillis: Long = System.currentTimeMillis(),
        targetLimit: Int = TOP_TARGETS_LIMIT,
    ): StatisticsLoadResult {
        return try {
            val summary = checkStatisticsRepository.getSummary()
            if (summary.totalRuns == 0) {
                return StatisticsLoadResult.Empty
            }
            val lastUpdatedAt = summary.lastRunAt ?: summary.updatedAt.takeIf { it > 0L }
            val dashboard = StatisticsDashboard(
                summary = summary,
                targets = sortTargets(checkStatisticsRepository.getTargetStatistics(targetLimit)),
                routeKinds = checkStatisticsRepository.getRouteKindStatistics(),
                networks = checkStatisticsRepository.getNetworkStatistics(),
                daily = checkStatisticsRepository.getDailyStatistics(),
                isStale = staleResolver.isStale(summary.lastRunAt, nowMillis),
                lastUpdatedAt = lastUpdatedAt,
            )
            StatisticsLoadResult.Success(dashboard)
        } catch (exception: Exception) {
            StatisticsLoadResult.Failure(exception)
        }
    }

    private fun sortTargets(targets: List<TargetStatistics>): List<TargetStatistics> {
        return targets.sortedWith(
            compareByDescending<TargetStatistics> { target ->
                if (target.totalChecks == 0) {
                    0.0
                } else {
                    target.failureChecks.toDouble() / target.totalChecks.toDouble()
                }
            }.thenByDescending { it.totalChecks },
        )
    }

    companion object {
        const val TOP_TARGETS_LIMIT: Int = 20
    }
}
