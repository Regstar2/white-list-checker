package com.whitelistchecker.data.statistics

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.whitelistchecker.domain.statistics.CheckStatisticsConfig

@Entity(tableName = "check_statistics_summary")
data class CheckStatisticsSummaryEntity(
    @PrimaryKey
    val id: Int = CheckStatisticsConfig.SUMMARY_ROW_ID,
    val totalRuns: Int,
    val successRuns: Int,
    val partialFailureRuns: Int,
    val failureRuns: Int,
    val cancelledRuns: Int,
    val unknownRuns: Int,
    val successRate: Double?,
    val averageLatencyMs: Long?,
    val lastRunAt: Long?,
    val lastSuccessAt: Long?,
    val lastFailureAt: Long?,
    val consecutiveFailureCount: Int,
    val updatedAt: Long,
    val latencySampleCount: Int,
    val latencySumMs: Long,
    val schemaVersion: Int,
)
