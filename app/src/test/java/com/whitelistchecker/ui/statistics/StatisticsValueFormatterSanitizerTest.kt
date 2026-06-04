package com.whitelistchecker.ui.statistics

import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsValueFormatterSanitizerTest {

    @Test
    fun `formatSuccessRate hides nan`() {
        assertEquals("", StatisticsValueFormatter.formatSuccessRate(Double.NaN))
    }

    @Test
    fun `formatSuccessRate hides infinity`() {
        assertEquals("", StatisticsValueFormatter.formatSuccessRate(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `formatSuccessRate clamps above one hundred percent display`() {
        assertEquals("100%", StatisticsValueFormatter.formatSuccessRate(1.5))
    }
}
