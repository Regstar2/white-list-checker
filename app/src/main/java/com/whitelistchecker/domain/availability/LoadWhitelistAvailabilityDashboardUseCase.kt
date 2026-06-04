package com.whitelistchecker.domain.availability

import com.whitelistchecker.domain.model.availability.WhitelistTargetAvailabilityStats
import com.whitelistchecker.domain.statistics.StatisticsStaleResolver

class LoadWhitelistAvailabilityDashboardUseCase(
    private val repository: WhitelistAvailabilityRepository,
    private val staleResolver: StatisticsStaleResolver = StatisticsStaleResolver(),
) {

    suspend fun load(nowMillis: Long = System.currentTimeMillis()): WhitelistAvailabilityLoadResult {
        return try {
            if (!repository.summaryHasData()) {
                return WhitelistAvailabilityLoadResult.Empty
            }
            val summary = repository.getSummary()
            val daily = repository.getDailyStatistics(WhitelistAvailabilityConfig.CHART_DAYS_LIMIT)
                .sortedBy { it.date }
            val targets = repository.getTargetStatistics(WhitelistAvailabilityConfig.TOP_TARGETS_LIMIT * 2)
            val topAvailable = targets
                .filter { (it.availabilityPercent ?: 0.0) > 0.0 }
                .sortedByDescending { it.availabilityPercent ?: 0.0 }
                .take(WhitelistAvailabilityConfig.TOP_TARGETS_LIMIT)
            val topUnstable = filterTopUnstableTargets(targets)

            val dashboard = WhitelistAvailabilityDashboard(
                summary = summary,
                daily = daily,
                topAvailableTargets = topAvailable,
                topUnstableTargets = topUnstable,
                lastUpdatedAt = summary.lastUpdatedAt.takeIf { it > 0L },
                isStale = staleResolver.isStale(summary.lastUpdatedAt, nowMillis),
            )
            WhitelistAvailabilityLoadResult.Success(dashboard)
        } catch (exception: Exception) {
            WhitelistAvailabilityLoadResult.Failure(exception)
        }
    }

    private fun filterTopUnstableTargets(
        targets: List<WhitelistTargetAvailabilityStats>,
    ): List<WhitelistTargetAvailabilityStats> {
        if (targets.isEmpty()) return emptyList()
        val sorted = targets.sortedByDescending { it.unstableScore }
        val distinctScores = sorted.map { it.unstableScore }.distinct()
        if (distinctScores.size <= 1 || sorted.first().unstableScore <= 0.0) {
            return emptyList()
        }
        return sorted.take(WhitelistAvailabilityConfig.TOP_TARGETS_LIMIT)
    }
}
