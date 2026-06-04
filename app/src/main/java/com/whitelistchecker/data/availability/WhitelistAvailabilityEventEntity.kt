package com.whitelistchecker.data.availability

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "whitelist_availability_events",
    indices = [Index("detectedAt"), Index("targetId")],
)
data class WhitelistAvailabilityEventEntity(
    @PrimaryKey
    val id: String,
    val targetId: String,
    val targetLabel: String,
    val previousState: String,
    val newState: String,
    val transitionType: String,
    val detectedAt: Long,
    val checkRunId: String,
    val routeKind: String?,
    val networkType: String?,
    val operatorName: String?,
    val latencyMs: Long?,
    val errorCode: String?,
    val createdAt: Long,
)
