package com.whitelistchecker.domain.model

data class PendingTelegramReport(
    val id: String,
    val text: String,
    val eventType: WhitelistStateChangeType,
    val oldState: WhitelistState,
    val newState: WhitelistState,
    val createdAtMillis: Long,
    val attemptCount: Int,
    val lastAttemptAtMillis: Long?,
    val lastError: String?,
)
