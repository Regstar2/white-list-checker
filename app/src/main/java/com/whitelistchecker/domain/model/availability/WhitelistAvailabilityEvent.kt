package com.whitelistchecker.domain.model.availability

data class WhitelistAvailabilityEvent(
    val id: String,
    val targetId: String,
    val targetLabel: String,
    val previousState: WhitelistAvailabilityState,
    val newState: WhitelistAvailabilityState,
    val transitionType: WhitelistAvailabilityTransitionType,
    val detectedAt: Long,
    val checkRunId: String,
    val routeKind: String?,
    val networkType: String?,
    val operatorName: String?,
    val latencyMs: Long?,
    val errorCode: String?,
    val createdAt: Long,
)
