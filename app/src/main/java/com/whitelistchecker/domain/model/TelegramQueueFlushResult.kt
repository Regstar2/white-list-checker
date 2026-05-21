package com.whitelistchecker.domain.model

data class TelegramQueueFlushResult(
    val attemptedCount: Int,
    val sentCount: Int,
    val failedCount: Int,
    val skippedCount: Int,
    val lastError: String?,
)
