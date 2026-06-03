package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.model.history.CheckRun
import com.whitelistchecker.domain.model.history.CheckRunOverallStatus
import com.whitelistchecker.domain.model.history.CheckRunWithTargetResults
import com.whitelistchecker.domain.model.history.CheckTargetResult
import com.whitelistchecker.domain.model.history.CheckTargetResultStatus
import com.whitelistchecker.domain.model.history.CheckTriggerType
import com.whitelistchecker.domain.model.WhitelistState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneId

class CheckStatisticsCalculatorTest {

    private val calculator = CheckStatisticsCalculator()
    private val zoneId = ZoneId.of("UTC")

    @Test
    fun `success rate is null when total is zero`() {
        assertNull(calculator.computeSuccessRate(0, 0))
    }

    @Test
    fun `single success run updates summary`() {
        val snapshot = calculator.applyRun(
            calculator.emptySnapshot(),
            sampleRun(CheckRunOverallStatus.SUCCESS),
            listOf(sampleTarget(success = true)),
        )
        assertEquals(1, snapshot.summary.totalRuns)
        assertEquals(1, snapshot.summary.successRuns)
        assertEquals(0, snapshot.summary.failureRuns)
        assertEquals(1.0, snapshot.summary.successRate!!, 0.001)
        assertEquals(100L, snapshot.summary.averageLatencyMs)
        assertEquals(1_000L, snapshot.summary.lastSuccessAt)
        assertEquals(0, snapshot.summary.consecutiveFailureCount)
    }

    @Test
    fun `single failure run updates summary`() {
        val snapshot = calculator.applyRun(
            calculator.emptySnapshot(),
            sampleRun(CheckRunOverallStatus.FAILURE),
            emptyList(),
        )
        assertEquals(1, snapshot.summary.totalRuns)
        assertEquals(1, snapshot.summary.failureRuns)
        assertEquals(1, snapshot.summary.consecutiveFailureCount)
        assertEquals(1_000L, snapshot.summary.lastFailureAt)
    }

    @Test
    fun `partial failure increments partial counter`() {
        val snapshot = calculator.applyRun(
            calculator.emptySnapshot(),
            sampleRun(CheckRunOverallStatus.PARTIAL_FAILURE),
            listOf(
                sampleTarget(success = true),
                sampleTarget(success = false, targetId = "https://ya.ru"),
            ),
        )
        assertEquals(1, snapshot.summary.partialFailureRuns)
        assertEquals(1, snapshot.summary.consecutiveFailureCount)
    }

    @Test
    fun `consecutive failures reset after success`() {
        var snapshot = calculator.emptySnapshot()
        snapshot = calculator.applyRun(snapshot, sampleRun(CheckRunOverallStatus.FAILURE), emptyList())
        snapshot = calculator.applyRun(snapshot, sampleRun(CheckRunOverallStatus.FAILURE, finishedAt = 2_000L), emptyList())
        assertEquals(2, snapshot.summary.consecutiveFailureCount)
        snapshot = calculator.applyRun(snapshot, sampleRun(CheckRunOverallStatus.SUCCESS, finishedAt = 3_000L), emptyList())
        assertEquals(0, snapshot.summary.consecutiveFailureCount)
    }

    @Test
    fun `target statistics track per target`() {
        val snapshot = calculator.applyRun(
            calculator.emptySnapshot(),
            sampleRun(CheckRunOverallStatus.SUCCESS),
            listOf(sampleTarget(success = true, targetId = "https://a.test")),
        )
        val target = snapshot.targets.getValue("https://a.test")
        assertEquals(1, target.totalChecks)
        assertEquals(1, target.successChecks)
        assertEquals(1.0, target.successRate!!, 0.001)
    }

