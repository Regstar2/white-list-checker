package com.whitelistchecker.domain.model

data class ActiveMonitoringStatus(
    val state: ActiveMonitoringState = ActiveMonitoringState.STOPPED,
    val lastCheckAtMillis: Long? = null,
    val lastStopReason: String? = null,
    val lastError: String? = null,
    val telegramCommandOffset: Long? = null,
    val telegramLastError: String? = null,
    val backgroundWasEnabledBeforeStart: Boolean = false,
)
