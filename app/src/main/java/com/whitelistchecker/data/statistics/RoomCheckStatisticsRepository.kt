package com.whitelistchecker.data.statistics

import androidx.room.withTransaction
import com.whitelistchecker.data.db.AppDatabase
import com.whitelistchecker.domain.model.history.CheckRun
import com.whitelistchecker.domain.model.history.CheckTargetResult
import com.whitelistchecker.domain.model.statistics.CheckStatisticsSnapshot
import com.whitelistchecker.domain.model.statistics.CheckStatisticsSummary
import com.whitelistchecker.domain.model.statistics.DailyCheckStatistics
import com.whitelistchecker.domain.model.statistics.NetworkStatistics
import com.whitelistchecker.domain.model.statistics.RouteKindStatistics
import com.whitelistchecker.domain.model.statistics.TargetStatistics
import com.whitelistchecker.domain.statistics.CheckStatisticsCalculator
import com.whitelistchecker.domain.statistics.CheckStatisticsConfig
import com.whitelistchecker.domain.statistics.CheckStatisticsRepository

class RoomCheckStatisticsRepository(
    private val database: AppDatabase,
    private val dao: CheckStatisticsDao,
    private val calculator: CheckStatisticsCalculator,
) : CheckStatisticsRepository {

    override suspend fun updateFromCheckRun(
        checkRun: CheckRun,
        targetResults: List<CheckTargetResult>,
    ) {
        database.withTransaction {
            val snapshot = loadSnapshot()
            val updated = calculator.applyRun(snapshot, checkRun, targetResults)
            persistSnapshot(updated)
            applyDailyRetentionLocked(checkRun.finishedAtMillis)
        }
    }

    override suspend fun getSummary(): CheckStatisticsSummary {
        return dao.getSummary(CheckStatisticsConfig.SUMMARY_ROW_ID)?.let(CheckStatisticsEntityMapper::toDomain)
            ?: CheckStatisticsSummary()
    }

    override suspend fun summaryExists(): Boolean {
        if (dao.countSummaryRows(CheckStatisticsConfig.SUMMARY_ROW_ID) == 0) {
            return false
        }
        return getSummary().totalRuns > 0
    }

    override suspend fun countTargetStatistics(): Int = dao.countTargets()

    override suspend fun countRouteKindStatistics(): Int = dao.countRouteKinds()

    override suspend fun countNetworkStatistics(): Int = dao.countNetworks()

    override suspend fun countDailyStatistics(): Int = dao.countDaily()

    override suspend fun getTargetStatistics(limit: Int): List<TargetStatistics> {
        return dao.getTargets(limit).map(CheckStatisticsEntityMapper::toDomain)
    }

    override suspend fun getRouteKindStatistics(): List<RouteKindStatistics> {
        return dao.getRouteKinds().map(CheckStatisticsEntityMapper::toDomain)
    }

    override suspend fun getNetworkStatistics(): List<NetworkStatistics> {
        return dao.getNetworks().map(CheckStatisticsEntityMapper::toDomain)
    }

    override suspend fun getDailyStatistics(limit: Int): List<DailyCheckStatistics> {
        return dao.getDaily(limit).map(CheckStatisticsEntityMapper::toDomain)
    }

    override suspend fun replaceAll(snapshot: CheckStatisticsSnapshot) {
        database.withTransaction {
            persistSnapshot(snapshot)
        }
    }

    override suspend fun clearStatistics() {
        database.withTransaction {
            dao.replaceAll(
                summary = CheckStatisticsEntityMapper.defaultSummaryEntity(),
                targets = emptyList(),
                routeKinds = emptyList(),
                networks = emptyList(),
                daily = emptyList(),
            )
        }
    }

    override suspend fun applyDailyRetention(nowMillis: Long) {
        database.withTransaction {
            applyDailyRetentionLocked(nowMillis)
        }
    }

    private suspend fun loadSnapshot(): CheckStatisticsSnapshot {
        return CheckStatisticsEntityMapper.toSnapshot(
            summary = dao.getSummary(CheckStatisticsConfig.SUMMARY_ROW_ID),
            targets = dao.getAllTargets(),
            routeKinds = dao.getRouteKinds(),
            networks = dao.getNetworks(),
            daily = dao.getAllDaily(),
        )
    }

    private suspend fun persistSnapshot(snapshot: CheckStatisticsSnapshot) {
        dao.replaceAll(
            summary = CheckStatisticsEntityMapper.toEntity(snapshot.summary),
            targets = snapshot.targets.values.map(CheckStatisticsEntityMapper::toEntity),
            routeKinds = snapshot.routeKinds.values.map(CheckStatisticsEntityMapper::toEntity),
            networks = snapshot.networks.values.map(CheckStatisticsEntityMapper::toEntity),
            daily = snapshot.daily.values.map(CheckStatisticsEntityMapper::toEntity),
        )
    }

    private suspend fun applyDailyRetentionLocked(nowMillis: Long) {
        val cutoffDateKey = calculator.formatDateKey(
            nowMillis - CheckStatisticsConfig.MAX_DAILY_STATISTICS_AGE_MS,
        )
        dao.deleteDailyOlderThan(cutoffDateKey)
    }
}
