package com.whitelistchecker.ui.home

import com.whitelistchecker.R
import com.whitelistchecker.domain.model.LastCheckDisplayState
import com.whitelistchecker.domain.model.LastCheckFreshness
import com.whitelistchecker.domain.model.LastCheckOutcome
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLastCheckPresentationMapperTest {

    @Test
    fun `no check maps to neutral empty state`() {
        val model = HomeLastCheckPresentationMapper.map(LastCheckDisplayState.NoCheck)

        assertEquals(R.string.home_result_no_check_title, model.headlineRes)
        assertEquals(HomeResultTone.NEUTRAL, model.tone)
        assertFalse(model.showDetails)
        assertEquals(null, model.localCount)
    }

    @Test
    fun `running maps to neutral loading state`() {
        val model = HomeLastCheckPresentationMapper.map(LastCheckDisplayState.Running)

        assertEquals(R.string.home_result_running_title, model.headlineRes)
        assertEquals(HomeResultTone.NEUTRAL, model.tone)
        assertFalse(model.showDetails)
    }

    @Test
    fun `whitelist on maps to warning with local first counters preserved`() {
        val model = HomeLastCheckPresentationMapper.map(
            LastCheckDisplayState.Available(
                result = sampleResult(
                    state = WhitelistState.WHITELIST_ON,
                    foreign = TargetGroupSummary(TargetGroup.FOREIGN, availableCount = 0, totalCount = 8),
                    local = TargetGroupSummary(TargetGroup.LOCAL, availableCount = 7, totalCount = 8),
                ),
                freshness = LastCheckFreshness.FRESH,
                outcome = LastCheckOutcome.SUCCESS,
            ),
        )

        assertEquals(R.string.home_result_state_whitelist_on, model.headlineRes)
        assertEquals(HomeResultTone.WARNING, model.tone)
        assertEquals(7, model.localCount?.availableCount)
        assertEquals(8, model.localCount?.totalCount)
        assertEquals(0, model.foreignCount?.availableCount)
        assertEquals(8, model.foreignCount?.totalCount)
        assertTrue(model.showDetails)
    }

    @Test
    fun `mobile checked network with wifi active maps to readable route without raw Mobile`() {
        val model = HomeLastCheckPresentationMapper.map(
            LastCheckDisplayState.Available(
                result = sampleResult(
                    checkedNetworkLabel = "Mobile",
                    activeNetworkLabel = "Wi-Fi",
                ),
                freshness = LastCheckFreshness.FRESH,
                outcome = LastCheckOutcome.SUCCESS,
            ),
        )

        assertEquals(R.string.home_result_route_mobile_wifi, model.route?.textRes)
        assertEquals(null, model.route?.checkedNetwork)
        assertEquals(null, model.route?.activeNetwork)
    }

    @Test
    fun `failed available result uses error tone and keeps error detail`() {
        val model = HomeLastCheckPresentationMapper.map(
            LastCheckDisplayState.Available(
                result = sampleResult(error = "SecurityException"),
                freshness = LastCheckFreshness.STALE,
                outcome = LastCheckOutcome.FAILURE,
            ),
        )

        assertEquals(HomeResultTone.ERROR, model.tone)
        assertEquals("SecurityException", model.error)
        assertTrue(model.stale)
        assertTrue(model.showDetails)
    }

    private fun sampleResult(
        state: WhitelistState = WhitelistState.WHITELIST_OFF,
        foreign: TargetGroupSummary = TargetGroupSummary(TargetGroup.FOREIGN, availableCount = 8, totalCount = 8),
        local: TargetGroupSummary = TargetGroupSummary(TargetGroup.LOCAL, availableCount = 8, totalCount = 8),
        checkedNetworkLabel: String = "Mobile",
        activeNetworkLabel: String = "Wi-Fi",
        error: String? = null,
    ): NetworkCheckResult {
        return NetworkCheckResult(
            siteResults = emptyList(),
            foreignSummary = foreign,
            localSummary = local,
            state = state,
            activeNetworkLabel = activeNetworkLabel,
            checkedNetworkLabel = checkedNetworkLabel,
            checkedAtMillis = 1_000L,
            error = error,
        )
    }
}
