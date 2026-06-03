package com.whitelistchecker.data.history

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface CheckHistoryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRun(run: CheckRunEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTargetResults(targets: List<CheckTargetResultEntity>)

    @Transaction
    suspend fun insertCheckRunWithTargets(
        run: CheckRunEntity,
        targets: List<CheckTargetResultEntity>,
    ) {
        insertRun(run)
        if (targets.isNotEmpty()) {
            insertTargetResults(targets)
        }
    }

    @Transaction
    @Query("SELECT * FROM check_runs ORDER BY finishedAtMillis DESC, createdAtMillis DESC LIMIT 1")
    suspend fun getLatestWithTargets(): CheckRunWithTargetResultsEntity?

    @Transaction
    @Query(
        """
        SELECT * FROM check_runs
        ORDER BY finishedAtMillis DESC, createdAtMillis DESC
        LIMIT :limit
        """,
    )
    suspend fun getRecentWithTargets(limit: Int): List<CheckRunWithTargetResultsEntity>

    @Query("SELECT id FROM check_runs WHERE finishedAtMillis < :olderThanMillis")
    suspend fun getRunIdsOlderThan(olderThanMillis: Long): List<String>

    @Query("SELECT id FROM check_runs ORDER BY finishedAtMillis ASC, createdAtMillis ASC")
    suspend fun getRunIdsOldestFirst(): List<String>

    @Query("SELECT COUNT(*) FROM check_runs")
    suspend fun countRuns(): Int

    @Query("DELETE FROM check_runs WHERE id IN (:runIds)")
    suspend fun deleteRunsByIds(runIds: List<String>)
}
