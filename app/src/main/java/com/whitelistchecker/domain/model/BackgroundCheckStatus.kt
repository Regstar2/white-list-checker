package com.whitelistchecker.domain.model

data class BackgroundCheckStatus(
    val lastRunAtMillis: Long? = null,
    val lastFinishedAtMillis: Long? = null,
    val lastState: WhitelistState = WhitelistState.UNKNOWN,
    val lastError: String? = null,
    val lastTelegramSendResult: String? = null,
    val lastQueueFlushSummary: String? = null,
)
