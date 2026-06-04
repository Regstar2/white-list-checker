package com.whitelistchecker.data.availability

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whitelist_daily_availability")
data class WhitelistDailyAvailabilityEntity(
    @PrimaryKey
    val date: String,
    val availableTargetCount: Int,
    val unavailableTargetCount: Int,
    val becameAvailableCount: Int,
    val becameUnavailableCount: Int,
    val availabilityPercent: Double?,
    val checkRunCount: Int,
)
