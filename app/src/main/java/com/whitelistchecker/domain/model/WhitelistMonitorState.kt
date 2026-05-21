package com.whitelistchecker.domain.model

data class WhitelistMonitorState(
    val lastConfirmedState: WhitelistState = WhitelistState.UNKNOWN,
    val pendingState: WhitelistState = WhitelistState.UNKNOWN,
    val pendingStateCount: Int = 0,
    val lastConfirmedAtMillis: Long? = null,
    val lastStateChangeAtMillis: Long? = null,
)
