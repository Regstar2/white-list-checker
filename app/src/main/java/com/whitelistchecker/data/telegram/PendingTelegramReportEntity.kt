package com.whitelistchecker.data.telegram

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_telegram_reports")
data class PendingTelegramReportEntity(
    @PrimaryKey
    val id: String,
    val text: String,
    val eventType: String,
    val oldState: String,
    val newState: String,
    val recipientId: String,
    val chatId: String,
    val recipientName: String,
    val createdAtMillis: Long,
    val attemptCount: Int,
    val lastAttemptAtMillis: Long?,
    val lastError: String?,
)
