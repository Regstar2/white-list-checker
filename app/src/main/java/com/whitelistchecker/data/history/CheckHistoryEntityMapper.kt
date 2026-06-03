package com.whitelistchecker.data.history

import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.history.CheckRun
import com.whitelistchecker.domain.model.history.CheckRunOverallStatus
import com.whitelistchecker.domain.model.history.CheckRunWithTargetResults
import com.whitelistchecker.domain.model.history.CheckTargetResult
import com.whitelistchecker.domain.model.history.CheckTargetResultStatus
import com.whitelistchecker.domain.model.history.CheckTriggerType

internal object CheckHistoryEntityMapper {

    fun toEntity(run: CheckRun): CheckRunEntity {
        return CheckRunEntity(
            id = run.id,
            startedAtMillis = run.startedAtMillis,
            finishedAtMillis = run.finishedAtMillis,
            durationMs = run.durationMs,
            triggerType = run.triggerType.name,
            networkType = run.networkType,
            operatorName = run.operatorName,
            routeMode = run.routeMode,
            overallStatus = run.overallStatus.name,
            whitelistState = run.whitelistState.name,
            successCount = run.successCount,
            failureCount = run.failureCount,
            skippedCount = run.skippedCount,
            appVersion = run.appVersion,
            schemaVersion = run.schemaVersion,
            createdAtMillis = run.createdAtMillis,
            checkError = run.checkError,
            diagnosticsMessage = run.diagnosticsMessage,
        )
    }

    fun toEntity(target: CheckTargetResult): CheckTargetResultEntity {
        return CheckTargetResultEntity(
            id = target.id,
            checkRunId = target.checkRunId,
            targetId = target.targetId,
            targetLabel = target.targetLabel,
            targetHost = target.targetHost,
            routeKind = target.routeKind,
            status = target.status.name,
            latencyMs = target.latencyMs,
            httpStatusCode = target.httpStatusCode,
            errorCode = target.errorCode,
            errorCategory = target.errorCategory,
            startedAtMillis = target.startedAtMillis,
            finishedAtMillis = target.finishedAtMillis,
            createdAtMillis = target.createdAtMillis,
        )
    }

    fun toDomain(entity: CheckRunWithTargetResultsEntity): CheckRunWithTargetResults {
        return CheckRunWithTargetResults(
            run = toDomain(entity.run),
            targetResults = entity.targetResults.map(::toDomain),
        )
    }

    private fun toDomain(entity: CheckRunEntity): CheckRun {
        return CheckRun(
            id = entity.id,
            startedAtMillis = entity.startedAtMillis,
            finishedAtMillis = entity.finishedAtMillis,
            durationMs = entity.durationMs,
            triggerType = parseTriggerType(entity.triggerType),
            networkType = entity.networkType,
            operatorName = entity.operatorName,
            routeMode = entity.routeMode,
            overallStatus = parseOverallStatus(entity.overallStatus),
            whitelistState = parseWhitelistState(entity.whitelistState),
            successCount = entity.successCount,
            failureCount = entity.failureCount,
            skippedCount = entity.skippedCount,
            appVersion = entity.appVersion,
            schemaVersion = entity.schemaVersion,
            createdAtMillis = entity.createdAtMillis,
            checkError = entity.checkError,
            diagnosticsMessage = entity.diagnosticsMessage,
        )
    }

    private fun toDomain(entity: CheckTargetResultEntity): CheckTargetResult {
        return CheckTargetResult(
            id = entity.id,
            checkRunId = entity.checkRunId,
            targetId = entity.targetId,
            targetLabel = entity.targetLabel,
            targetHost = entity.targetHost,
            routeKind = entity.routeKind,
            status = parseTargetStatus(entity.status),
            latencyMs = entity.latencyMs,
            httpStatusCode = entity.httpStatusCode,
            errorCode = entity.errorCode,
            errorCategory = entity.errorCategory,
            startedAtMillis = entity.startedAtMillis,
            finishedAtMillis = entity.finishedAtMillis,
            createdAtMillis = entity.createdAtMillis,
        )
    }

    private fun parseTriggerType(value: String): CheckTriggerType {
        return runCatching { CheckTriggerType.valueOf(value) }
            .getOrDefault(CheckTriggerType.MANUAL)
    }

    private fun parseOverallStatus(value: String): CheckRunOverallStatus {
        return runCatching { CheckRunOverallStatus.valueOf(value) }
            .getOrDefault(CheckRunOverallStatus.UNKNOWN)
    }

    private fun parseTargetStatus(value: String): CheckTargetResultStatus {
        return runCatching { CheckTargetResultStatus.valueOf(value) }
            .getOrDefault(CheckTargetResultStatus.UNKNOWN)
    }

    private fun parseWhitelistState(value: String): WhitelistState {
        return runCatching { WhitelistState.valueOf(value) }
            .getOrDefault(WhitelistState.UNKNOWN)
    }
}
