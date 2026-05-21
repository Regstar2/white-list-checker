package com.whitelistchecker.domain.model

data class WhitelistMonitorResult(
    val checkResult: NetworkCheckResult,
    val monitorState: WhitelistMonitorState,
    val stateChangeEvent: WhitelistStateChangeEvent?,
)
