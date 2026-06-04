package com.whitelistchecker.ui.statistics

import com.whitelistchecker.domain.model.LastCheckDisplayState
import com.whitelistchecker.domain.model.LastCheckOutcome
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.statistics.StatisticsDashboard

object StatisticsFreshnessMapper {

    fun map(
        checkDashboard: StatisticsDashboard,
        lastCheck: NetworkCheckResult?,
        lastCheckDisplay: LastCheckDisplayState,
        whitelistAvailableTargets: Int,
        whitelistTotalTargets: Int,
        whitelistLowSample: Boolean,
    ): StatisticsFreshnessUi {
        val (available, total) = targetsCheckedCounts(
            lastCheck = lastCheck,
            whitelistAvailableTargets = whitelistAvailableTargets,
            whitelistTotalTargets = whitelistTotalTargets,
        )
        return StatisticsFreshnessUi(
            dataUpdatedAt = checkDashboard.lastUpdatedAt,
            isStale = checkDashboard.isStale,
            isLowSample = checkDashboard.summary.totalRuns < 3 || whitelistLowSample,
            lastCheckAt = lastCheck?.checkedAtMillis ?: checkDashboard.summary.lastRunAt,
            lastCheckStatus = mapLastCheckStatus(lastCheckDisplay, checkDashboard),
            targetsCheckedAvailable = available,
            targetsCheckedTotal = total,
        )
    }

    private fun mapLastCheckStatus(
        display: LastCheckDisplayState,
        dashboard: StatisticsDashboard,
    ): LastCheckTechnicalStatus {
        if (display !is LastCheckDisplayState.Available) {
            return if (dashboard.summary.lastRunAt != null) {
                LastCheckTechnicalStatus.COMPLETED
            } else {
                LastCheckTechnicalStatus.NONE
            }
        }
        return when (display.outcome) {
            LastCheckOutcome.SUCCESS -> LastCheckTechnicalStatus.COMPLETED
            LastCheckOutcome.FAILURE -> LastCheckTechnicalStatus.FAILED
        }
    }

    private fun targetsCheckedCounts(
        lastCheck: NetworkCheckResult?,
        whitelistAvailableTargets: Int,
        whitelistTotalTargets: Int,
    ): Pair<Int, Int> {
        if (lastCheck != null) {
            val available = lastCheck.foreignSummary.availableCount + lastCheck.localSummary.availableCount
            val total = lastCheck.foreignSummary.totalCount + lastCheck.localSummary.totalCount
            if (total > 0) return available to total
        }
        return whitelistAvailableTargets to whitelistTotalTargets.coerceAtLeast(0)
    }
}
