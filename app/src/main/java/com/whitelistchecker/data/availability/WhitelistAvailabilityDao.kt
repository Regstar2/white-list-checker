package com.whitelistchecker.data.availability

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface WhitelistAvailabilityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<WhitelistAvailabilityEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(entity: WhitelistAvailabilitySummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTargets(entities: List<WhitelistTargetAvailabilityEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDaily(entities: List<WhitelistDailyAvailabilityEntity>)

    @Query("SELECT * FROM whitelist_availability_summary WHERE id = :id LIMIT 1")
    suspend fun getSummary(id: Int): WhitelistAvailabilitySummaryEntity?

    @Query("SELECT * FROM whitelist_target_availability")
    suspend fun getAllTargets(): List<WhitelistTargetAvailabilityEntity>

    @Query(
        """
        SELECT * FROM whitelist_daily_availability
        ORDER BY date DESC
        LIMIT :limit
        """,
    )
    suspend fun getDaily(limit: Int): List<WhitelistDailyAvailabilityEntity>

    @Query("SELECT * FROM whitelist_daily_availability")
    suspend fun getAllDaily(): List<WhitelistDailyAvailabilityEntity>

    @Query("DELETE FROM whitelist_availability_events")
    suspend fun clearEvents()

    @Query("DELETE FROM whitelist_target_availability")
    suspend fun clearTargets()

    @Query("DELETE FROM whitelist_daily_availability")
    suspend fun clearDaily()

    @Query("DELETE FROM whitelist_daily_availability WHERE date < :oldestDateKey")
    suspend fun deleteDailyOlderThan(oldestDateKey: String)

    @Query("DELETE FROM whitelist_availability_events WHERE detectedAt < :olderThanMillis")
    suspend fun deleteEventsOlderThan(olderThanMillis: Long)

    @Query(
        """
        SELECT id FROM whitelist_availability_events
        ORDER BY detectedAt ASC
        LIMIT :count
        """,
    )
    suspend fun getOldestEventIds(count: Int): List<String>

    @Query("DELETE FROM whitelist_availability_events WHERE id IN (:ids)")
    suspend fun deleteEventsByIds(ids: List<String>)

    @Query("SELECT COUNT(*) FROM whitelist_availability_events")
    suspend fun countEvents(): Int

    @Query(
        """
        SELECT * FROM whitelist_availability_events
        ORDER BY detectedAt DESC
        LIMIT :limit
        """,
    )
    suspend fun getRecentEvents(limit: Int): List<WhitelistAvailabilityEventEntity>

    @Transaction
    suspend fun replaceAll(
        summary: WhitelistAvailabilitySummaryEntity,
        targets: List<WhitelistTargetAvailabilityEntity>,
        daily: List<WhitelistDailyAvailabilityEntity>,
        events: List<WhitelistAvailabilityEventEntity>,
    ) {
        clearEvents()
        clearTargets()
        clearDaily()
        upsertSummary(summary)
        if (targets.isNotEmpty()) upsertTargets(targets)
        if (daily.isNotEmpty()) upsertDaily(daily)
        if (events.isNotEmpty()) insertEvents(events)
    }
}
