package com.whitelistchecker.data.statistics

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "target_statistics")
data class TargetStatisticsEntity(
    @PrimaryKey
    val targetId: String,
    val targetLabel: String,
    val targetHost: String,
    val totalChecks: Int,
    val successChecks: Int,
    val failureChecks: Int,
    val timeoutChecks: Int,
    val successRate: Double?,
    val averageLatencyMs: Long?,
    val lastCheckedAt: Long?,
    val lastSuccessAt: Long?,
    val lastFailureAt: Long?,
    val consecutiveFailureCount: Int,
    val updatedAt: Long,
    val latencySampleCount: Int,
    val latencySumMs: Long,
)
