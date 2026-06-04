package com.whitelistchecker.ui.statistics

import com.whitelistchecker.domain.model.LastCheckDisplayState
import com.whitelistchecker.domain.model.LastCheckFreshness
import com.whitelistchecker.domain.model.LastCheckOutcome
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.statistics.CheckStatisticsSummary
import com.whitelistchecker.domain.statistics.StatisticsDashboard
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsFreshnessMapperTest {

    @Test
    fun `uses last check target counts for header`() {
        val freshness = StatisticsFreshnessMapper.map(
            checkDashboard = dashboard(totalRuns = 10),
            lastCheck = sampleLastCheck(available = 5, total = 8),
            lastCheckDisplay = LastCheckDisplayState.Available(
                result = sampleLastCheck(5, 8),
                freshness = LastCheckFreshness.FRESH,
                outcome = LastCheckOutcome.SUCCESS,
            ),
            whitelistAvailableTargets = 6,
            whitelistTotalTargets = 8,
            whitelistLowSample = false,
        )
        assertEquals(5, freshness.targetsCheckedAvailable)
        assertEquals(8, freshness.targetsCheckedTotal)
        assertEquals(LastCheckTechnicalStatus.COMPLETED, freshness.lastCheckStatus)
    }

    private fun dashboard(totalRuns: Int) = StatisticsDashboard(
        summary = CheckStatisticsSummary(totalRuns = totalRuns),
        targets = emptyList(),
        routeKinds = emptyList(),
        networks = emptyList(),
        daily = emptyList(),
        isStale = false,
        lastUpdatedAt = 2_000L,
    )

    private fun sampleLastCheck(available: Int, total: Int) = NetworkCheckResult(
        siteResults = emptyList(),
        foreignSummary = TargetGroupSummary(TargetGroup.FOREIGN, available, total),
        localSummary = TargetGroupSummary(TargetGroup.LOCAL, 0, 0),
        state = WhitelistState.WHITELIST_OFF,
        activeNetworkLabel = "Wi-Fi",
        checkedNetworkLabel = "Mobile",
        checkedAtMillis = 1_000L,
    )
}
