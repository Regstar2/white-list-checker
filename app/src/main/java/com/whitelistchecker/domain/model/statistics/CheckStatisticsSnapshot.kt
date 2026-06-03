package com.whitelistchecker.domain.model.statistics

data class CheckStatisticsSnapshot(
    val summary: CheckStatisticsSummary = CheckStatisticsSummary(),
    val targets: Map<String, TargetStatistics> = emptyMap(),
    val routeKinds: Map<String, RouteKindStatistics> = emptyMap(),
    val networks: Map<String, NetworkStatistics> = emptyMap(),
    val daily: Map<String, DailyCheckStatistics> = emptyMap(),
)
