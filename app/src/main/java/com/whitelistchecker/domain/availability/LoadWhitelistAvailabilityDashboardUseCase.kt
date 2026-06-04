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
            val targets = repository.getTargetStatistics(WhitelistAvailabilityConfig.TARGET_STATES_LIMIT)
            val targetStates = targets.sortedBy { it.displayLabel.lowercase() }
            val recentEvents = repository.getRecentEvents(WhitelistAvailabilityConfig.RECENT_EVENTS_LIMIT)
            val topAvailable = filterTopAvailableTargets(targets)
            val topStable = filterTopStableTargets(targets)
            val topUnstable = filterTopUnstableTargets(targets)

            val dashboard = WhitelistAvailabilityDashboard(
                summary = summary,
                daily = daily,
                targetStates = targetStates,
                recentEvents = recentEvents,
                topAvailableTargets = topAvailable,
                topStableTargets = topStable,
                topUnstableTargets = topUnstable,
                lastUpdatedAt = summary.lastUpdatedAt.takeIf { it > 0L },
                isStale = staleResolver.isStale(summary.lastUpdatedAt, nowMillis),
            )
            WhitelistAvailabilityLoadResult.Success(dashboard)
        } catch (exception: Exception) {
            WhitelistAvailabilityLoadResult.Failure(exception)
        }
    }

    private fun filterTopAvailableTargets(
        targets: List<WhitelistTargetAvailabilityStats>,
    ): List<WhitelistTargetAvailabilityStats> {
        val filtered = targets
            .filter { (it.availabilityPercent ?: 0.0) > 0.0 }
            .sortedByDescending { it.availabilityPercent ?: 0.0 }
        if (filtered.isEmpty()) return emptyList()
        val percents = filtered.map { it.availabilityPercent ?: 0.0 }
        if (percents.distinct().size <= 1 && percents.all { it >= 0.999 }) {
            return emptyList()
        }
        return filtered.take(WhitelistAvailabilityConfig.TOP_TARGETS_LIMIT)
    }

    private fun filterTopStableTargets(
        targets: List<WhitelistTargetAvailabilityStats>,
    ): List<WhitelistTargetAvailabilityStats> {
        if (targets.isEmpty()) return emptyList()
        val sorted = targets
            .filter { (it.availabilityPercent ?: 0.0) > 0.0 }
            .sortedWith(
                compareByDescending<WhitelistTargetAvailabilityStats> { it.availabilityPercent ?: 0.0 }
                    .thenBy { it.unstableScore },
            )
        if (sorted.isEmpty()) return emptyList()
        val distinctAvailability = sorted.map { it.availabilityPercent ?: 0.0 }.distinct()
        val distinctUnstable = sorted.map { it.unstableScore }.distinct()
        if (distinctAvailability.size <= 1 && distinctUnstable.size <= 1) {
            return emptyList()
        }
        return sorted.take(WhitelistAvailabilityConfig.TOP_TARGETS_LIMIT)
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
