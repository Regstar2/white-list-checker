package com.whitelistchecker.domain.model

data class StateChangeDetectionResult(
    val updatedMonitorState: WhitelistMonitorState,
    val event: WhitelistStateChangeEvent?,
)
