package com.whitelistchecker.domain.model.history

data class CheckTargetResult(
    val id: String,
    val checkRunId: String,
    val targetId: String,
    val targetLabel: String,
    val targetHost: String,
    val routeKind: String,
    val status: CheckTargetResultStatus,
    val latencyMs: Long,
    val httpStatusCode: Int?,
    val errorCode: String?,
    val errorCategory: String?,
    val startedAtMillis: Long,
    val finishedAtMillis: Long,
    val createdAtMillis: Long,
)
