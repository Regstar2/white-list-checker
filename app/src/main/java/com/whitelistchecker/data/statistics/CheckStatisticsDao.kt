package com.whitelistchecker.data.statistics

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface CheckStatisticsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(entity: CheckStatisticsSummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTargets(entities: List<TargetStatisticsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRouteKinds(entities: List<RouteKindStatisticsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNetworks(entities: List<NetworkStatisticsEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDaily(entities: List<DailyCheckStatisticsEntity>)

    @Query("SELECT * FROM check_statistics_summary WHERE id = :id LIMIT 1")
    suspend fun getSummary(id: Int): CheckStatisticsSummaryEntity?

    @Query("SELECT COUNT(*) FROM check_statistics_summary WHERE id = :id")
    suspend fun countSummaryRows(id: Int): Int

    @Query("SELECT COUNT(*) FROM target_statistics")
    suspend fun countTargets(): Int

    @Query("SELECT COUNT(*) FROM route_kind_statistics")
    suspend fun countRouteKinds(): Int

    @Query("SELECT COUNT(*) FROM network_statistics")
    suspend fun countNetworks(): Int

    @Query("SELECT COUNT(*) FROM daily_check_statistics")
    suspend fun countDaily(): Int

    @Query("SELECT * FROM target_statistics ORDER BY lastCheckedAt DESC LIMIT :limit")
    suspend fun getTargets(limit: Int): List<TargetStatisticsEntity>

    @Query("SELECT * FROM target_statistics")
    suspend fun getAllTargets(): List<TargetStatisticsEntity>

    @Query("SELECT * FROM route_kind_statistics ORDER BY lastCheckedAt DESC")
    suspend fun getRouteKinds(): List<RouteKindStatisticsEntity>

    @Query("SELECT * FROM network_statistics ORDER BY lastRunAt DESC")
    suspend fun getNetworks(): List<NetworkStatisticsEntity>

    @Query(
        """
        SELECT * FROM daily_check_statistics
        ORDER BY date DESC
        LIMIT :limit
        """,
    )
    suspend fun getDaily(limit: Int): List<DailyCheckStatisticsEntity>

    @Query("SELECT * FROM daily_check_statistics")
    suspend fun getAllDaily(): List<DailyCheckStatisticsEntity>

    @Query("DELETE FROM target_statistics")
    suspend fun clearTargets()

    @Query("DELETE FROM route_kind_statistics")
    suspend fun clearRouteKinds()

    @Query("DELETE FROM network_statistics")
    suspend fun clearNetworks()

    @Query("DELETE FROM daily_check_statistics")
    suspend fun clearDaily()

    @Query("DELETE FROM daily_check_statistics WHERE date < :oldestDateKey")
    suspend fun deleteDailyOlderThan(oldestDateKey: String)

    @Transaction
    suspend fun replaceAll(
        summary: CheckStatisticsSummaryEntity,
        targets: List<TargetStatisticsEntity>,
        routeKinds: List<RouteKindStatisticsEntity>,
        networks: List<NetworkStatisticsEntity>,
        daily: List<DailyCheckStatisticsEntity>,
    ) {
        clearTargets()
        clearRouteKinds()
        clearNetworks()
        clearDaily()
        upsertSummary(summary)
        if (targets.isNotEmpty()) upsertTargets(targets)
        if (routeKinds.isNotEmpty()) upsertRouteKinds(routeKinds)
        if (networks.isNotEmpty()) upsertNetworks(networks)
        if (daily.isNotEmpty()) upsertDaily(daily)
    }
}
