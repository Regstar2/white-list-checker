package com.whitelistchecker.domain.model.availability

data class WhitelistAvailabilitySnapshot(
    val summary: WhitelistAvailabilitySummary = WhitelistAvailabilitySummary(),
    val targets: Map<String, WhitelistTargetAvailabilityStats> = emptyMap(),
    val daily: Map<String, WhitelistDailyAvailability> = emptyMap(),
    val lastKnownStates: Map<String, WhitelistAvailabilityState> = emptyMap(),
)
