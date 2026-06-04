package com.whitelistchecker.ui.statistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsValueFormatterTest {

    @Test
    fun `formatSuccessRate rounds whole percent`() {
        assertEquals("85%", StatisticsValueFormatter.formatSuccessRate(0.85))
    }

    @Test
    fun `formatSuccessRate shows one decimal when needed`() {
        assertEquals("12.5%", StatisticsValueFormatter.formatSuccessRate(0.125))
    }

    @Test
    fun `formatSuccessRate hides invalid rate`() {
        assertEquals("", StatisticsValueFormatter.formatSuccessRate(-0.2))
    }

    @Test
    fun `formatSuccessRate returns empty for null`() {
        assertEquals("", StatisticsValueFormatter.formatSuccessRate(null))
    }

    @Test
    fun `sanitizeHost strips scheme and www`() {
        assertEquals("google.com", StatisticsValueFormatter.sanitizeHost("https://www.google.com/path?q=1"))
    }

    @Test
    fun `hasMeaningfulCountChart rejects uniform low scores`() {
        assertFalse(hasMeaningfulCountChart(listOf(1, 1, 1)))
        assertTrue(hasMeaningfulCountChart(listOf(1, 3, 2)))
    }
}
