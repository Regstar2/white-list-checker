package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.model.statistics.CheckStatisticsSummary
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsConsistencyCheckerTest {

    private val checker = StatisticsConsistencyChecker()
    private val nowMillis = 1_000_000L

    @Test
    fun `consistent empty data has no warnings`() {
        val report = checker.check(
            checkRunCount = 0,
            targetResultCount = 0,
            targetStatisticsCount = 0,
            summary = CheckStatisticsSummary(),
            summaryExists = false,
            nowMillis = nowMillis,
        )
        assertFalse(report.hasWarnings)
    }

    @Test
    fun `check runs without summary produces warning`() {
        val report = checker.check(
            checkRunCount = 3,
            targetResultCount = 6,
            targetStatisticsCount = 0,
            summary = CheckStatisticsSummary(),
            summaryExists = false,
            nowMillis = nowMillis,
        )
        assertTrue(
            report.warnings.contains(StatisticsConsistencyWarningCode.CHECK_RUNS_WITHOUT_SUMMARY),
        )
    }

    @Test
    fun `outcome sum exceeding total produces warning`() {
        val report = checker.check(
            checkRunCount = 2,
            targetResultCount = 2,
            targetStatisticsCount = 1,
            summary = CheckStatisticsSummary(
                totalRuns = 2,
                successRuns = 2,
                failureRuns = 2,
            ),
            summaryExists = true,
            nowMillis = nowMillis,
        )
        assertTrue(
            report.warnings.contains(StatisticsConsistencyWarningCode.OUTCOME_SUM_EXCEEDS_TOTAL),
        )
    }

    @Test
    fun `nan success rate produces warning`() {
        val report = checker.check(
            checkRunCount = 1,
            targetResultCount = 1,
            targetStatisticsCount = 1,
            summary = CheckStatisticsSummary(
                totalRuns = 1,
                successRuns = 1,
                successRate = Double.NaN,
            ),
            summaryExists = true,
            nowMillis = nowMillis,
        )
        assertTrue(
            report.warnings.contains(StatisticsConsistencyWarningCode.INVALID_SUCCESS_RATE),
        )
    }

    @Test
    fun `negative latency produces warning`() {
        val report = checker.check(
            checkRunCount = 1,
            targetResultCount = 1,
            targetStatisticsCount = 1,
            summary = CheckStatisticsSummary(
                totalRuns = 1,
                averageLatencyMs = -5L,
            ),
            summaryExists = true,
            nowMillis = nowMillis,
        )
        assertTrue(
            report.warnings.contains(StatisticsConsistencyWarningCode.INVALID_AVERAGE_LATENCY),
        )
    }

    @Test
    fun `negative consecutive failures produces warning`() {
        val report = checker.check(
            checkRunCount = 1,
            targetResultCount = 0,
            targetStatisticsCount = 0,
            summary = CheckStatisticsSummary(
                totalRuns = 1,
                consecutiveFailureCount = -1,
            ),
            summaryExists = true,
            nowMillis = nowMillis,
        )
        assertTrue(
            report.warnings.contains(StatisticsConsistencyWarningCode.NEGATIVE_CONSECUTIVE_FAILURES),
        )
    }
}
