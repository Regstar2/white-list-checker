package com.whitelistchecker.domain.model.availability

data class WhitelistDailyAvailability(
    val date: String,
    val availableTargetCount: Int = 0,
    val unavailableTargetCount: Int = 0,
    val becameAvailableCount: Int = 0,
    val becameUnavailableCount: Int = 0,
    val availabilityPercent: Double? = null,
    val checkRunCount: Int = 0,
)
