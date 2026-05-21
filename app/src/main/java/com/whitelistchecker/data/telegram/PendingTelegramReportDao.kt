package com.whitelistchecker.data.telegram

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingTelegramReportDao {

    @Query("SELECT * FROM pending_telegram_reports ORDER BY createdAtMillis ASC")
    suspend fun getAll(): List<PendingTelegramReportEntity>

    @Query("SELECT COUNT(*) FROM pending_telegram_reports")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingTelegramReportEntity)

    @Delete
    suspend fun delete(entity: PendingTelegramReportEntity)

    @Query("DELETE FROM pending_telegram_reports WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM pending_telegram_reports WHERE createdAtMillis < :thresholdMillis")
    suspend fun deleteOlderThan(thresholdMillis: Long)

    @Query("DELETE FROM pending_telegram_reports")
    suspend fun clear()

    @Query(
        """
        UPDATE pending_telegram_reports
        SET attemptCount = :attemptCount,
            lastAttemptAtMillis = :lastAttemptAtMillis,
            lastError = :lastError
        WHERE id = :id
        """,
    )
    suspend fun updateAttempt(
        id: String,
        attemptCount: Int,
        lastAttemptAtMillis: Long,
        lastError: String?,
    )
}
