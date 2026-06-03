package com.whitelistchecker.data.statistics

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "route_kind_statistics")
data class RouteKindStatisticsEntity(
    @PrimaryKey
    val routeKind: String,
    val totalChecks: Int,
    val successChecks: Int,
    val failureChecks: Int,
    val successRate: Double?,
    val averageLatencyMs: Long?,
    val lastCheckedAt: Long?,
    val updatedAt: Long,
    val latencySampleCount: Int,
    val latencySumMs: Long,
)