    @Test
    fun `route kind statistics are separated`() {
        val snapshot = calculator.applyRun(
            calculator.emptySnapshot(),
            sampleRun(CheckRunOverallStatus.PARTIAL_FAILURE),
            listOf(
                sampleTarget(success = true, routeKind = "FOREIGN"),
                sampleTarget(success = false, routeKind = "LOCAL", targetId = "https://ya.ru"),
            ),
        )
        assertEquals(1, snapshot.routeKinds.getValue("FOREIGN").successChecks)
        assertEquals(1, snapshot.routeKinds.getValue("LOCAL").failureChecks)
    }

    @Test
    fun `network statistics use composite key`() {
        val run = sampleRun(CheckRunOverallStatus.SUCCESS).copy(
            networkType = "Wi-Fi",
            operatorName = "TestOp",
        )
        val snapshot = calculator.applyRun(
            calculator.emptySnapshot(),
            run,
            listOf(sampleTarget(success = true)),
        )
        val key = calculator.networkKey("Wi-Fi", "TestOp")
        assertEquals(1, snapshot.networks.getValue(key).totalRuns)
    }

    @Test
    fun `daily statistics bucket by date`() {
        val finishedAt = 1_704_067_200_000L // 2024-01-01 UTC
        val snapshot = calculator.applyRun(
            calculator.emptySnapshot(),
            sampleRun(CheckRunOverallStatus.SUCCESS, finishedAt = finishedAt),
            listOf(sampleTarget(success = true)),
        )
        val dateKey = calculator.formatDateKey(finishedAt, zoneId)
        assertEquals(1, snapshot.daily.getValue(dateKey).totalRuns)
        assertEquals(1, snapshot.daily.getValue(dateKey).successTargetChecks)
    }

    @Test
    fun `rebuild matches sequential apply`() {
        val runs = listOf(
            CheckRunWithTargetResults(
                sampleRun(CheckRunOverallStatus.SUCCESS, finishedAt = 1_000L),
                listOf(sampleTarget(success = true)),
            ),
            CheckRunWithTargetResults(
                sampleRun(CheckRunOverallStatus.FAILURE, finishedAt = 2_000L),
                emptyList(),
            ),
        )
        var sequential = calculator.emptySnapshot()
        runs.forEach { entry ->
            sequential = calculator.applyRun(sequential, entry.run, entry.targetResults)
        }
        val rebuilt = calculator.rebuildFromHistory(runs)
        assertEquals(sequential.summary, rebuilt.summary)
        assertEquals(sequential.targets, rebuilt.targets)
    }

    private fun sampleRun(
        status: CheckRunOverallStatus,
        finishedAt: Long = 1_000L,
    ): CheckRun {
        return CheckRun(
            id = "run-$finishedAt",
            startedAtMillis = finishedAt - 100,
            finishedAtMillis = finishedAt,
            durationMs = 100,
            triggerType = CheckTriggerType.MANUAL,
            networkType = "Wi-Fi",
            operatorName = null,
            routeMode = "CELLULAR",
            overallStatus = status,
            whitelistState = WhitelistState.WHITELIST_OFF,
            successCount = 1,
            failureCount = 0,
            skippedCount = 0,
            appVersion = "0.8.1",
            schemaVersion = 1,
            createdAtMillis = finishedAt,
            checkError = null,
            diagnosticsMessage = null,
        )
    }

    private fun sampleTarget(
        success: Boolean,
        targetId: String = "https://google.com",
        routeKind: String = "FOREIGN",
    ): CheckTargetResult {
        return CheckTargetResult(
            id = "target-$targetId",
            checkRunId = "run",
            targetId = targetId,
            targetLabel = "Label",
            targetHost = targetId,
            routeKind = routeKind,
            status = if (success) {
                CheckTargetResultStatus.SUCCESS
            } else {
                CheckTargetResultStatus.DNS_ERROR
            },
            latencyMs = 100L,
            httpStatusCode = if (success) 204 else null,
            errorCode = if (success) null else "dns",
            errorCategory = "DNS",
            startedAtMillis = 900L,
            finishedAtMillis = 1_000L,
            createdAtMillis = 1_000L,
        )
    }
}
