package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.check.LastCheckConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsStaleResolverTest {

    private val resolver = StatisticsStaleResolver()
    private val threshold = LastCheckConfig.DEFAULT_LAST_CHECK_STALE_THRESHOLD_MS
    private val nowMillis = 1_000_000_000L

    @Test
    fun `null last run is not stale`() {
        assertFalse(resolver.isStale(null, nowMillis, threshold))
    }

    @Test
    fun `recent last run is not stale`() {
        val lastRun = nowMillis - threshold
        assertFalse(resolver.isStale(lastRun, nowMillis, threshold))
    }

    @Test
    fun `old last run is stale`() {
        val lastRun = nowMillis - threshold - 1
        assertTrue(resolver.isStale(lastRun, nowMillis, threshold))
    }
}
