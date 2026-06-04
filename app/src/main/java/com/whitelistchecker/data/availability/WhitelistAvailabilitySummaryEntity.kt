package com.whitelistchecker.data.availability

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whitelist_availability_summary")
data class WhitelistAvailabilitySummaryEntity(
    @PrimaryKey
    val id: Int,
    val totalTargets: Int,
    val currentlyAvailableTargets: Int,
    val currentlyUnavailableTargets: Int,
    val unknownTargets: Int,
    val totalBecameAvailableEvents: Int,
    val totalBecameUnavailableEvents: Int,
    val availabilityPercent: Double?,
    val lastBecameAvailableAt: Long?,
    val lastBecameUnavailableAt: Long?,
    val lastUpdatedAt: Long,
    val dataRangeStart: Long?,
    val dataRangeEnd: Long?,
    val mostStableTargetLabel: String?,
    val mostUnstableTargetLabel: String?,
    val schemaVersion: Int,
)
