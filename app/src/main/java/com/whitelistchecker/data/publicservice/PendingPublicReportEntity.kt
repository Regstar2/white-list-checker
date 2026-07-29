package com.whitelistchecker.data.publicservice

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pending_public_reports",
    indices = [Index(value = ["createdAtMillis"])],
)
data class PendingPublicReportEntity(
    @PrimaryKey
    val reportId: String,
    val payloadJson: String,
    val createdAtMillis: Long,
    val checkedAtMillis: Long,
    val attemptCount: Int,
    val lastAttemptAtMillis: Long?,
    val lastError: String?,
)
