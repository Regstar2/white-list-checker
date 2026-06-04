package com.whitelistchecker.domain.model.availability

data class WhitelistTargetAvailabilityStats(
    val targetId: String,
    val displayLabel: String,
    val currentState: WhitelistAvailabilityState = WhitelistAvailabilityState.UNKNOWN,
    val becameAvailableCount: Int = 0,
    val becameUnavailableCount: Int = 0,
    val availabilityPercent: Double? = null,
    val lastBecameAvailableAt: Long? = null,
    val lastBecameUnavailableAt: Long? = null,
    val lastSeenAt: Long? = null,
    val unstableScore: Int = 0,
    val availableChecks: Int = 0,
    val unavailableChecks: Int = 0,
)
