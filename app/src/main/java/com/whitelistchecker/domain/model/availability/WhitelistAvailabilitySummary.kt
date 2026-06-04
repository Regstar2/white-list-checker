package com.whitelistchecker.domain.model.availability

data class WhitelistAvailabilitySummary(
    val totalTargets: Int = 0,
    val currentlyAvailableTargets: Int = 0,
    val currentlyUnavailableTargets: Int = 0,
    val unknownTargets: Int = 0,
    val totalBecameAvailableEvents: Int = 0,
    val totalBecameUnavailableEvents: Int = 0,
    val availabilityPercent: Double? = null,
    val lastBecameAvailableAt: Long? = null,
    val lastBecameUnavailableAt: Long? = null,
    val lastUpdatedAt: Long = 0L,
    val dataRangeStart: Long? = null,
    val dataRangeEnd: Long? = null,
    val mostStableTargetLabel: String? = null,
    val mostUnstableTargetLabel: String? = null,
)
