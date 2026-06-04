package com.whitelistchecker.domain.statistics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsNumericSanitizerTest {

    @Test
    fun `sanitizeSuccessRate rejects nan and infinity`() {
        assertNull(StatisticsNumericSanitizer.sanitizeSuccessRate(Double.NaN))
        assertNull(StatisticsNumericSanitizer.sanitizeSuccessRate(Double.POSITIVE_INFINITY))
    }

    @Test
    fun `sanitizeSuccessRate clamps above one`() {
        assertEquals(1.0, StatisticsNumericSanitizer.sanitizeSuccessRate(1.5)!!, 0.001)
    }

    @Test
    fun `sanitizeLatency rejects negative values`() {
        assertNull(StatisticsNumericSanitizer.sanitizeLatencyMs(-1L))
    }

    @Test
    fun `hasInvalidSuccessRate detects bad values`() {
        assertTrue(StatisticsNumericSanitizer.hasInvalidSuccessRate(Double.NaN))
    }
}
