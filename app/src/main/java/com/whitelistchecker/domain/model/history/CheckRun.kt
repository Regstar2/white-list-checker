package com.whitelistchecker.domain.model.history

import com.whitelistchecker.domain.model.WhitelistState

data class CheckRun(
    val id: String,
    val startedAtMillis: Long,
    val finishedAtMillis: Long,
    val durationMs: Long,
    val triggerType: CheckTriggerType,
    val networkType: String,
    val operatorName: String?,
    val routeMode: String,
    val overallStatus: CheckRunOverallStatus,
    val whitelistState: WhitelistState,
    val successCount: Int,
    val failureCount: Int,
    val skippedCount: Int,
    val appVersion: String,
    val schemaVersion: Int,
    val createdAtMillis: Long,
    val checkError: String?,
    val diagnosticsMessage: String?,
)
