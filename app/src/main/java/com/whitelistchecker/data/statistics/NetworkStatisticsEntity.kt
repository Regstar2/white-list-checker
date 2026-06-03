package com.whitelistchecker.data.statistics

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "network_statistics")
data class NetworkStatisticsEntity(
    @PrimaryKey
    val networkKey: String,
    val networkType: String,
    val operatorName: String?,
    val totalRuns: Int,
    val successRuns: Int,
    val failureRuns: Int,
    val successRate: Double?,
    val averageLatencyMs: Long?,
    val lastRunAt: Long?,
    val updatedAt: Long,
    val latencySampleCount: Int,
    val latencySumMs: Long,
)
