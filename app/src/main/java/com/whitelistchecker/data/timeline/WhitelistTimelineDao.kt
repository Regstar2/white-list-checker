package com.whitelistchecker.data.timeline

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WhitelistTimelineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSample(sample: WhitelistTimelineSampleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSamples(samples: List<WhitelistTimelineSampleEntity>)

    @Query(
        """
        SELECT * FROM whitelist_timeline_samples
        WHERE checkedAtMillis >= :cutoffMillis
        ORDER BY checkedAtMillis ASC
        """,
    )
    suspend fun getSamplesSince(cutoffMillis: Long): List<WhitelistTimelineSampleEntity>

    @Query("SELECT COUNT(*) FROM whitelist_timeline_samples")
    suspend fun countSamples(): Int

    @Query("DELETE FROM whitelist_timeline_samples")
    suspend fun clear()

    @Query("DELETE FROM whitelist_timeline_samples WHERE checkedAtMillis < :olderThanMillis")
    suspend fun deleteOlderThan(olderThanMillis: Long)

    @Query(
        """
        SELECT checkRunId FROM whitelist_timeline_samples
        ORDER BY checkedAtMillis ASC
        LIMIT :count
        """,
    )
    suspend fun getOldestSampleIds(count: Int): List<String>

    @Query("DELETE FROM whitelist_timeline_samples WHERE checkRunId IN (:ids)")
    suspend fun deleteSamplesByIds(ids: List<String>)
}
