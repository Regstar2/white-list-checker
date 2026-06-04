package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.model.history.CheckRun
import com.whitelistchecker.domain.model.history.CheckRunOverallStatus
import com.whitelistchecker.domain.model.history.CheckRunWithTargetResults
import com.whitelistchecker.domain.model.history.CheckTargetResult
import com.whitelistchecker.domain.model.history.CheckTargetResultStatus
import com.whitelistchecker.domain.model.statistics.CheckStatisticsSnapshot
import com.whitelistchecker.domain.model.statistics.CheckStatisticsSummary
import com.whitelistchecker.domain.model.statistics.DailyCheckStatistics
import com.whitelistchecker.domain.model.statistics.NetworkStatistics
import com.whitelistchecker.domain.model.statistics.RouteKindStatistics
import com.whitelistchecker.domain.model.statistics.TargetStatistics
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CheckStatisticsCalculator {

    fun emptySnapshot(): CheckStatisticsSnapshot = CheckStatisticsSnapshot()

    fun rebuildFromHistory(
        runs: List<CheckRunWithTargetResults>,
    ): CheckStatisticsSnapshot {
        var snapshot = emptySnapshot()
        val ordered = runs.sortedBy { it.run.finishedAtMillis }
        ordered.forEach { entry ->
            snapshot = applyRun(snapshot, entry.run, entry.targetResults)
        }
        return snapshot
    }

    fun applyRun(
        snapshot: CheckStatisticsSnapshot,
        run: CheckRun,
        targetResults: List<CheckTargetResult>,
    ): CheckStatisticsSnapshot {
        val updatedAt = run.finishedAtMillis
        return CheckStatisticsSnapshot(
            summary = applyRunToSummary(snapshot.summary, run, targetResults, updatedAt),
            targets = applyRunToTargets(snapshot.targets, targetResults, updatedAt),
            routeKinds = applyRunToRouteKinds(snapshot.routeKinds, targetResults, updatedAt),
            networks = applyRunToNetworks(snapshot.networks, run, targetResults, updatedAt),
            daily = applyRunToDaily(snapshot.daily, run, targetResults, updatedAt),
        )
    }

    fun computeSuccessRate(successCount: Int, totalCount: Int): Double? {
        if (totalCount == 0) return null
        return successCount.toDouble() / totalCount.toDouble()
    }

    fun computeAverageLatency(sumMs: Long, sampleCount: Int): Long? {
        if (sampleCount == 0) return null
        return sumMs / sampleCount
    }

    fun formatDateKey(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(zoneId)
            .format(DATE_FORMATTER)
    }

    fun networkKey(networkType: String, operatorName: String?): String {
        return if (operatorName.isNullOrBlank()) {
            networkType
        } else {
            "$networkType::$operatorName"
        }
    }

    private fun applyRunToSummary(
        summary: CheckStatisticsSummary,
        run: CheckRun,
        targetResults: List<CheckTargetResult>,
        updatedAt: Long,
    ): CheckStatisticsSummary {
        val (latencySum, latencyCount) = addTargetLatencies(
            summary.latencySumMs,
            summary.latencySampleCount,
            targetResults,
        )
        val counts = incrementRunStatusCounts(summary, run.overallStatus)
        val consecutiveFailures = nextConsecutiveFailureCount(
            current = summary.consecutiveFailureCount,
            status = run.overallStatus,
        )
        val lastSuccessAt = if (run.overallStatus == CheckRunOverallStatus.SUCCESS) {
            run.finishedAtMillis
        } else {
            summary.lastSuccessAt
        }
        val lastFailureAt = if (isFailureRun(run.overallStatus)) {
            run.finishedAtMillis
        } else {
            summary.lastFailureAt
        }
        val successRateTotal = counts.totalRuns - counts.cancelledRuns - counts.unknownRuns
        val successRateNumerator = counts.successRuns
        return counts.copy(
            successRate = computeSuccessRate(successRateNumerator, successRateTotal),
            averageLatencyMs = computeAverageLatency(latencySum, latencyCount),
            lastRunAt = run.finishedAtMillis,
            lastSuccessAt = lastSuccessAt,
            lastFailureAt = lastFailureAt,
            consecutiveFailureCount = consecutiveFailures,
            updatedAt = updatedAt,
            latencySampleCount = latencyCount,
            latencySumMs = latencySum,
        )
    }

    private fun incrementRunStatusCounts(
        summary: CheckStatisticsSummary,
        status: CheckRunOverallStatus,
    ): CheckStatisticsSummary {
        return when (status) {
            CheckRunOverallStatus.SUCCESS -> summary.copy(
                totalRuns = summary.totalRuns + 1,
                successRuns = summary.successRuns + 1,
            )
            CheckRunOverallStatus.PARTIAL_FAILURE -> summary.copy(
                totalRuns = summary.totalRuns + 1,
                partialFailureRuns = summary.partialFailureRuns + 1,
            )
            CheckRunOverallStatus.FAILURE -> summary.copy(
                totalRuns = summary.totalRuns + 1,
                failureRuns = summary.failureRuns + 1,
            )
            CheckRunOverallStatus.CANCELLED -> summary.copy(
                totalRuns = summary.totalRuns + 1,
                cancelledRuns = summary.cancelledRuns + 1,
            )
            CheckRunOverallStatus.UNKNOWN -> summary.copy(
                totalRuns = summary.totalRuns + 1,
                unknownRuns = summary.unknownRuns + 1,
            )
        }
    }

    private fun applyRunToTargets(
        current: Map<String, TargetStatistics>,
        targetResults: List<CheckTargetResult>,
        updatedAt: Long,
    ): Map<String, TargetStatistics> {
        val mutable = current.toMutableMap()
        targetResults.forEach { target ->
            val existing = mutable[target.targetId] ?: TargetStatistics(
                targetId = target.targetId,
                targetLabel = target.targetLabel,
                targetHost = target.targetHost,
            )
            val success = target.status == CheckTargetResultStatus.SUCCESS
            val timeout = target.status == CheckTargetResultStatus.TIMEOUT
            val failure = !success
            val (latencySum, latencyCount) = addSingleLatency(
                existing.latencySumMs,
                existing.latencySampleCount,
                target.latencyMs,
            )
            val consecutive = if (success) {
                0
            } else {
                existing.consecutiveFailureCount + 1
            }
            val updated = existing.copy(
                totalChecks = existing.totalChecks + 1,
                successChecks = existing.successChecks + if (success) 1 else 0,
                failureChecks = existing.failureChecks + if (failure) 1 else 0,
                timeoutChecks = existing.timeoutChecks + if (timeout) 1 else 0,
                successRate = computeSuccessRate(
                    existing.successChecks + if (success) 1 else 0,
                    existing.totalChecks + 1,
                ),
                averageLatencyMs = computeAverageLatency(latencySum, latencyCount),
                lastCheckedAt = target.finishedAtMillis,
                lastSuccessAt = if (success) target.finishedAtMillis else existing.lastSuccessAt,
                lastFailureAt = if (failure) target.finishedAtMillis else existing.lastFailureAt,
                consecutiveFailureCount = consecutive,
                updatedAt = updatedAt,
                latencySampleCount = latencyCount,
                latencySumMs = latencySum,
            )
            mutable[target.targetId] = updated
        }
        return mutable
    }

    private fun applyRunToRouteKinds(
        current: Map<String, RouteKindStatistics>,
        targetResults: List<CheckTargetResult>,
        updatedAt: Long,
    ): Map<String, RouteKindStatistics> {
        val mutable = current.toMutableMap()
        targetResults.forEach { target ->
            val existing = mutable[target.routeKind] ?: RouteKindStatistics(routeKind = target.routeKind)
            val success = target.status == CheckTargetResultStatus.SUCCESS
            val failure = !success
            val (latencySum, latencyCount) = addSingleLatency(
                existing.latencySumMs,
                existing.latencySampleCount,
                target.latencyMs,
            )
            val updated = existing.copy(
                totalChecks = existing.totalChecks + 1,
                successChecks = existing.successChecks + if (success) 1 else 0,
                failureChecks = existing.failureChecks + if (failure) 1 else 0,
                successRate = computeSuccessRate(
                    existing.successChecks + if (success) 1 else 0,
                    existing.totalChecks + 1,
                ),
                averageLatencyMs = computeAverageLatency(latencySum, latencyCount),
                lastCheckedAt = target.finishedAtMillis,
                updatedAt = updatedAt,
                latencySampleCount = latencyCount,
                latencySumMs = latencySum,
            )
            mutable[target.routeKind] = updated
        }
        return mutable
    }

    private fun applyRunToNetworks(
        current: Map<String, NetworkStatistics>,
        run: CheckRun,
        targetResults: List<CheckTargetResult>,
        updatedAt: Long,
    ): Map<String, NetworkStatistics> {
        val key = networkKey(run.networkType, run.operatorName)
        val existing = current[key] ?: NetworkStatistics(
            networkKey = key,
            networkType = run.networkType,
            operatorName = run.operatorName,
        )
        val successRun = run.overallStatus == CheckRunOverallStatus.SUCCESS
        val failureRun = isFailureRun(run.overallStatus)
        val (latencySum, latencyCount) = addTargetLatencies(
            existing.latencySumMs,
            existing.latencySampleCount,
            targetResults,
        )
        val updated = existing.copy(
            totalRuns = existing.totalRuns + 1,
            successRuns = existing.successRuns + if (successRun) 1 else 0,
            failureRuns = existing.failureRuns + if (failureRun) 1 else 0,
            successRate = computeSuccessRate(
                existing.successRuns + if (successRun) 1 else 0,
                existing.totalRuns + 1,
            ),
            averageLatencyMs = computeAverageLatency(latencySum, latencyCount),
            lastRunAt = run.finishedAtMillis,
            updatedAt = updatedAt,
            latencySampleCount = latencyCount,
            latencySumMs = latencySum,
        )
        return current + (key to updated)
    }

    private fun applyRunToDaily(
        current: Map<String, DailyCheckStatistics>,
        run: CheckRun,
        targetResults: List<CheckTargetResult>,
        updatedAt: Long,
    ): Map<String, DailyCheckStatistics> {
        val dateKey = formatDateKey(run.finishedAtMillis)
        val existing = current[dateKey] ?: DailyCheckStatistics(date = dateKey)
        val (latencySum, latencyCount) = addTargetLatencies(
            existing.latencySumMs,
            existing.latencySampleCount,
            targetResults,
        )
        var successTargets = 0
        var failureTargets = 0
        targetResults.forEach { target ->
            if (target.status == CheckTargetResultStatus.SUCCESS) {
                successTargets++
            } else if (isCountableTargetFailure(target.status)) {
                failureTargets++
            }
        }
        val updated = when (run.overallStatus) {
            CheckRunOverallStatus.SUCCESS -> existing.copy(successRuns = existing.successRuns + 1)
            CheckRunOverallStatus.PARTIAL_FAILURE -> existing.copy(
                partialFailureRuns = existing.partialFailureRuns + 1,
            )
            CheckRunOverallStatus.FAILURE -> existing.copy(failureRuns = existing.failureRuns + 1)
            else -> existing
        }.copy(
            totalRuns = existing.totalRuns + 1,
            totalTargetChecks = existing.totalTargetChecks + targetResults.size,
            successTargetChecks = existing.successTargetChecks + successTargets,
            failureTargetChecks = existing.failureTargetChecks + failureTargets,
            averageLatencyMs = computeAverageLatency(latencySum, latencyCount),
            updatedAt = updatedAt,
            latencySampleCount = latencyCount,
            latencySumMs = latencySum,
        )
        return current + (dateKey to updated)
    }

    private fun addTargetLatencies(
        currentSum: Long,
        currentCount: Int,
        targets: List<CheckTargetResult>,
    ): Pair<Long, Int> {
        var sum = currentSum
        var count = currentCount
        targets.forEach { target ->
            val added = addSingleLatency(sum, count, target.latencyMs)
            sum = added.first
            count = added.second
        }
        return sum to count
    }

    private fun addSingleLatency(
        currentSum: Long,
        currentCount: Int,
        latencyMs: Long,
    ): Pair<Long, Int> {
        if (latencyMs < 0) return currentSum to currentCount
        return (currentSum + latencyMs) to (currentCount + 1)
    }

    private fun nextConsecutiveFailureCount(
        current: Int,
        status: CheckRunOverallStatus,
    ): Int {
        return when (status) {
            CheckRunOverallStatus.SUCCESS,
            CheckRunOverallStatus.PARTIAL_FAILURE,
            -> 0
            CheckRunOverallStatus.FAILURE -> current + 1
            CheckRunOverallStatus.CANCELLED,
            CheckRunOverallStatus.UNKNOWN,
            -> current
        }
    }

    private fun isFailureRun(status: CheckRunOverallStatus): Boolean {
        return status == CheckRunOverallStatus.FAILURE
    }

    private fun isCountableTargetFailure(status: CheckTargetResultStatus): Boolean {
        return status != CheckTargetResultStatus.SUCCESS &&
            status != CheckTargetResultStatus.SKIPPED &&
            status != CheckTargetResultStatus.CANCELLED
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}
