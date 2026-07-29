package com.whitelistchecker.domain.history

import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.SiteCheckErrorType
import com.whitelistchecker.domain.model.SiteCheckResult
import com.whitelistchecker.domain.model.history.CheckRun
import com.whitelistchecker.domain.model.history.CheckRunOverallStatus
import com.whitelistchecker.domain.model.history.CheckTargetResult
import com.whitelistchecker.domain.model.history.CheckTargetResultStatus
import com.whitelistchecker.domain.model.history.CheckTriggerType
import java.util.UUID

class CheckHistoryFromNetworkResultMapper {

    fun toCheckRun(
        result: NetworkCheckResult,
        triggerType: CheckTriggerType,
        startedAtMillis: Long,
        finishedAtMillis: Long,
        appVersion: String,
        createdAtMillis: Long = finishedAtMillis,
    ): Pair<CheckRun, List<CheckTargetResult>> {
        val runId = UUID.randomUUID().toString()
        val successCount = result.siteResults.count { it.available }
        val failureCount = result.siteResults.count { !it.available }
        val checkRun = CheckRun(
            id = runId,
            startedAtMillis = startedAtMillis,
            finishedAtMillis = finishedAtMillis,
            durationMs = (finishedAtMillis - startedAtMillis).coerceAtLeast(0L),
            triggerType = triggerType,
            networkType = result.checkedNetworkLabel,
            operatorName = null,
            routeMode = CheckHistoryConfig.ROUTE_MODE_CELLULAR,
            overallStatus = resolveOverallStatus(result, successCount, failureCount),
            whitelistState = result.state,
            successCount = successCount,
            failureCount = failureCount,
            skippedCount = 0,
            appVersion = appVersion,
            schemaVersion = CheckHistoryConfig.SCHEMA_VERSION,
            createdAtMillis = createdAtMillis,
            checkError = result.error,
            diagnosticsMessage = result.diagnosticsMessage,
        )
        val targetResults = result.siteResults.map { site ->
            toTargetResult(
                site = site,
                checkRunId = runId,
                runFinishedAtMillis = finishedAtMillis,
                createdAtMillis = createdAtMillis,
            )
        }
        return checkRun to targetResults
    }

    private fun resolveOverallStatus(
        result: NetworkCheckResult,
        successCount: Int,
        failureCount: Int,
    ): CheckRunOverallStatus {
        if (!result.error.isNullOrBlank() && result.siteResults.isEmpty()) {
            return CheckRunOverallStatus.FAILURE
        }
        return when {
            failureCount == 0 && result.error.isNullOrBlank() -> CheckRunOverallStatus.SUCCESS
            successCount > 0 && failureCount > 0 -> CheckRunOverallStatus.PARTIAL_FAILURE
            failureCount > 0 -> CheckRunOverallStatus.FAILURE
            !result.error.isNullOrBlank() -> CheckRunOverallStatus.FAILURE
            else -> CheckRunOverallStatus.UNKNOWN
        }
    }

    private fun toTargetResult(
        site: SiteCheckResult,
        checkRunId: String,
        runFinishedAtMillis: Long,
        createdAtMillis: Long,
    ): CheckTargetResult {
        val finishedAtMillis = runFinishedAtMillis
        val startedAtMillis = (runFinishedAtMillis - site.durationMs).coerceAtLeast(0L)
        return CheckTargetResult(
            id = UUID.randomUUID().toString(),
            checkRunId = checkRunId,
            targetId = site.target.url,
            targetLabel = site.target.name,
            targetHost = site.target.url,
            routeKind = site.target.group.name,
            status = mapTargetStatus(site),
            latencyMs = site.durationMs,
            httpStatusCode = site.httpCode,
            errorCode = site.error,
            errorCategory = site.errorType.name,
            startedAtMillis = startedAtMillis,
            finishedAtMillis = finishedAtMillis,
            createdAtMillis = createdAtMillis,
        )
    }

    private fun mapTargetStatus(site: SiteCheckResult): CheckTargetResultStatus {
        if (site.available) {
            return CheckTargetResultStatus.SUCCESS
        }
        return when (site.errorType) {
            SiteCheckErrorType.DNS -> CheckTargetResultStatus.DNS_ERROR
            SiteCheckErrorType.TIMEOUT -> CheckTargetResultStatus.TIMEOUT
            SiteCheckErrorType.CONNECTION -> CheckTargetResultStatus.CONNECTION_ERROR
            SiteCheckErrorType.TLS -> CheckTargetResultStatus.TLS_ERROR
            SiteCheckErrorType.HTTP -> CheckTargetResultStatus.FAILURE
            SiteCheckErrorType.UNKNOWN -> CheckTargetResultStatus.UNKNOWN
            SiteCheckErrorType.NONE -> CheckTargetResultStatus.FAILURE
        }
    }
}
