package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.model.history.CheckRun
import com.whitelistchecker.domain.model.history.CheckTargetResult
import com.whitelistchecker.domain.model.statistics.CheckStatisticsSnapshot
import com.whitelistchecker.domain.model.statistics.CheckStatisticsSummary
import com.whitelistchecker.domain.model.statistics.DailyCheckStatistics
import com.whitelistchecker.domain.model.statistics.NetworkStatistics
import com.whitelistchecker.domain.model.statistics.RouteKindStatistics
import com.whitelistchecker.domain.model.statistics.TargetStatistics

interface CheckStatisticsRepository {

    suspend fun updateFromCheckRun(
        checkRun: CheckRun,
        targetResults: List<CheckTargetResult>,
    )

    suspend fun getSummary(): CheckStatisticsSummary

    suspend fun getTargetStatistics(limit: Int = Int.MAX_VALUE): List<TargetStatistics>

    suspend fun getRouteKindStatistics(): List<RouteKindStatistics>

    suspend fun getNetworkStatistics(): List<NetworkStatistics>

    suspend fun getDailyStatistics(limit: Int = CheckStatisticsConfig.MAX_DAILY_STATISTICS_DAYS): List<DailyCheckStatistics>

    suspend fun replaceAll(snapshot: CheckStatisticsSnapshot)

    suspend fun clearStatistics()

    suspend fun applyDailyRetention(nowMillis: Long)
}
