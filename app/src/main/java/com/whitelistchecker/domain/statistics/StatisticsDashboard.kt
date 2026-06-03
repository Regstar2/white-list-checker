package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.model.statistics.CheckStatisticsSummary
import com.whitelistchecker.domain.model.statistics.DailyCheckStatistics
import com.whitelistchecker.domain.model.statistics.NetworkStatistics
import com.whitelistchecker.domain.model.statistics.RouteKindStatistics
import com.whitelistchecker.domain.model.statistics.TargetStatistics

data class StatisticsDashboard(
    val summary: CheckStatisticsSummary,
    val targets: List<TargetStatistics>,
    val routeKinds: List<RouteKindStatistics>,
    val networks: List<NetworkStatistics>,
    val daily: List<DailyCheckStatistics>,
    val isStale: Boolean,
    val lastUpdatedAt: Long?,
)

sealed class StatisticsLoadResult {
    data object Empty : StatisticsLoadResult()

    data class Success(val dashboard: StatisticsDashboard) : StatisticsLoadResult()

    data class Failure(val cause: Throwable) : StatisticsLoadResult()
}
