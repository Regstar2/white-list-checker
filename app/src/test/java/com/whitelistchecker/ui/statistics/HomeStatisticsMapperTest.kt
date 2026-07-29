package com.whitelistchecker.ui.statistics

import com.whitelistchecker.domain.statistics.WhitelistBinaryState
import com.whitelistchecker.domain.statistics.WhitelistTimelineDashboard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStatisticsMapperTest {

    @Test
    fun `maps whitelist timeline summary`() {
        val dashboard = WhitelistTimelineDashboard(
            currentState = WhitelistBinaryState.ON,
            currentStateAtMillis = 900L,
            totalSamples = 10,
            binarySamples = 7,
            whitelistOnSamples = 3,
            whitelistOffSamples = 4,
            whitelistOnPercent = 3.0 / 7.0,
            todayHourly = emptyList(),
            last14Days = emptyList(),
            last12Weeks = emptyList(),
            last12Months = emptyList(),
            lastUpdatedAt = 1_000L,
            isStale = false,
        )
        val state = HomeStatisticsMapper.map(dashboard)
        assertTrue(state is HomeStatisticsUiState.Content)
        val content = state as HomeStatisticsUiState.Content
        assertEquals(WhitelistBinaryState.ON, content.currentState)
        assertEquals(7, content.binarySamples)
        assertEquals(3.0 / 7.0, content.whitelistOnPercent!!, 0.001)
    }

    @Test
    fun `returns hidden when no whitelist data`() {
        assertTrue(HomeStatisticsMapper.map(null) is HomeStatisticsUiState.Hidden)
    }
}
