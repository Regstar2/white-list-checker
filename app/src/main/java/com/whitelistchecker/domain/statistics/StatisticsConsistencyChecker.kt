package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.model.statistics.CheckStatisticsSummary

class StatisticsConsistencyChecker {

    fun check(
        checkRunCount: Int,
        targetResultCount: Int,
        targetStatisticsCount: Int,
        summary: CheckStatisticsSummary,
        summaryExists: Boolean,
        nowMillis: Long,
    ): StatisticsConsistencyReport {
        val warnings = linkedSetOf<StatisticsConsistencyWarningCode>()

        if (checkRunCount > 0 && !summaryExists) {
            warnings += StatisticsConsistencyWarningCode.CHECK_RUNS_WITHOUT_SUMMARY
        }

        if (summary.totalRuns > checkRunCount && checkRunCount > 0) {
            warnings += StatisticsConsistencyWarningCode.TOTAL_RUNS_EXCEEDS_HISTORY
        }

        val outcomeSum = summary.successRuns +
            summary.partialFailureRuns +
            summary.failureRuns +
            summary.cancelledRuns +
            summary.unknownRuns
        if (summary.totalRuns > 0 && outcomeSum > summary.totalRuns) {
            warnings += StatisticsConsistencyWarningCode.OUTCOME_SUM_EXCEEDS_TOTAL
        }

        if (targetStatisticsCount > 0 && targetResultCount == 0 && checkRunCount > 0) {
            warnings += StatisticsConsistencyWarningCode.TARGET_STATS_WITHOUT_TARGET_RESULTS
        }

        summary.lastRunAt?.let { lastRunAt ->
            if (lastRunAt > nowMillis) {
                warnings += StatisticsConsistencyWarningCode.LAST_RUN_IN_FUTURE
            }
        }

        val lastRunAt = summary.lastRunAt
        if (lastRunAt != null &&
            summary.updatedAt > 0L &&
            summary.updatedAt < lastRunAt &&
            summary.totalRuns > 0
        ) {
            warnings += StatisticsConsistencyWarningCode.STATISTICS_UPDATED_BEFORE_LAST_RUN
        }

        if (StatisticsNumericSanitizer.hasInvalidSuccessRate(summary.successRate)) {
            warnings += StatisticsConsistencyWarningCode.INVALID_SUCCESS_RATE
        }

        if (StatisticsNumericSanitizer.hasInvalidAverageLatency(summary.averageLatencyMs)) {
            warnings += StatisticsConsistencyWarningCode.INVALID_AVERAGE_LATENCY
        }

        if (StatisticsNumericSanitizer.hasNegativeCount(summary.consecutiveFailureCount)) {
            warnings += StatisticsConsistencyWarningCode.NEGATIVE_CONSECUTIVE_FAILURES
        }

        return StatisticsConsistencyReport(warnings = warnings.toList())
    }
}
