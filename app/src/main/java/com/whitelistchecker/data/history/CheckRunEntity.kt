package com.whitelistchecker.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "check_runs")
data class CheckRunEntity(
    @PrimaryKey
    val id: String,
    val startedAtMillis: Long,
    val finishedAtMillis: Long,
    val durationMs: Long,
    val triggerType: String,
    val networkType: String,
    val operatorName: String?,
    val routeMode: String,
    val overallStatus: String,
    val whitelistState: String,
    val successCount: Int,
    val failureCount: Int,
    val skippedCount: Int,
    val appVersion: String,
    val schemaVersion: Int,
    val createdAtMillis: Long,
    val checkError: String?,
    val diagnosticsMessage: String?,
)
