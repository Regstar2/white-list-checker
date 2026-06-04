package com.whitelistchecker.data.availability

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whitelist_target_availability")
data class WhitelistTargetAvailabilityEntity(
    @PrimaryKey
    val targetId: String,
    val displayLabel: String,
    val currentState: String,
    val becameAvailableCount: Int,
    val becameUnavailableCount: Int,
    val availabilityPercent: Double?,
    val lastBecameAvailableAt: Long?,
    val lastBecameUnavailableAt: Long?,
    val lastSeenAt: Long?,
    val unstableScore: Int,
    val availableChecks: Int,
    val unavailableChecks: Int,
)
