package com.whitelistchecker.data.publicservice

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingPublicReportDao {

    @Query("SELECT * FROM pending_public_reports ORDER BY createdAtMillis ASC")
    suspend fun getAll(): List<PendingPublicReportEntity>

    @Query("SELECT COUNT(*) FROM pending_public_reports")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PendingPublicReportEntity)

    @Query("DELETE FROM pending_public_reports WHERE reportId = :reportId")
    suspend fun deleteByReportId(reportId: String)

    @Query("DELETE FROM pending_public_reports WHERE createdAtMillis < :thresholdMillis")
    suspend fun deleteOlderThan(thresholdMillis: Long)

    @Query("DELETE FROM pending_public_reports")
    suspend fun clear()

    @Query(
        """
        UPDATE pending_public_reports
        SET attemptCount = :attemptCount,
            lastAttemptAtMillis = :lastAttemptAtMillis,
            lastError = :lastError
        WHERE reportId = :reportId
        """,
    )
    suspend fun updateAttempt(
        reportId: String,
        attemptCount: Int,
        lastAttemptAtMillis: Long,
        lastError: String?,
    )
}
