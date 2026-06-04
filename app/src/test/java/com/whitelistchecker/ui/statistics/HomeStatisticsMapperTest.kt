package com.whitelistchecker.ui.statistics

import com.whitelistchecker.domain.availability.WhitelistAvailabilityDashboard
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilitySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStatisticsMapperTest {

    @Test
    fun `maps whitelist summary without check metrics`() {
        val dashboard = WhitelistAvailabilityDashboard(
            summary = WhitelistAvailabilitySummary(
                currentlyAvailableTargets = 6,
                availabilityPercent = 0.857,
                totalBecameAvailableEvents = 6,
                totalBecameUnavailableEvents = 1,
                totalTargets = 8,
            ),
            daily = emptyList(),
            targetStates = emptyList(),
            recentEvents = emptyList(),
            topAvailableTargets = emptyList(),
            topStableTargets = emptyList(),
            topUnstableTargets = emptyList(),
            lastUpdatedAt = 1_000L,
            isStale = false,
        )
        val state = HomeStatisticsMapper.map(dashboard)
        assertTrue(state is HomeStatisticsUiState.Content)
        val content = state as HomeStatisticsUiState.Content
        assertEquals(6, content.availableTargets)
        assertEquals(7, content.periodChanges)
        assertEquals(0.857, content.availabilityPercent!!, 0.001)
    }

    @Test
    fun `returns hidden when no whitelist data`() {
        assertTrue(HomeStatisticsMapper.map(null) is HomeStatisticsUiState.Hidden)
    }
}
