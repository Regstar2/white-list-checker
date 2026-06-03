package com.whitelistchecker.domain.check

import com.whitelistchecker.domain.model.LastCheckDisplayState
import com.whitelistchecker.domain.model.LastCheckFreshness
import com.whitelistchecker.domain.model.LastCheckOutcome
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LastCheckStateResolverTest {

    private val resolver = LastCheckStateResolver()
    private val staleThresholdMs = LastCheckConfig.DEFAULT_LAST_CHECK_STALE_THRESHOLD_MS
    private val nowMillis = 10_000_000L

    @Test
    fun `null last check returns NoCheck`() {
        val state = resolver.resolve(
            isChecking = false,
            loadFailed = false,
            lastCheck = null,
            nowMillis = nowMillis,
            staleThresholdMs = staleThresholdMs,
        )
        assertEquals(LastCheckDisplayState.NoCheck, state)
    }

    @Test
    fun `checking returns Running`() {
        val state = resolver.resolve(
            isChecking = true,
            loadFailed = false,
            lastCheck = sampleCheck(checkedAtMillis = nowMillis),
            nowMillis = nowMillis,
            staleThresholdMs = staleThresholdMs,
        )
        assertEquals(LastCheckDisplayState.Running, state)
    }

    @Test
    fun `load failed returns LoadError`() {
        val state = resolver.resolve(
            isChecking = false,
            loadFailed = true,
            lastCheck = sampleCheck(checkedAtMillis = nowMillis),
            nowMillis = nowMillis,
            staleThresholdMs = staleThresholdMs,
        )
        assertEquals(LastCheckDisplayState.LoadError, state)
    }

    @Test
    fun `fresh successful check`() {
        val state = resolver.resolve(
            isChecking = false,
            loadFailed = false,
            lastCheck = sampleCheck(checkedAtMillis = nowMillis - 60_000L),
            nowMillis = nowMillis,
            staleThresholdMs = staleThresholdMs,
        )
        assertTrue(state is LastCheckDisplayState.Available)
        val available = state as LastCheckDisplayState.Available
        assertEquals(LastCheckFreshness.FRESH, available.freshness)
        assertEquals(LastCheckOutcome.SUCCESS, available.outcome)
    }

    @Test
    fun `stale successful check`() {
        val state = resolver.resolve(
            isChecking = false,
            loadFailed = false,
            lastCheck = sampleCheck(checkedAtMillis = nowMillis - staleThresholdMs - 1),
            nowMillis = nowMillis,
            staleThresholdMs = staleThresholdMs,
        )
        assertTrue(state is LastCheckDisplayState.Available)
        val available = state as LastCheckDisplayState.Available
        assertEquals(LastCheckFreshness.STALE, available.freshness)
        assertEquals(LastCheckOutcome.SUCCESS, available.outcome)
    }

    @Test
    fun `fresh failed check`() {
        val state = resolver.resolve(
            isChecking = false,
            loadFailed = false,
            lastCheck = sampleCheck(
                checkedAtMillis = nowMillis - 60_000L,
                error = "SecurityException",
            ),
            nowMillis = nowMillis,
            staleThresholdMs = staleThresholdMs,
        )
        assertTrue(state is LastCheckDisplayState.Available)
        val available = state as LastCheckDisplayState.Available
        assertEquals(LastCheckFreshness.FRESH, available.freshness)
        assertEquals(LastCheckOutcome.FAILURE, available.outcome)
    }

    @Test
    fun `stale failed check`() {
        val state = resolver.resolve(
            isChecking = false,
            loadFailed = false,
            lastCheck = sampleCheck(
                checkedAtMillis = nowMillis - staleThresholdMs - 1,
                error = "timeout",
            ),
            nowMillis = nowMillis,
            staleThresholdMs = staleThresholdMs,
        )
        assertTrue(state is LastCheckDisplayState.Available)
        val available = state as LastCheckDisplayState.Available
        assertEquals(LastCheckFreshness.STALE, available.freshness)
        assertEquals(LastCheckOutcome.FAILURE, available.outcome)
    }

    private fun sampleCheck(
        checkedAtMillis: Long,
        error: String? = null,
    ): NetworkCheckResult {
        val summary = TargetGroupSummary(
            group = TargetGroup.FOREIGN,
            availableCount = 1,
            totalCount = 2,
        )
        return NetworkCheckResult(
            siteResults = emptyList(),
            foreignSummary = summary,
            localSummary = summary.copy(group = TargetGroup.LOCAL),
            state = WhitelistState.WHITELIST_OFF,
            activeNetworkLabel = "Wi-Fi",
            checkedNetworkLabel = "Mobile",
            checkedAtMillis = checkedAtMillis,
            error = error,
        )
    }
}
