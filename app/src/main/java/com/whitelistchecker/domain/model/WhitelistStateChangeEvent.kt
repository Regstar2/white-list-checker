package com.whitelistchecker.domain.model

data class WhitelistStateChangeEvent(
    val oldState: WhitelistState,
    val newState: WhitelistState,
    val type: WhitelistStateChangeType,
    val changedAtMillis: Long,
)
