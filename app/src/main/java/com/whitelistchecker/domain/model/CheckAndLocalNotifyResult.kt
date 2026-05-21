package com.whitelistchecker.domain.model

data class CheckAndLocalNotifyResult(
    val monitorResult: WhitelistMonitorResult,
    val localNotificationResult: LocalNotificationResult?,
)
