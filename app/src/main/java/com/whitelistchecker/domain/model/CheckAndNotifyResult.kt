package com.whitelistchecker.domain.model

data class CheckAndNotifyResult(
    val monitorResult: WhitelistMonitorResult,
    val localNotificationResult: LocalNotificationResult?,
    val telegramSendResult: TelegramSendResult?,
    val queueFlushResult: TelegramQueueFlushResult?,
    val pendingReportsCount: Int,
)
