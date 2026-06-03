package com.whitelistchecker.data.statistics

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_check_statistics")
data class DailyCheckStatisticsEntity(
    @PrimaryKey
    val date: String,
    val totalRuns: Int,
    val successRuns: Int,
    val partialFailureRuns: Int,
    val failureRuns: Int,
    val totalTargetChecks: Int,
    val successTargetChecks: Int,
    val failureTargetChecks: Int,
    val averageLatencyMs: Long?,
    val updatedAt: Long,
    val latencySampleCount: Int,
    val latencySumMs: Long,
)
