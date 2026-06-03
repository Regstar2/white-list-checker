package com.whitelistchecker.domain.history

import com.whitelistchecker.domain.model.CheckTarget
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.SiteCheckErrorType
import com.whitelistchecker.domain.model.SiteCheckResult
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.history.CheckRunOverallStatus
import com.whitelistchecker.domain.model.history.CheckTargetResultStatus
import com.whitelistchecker.domain.model.history.CheckTriggerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckHistoryFromNetworkResultMapperTest {

    private val mapper = CheckHistoryFromNetworkResultMapper()

    @Test
    fun `successful check maps to SUCCESS run and target results`() {
        val (run, targets) = mapper.toCheckRun(
            result = sampleResult(
                siteResults = listOf(
                    site("https://google.com", available = true),
                    site("https://ya.ru", available = true),
                ),
            ),
            triggerType = CheckTriggerType.MANUAL,
            startedAtMillis = 1_000L,
            finishedAtMillis = 2_500L,
            appVersion = "0.8.0",
        )

        assertEquals(CheckRunOverallStatus.SUCCESS, run.overallStatus)
        assertEquals(2, run.successCount)
        assertEquals(0, run.failureCount)
        assertEquals(1_500L, run.durationMs)
        assertEquals(2, targets.size)
        assertEquals(CheckTargetResultStatus.SUCCESS, targets.first().status)
        assertNull(run.checkError)
    }

    @Test
    fun `failed check without sites maps to FAILURE run`() {
        val (run, targets) = mapper.toCheckRun(
            result = sampleResult(
                siteResults = emptyList(),
                state = WhitelistState.CELLULAR_NETWORK_UNAVAILABLE,
                error = "no cellular",
            ),
            triggerType = CheckTriggerType.BACKGROUND,
            startedAtMillis = 10L,
            finishedAtMillis = 20L,
            appVersion = "0.8.0",
        )

        assertEquals(CheckRunOverallStatus.FAILURE, run.overallStatus)
        assertEquals(CheckTriggerType.BACKGROUND, run.triggerType)
        assertEquals("no cellular", run.checkError)
        assertTrue(targets.isEmpty())
    }

    @Test
    fun `mixed site results map to PARTIAL_FAILURE`() {
        val (run, _) = mapper.toCheckRun(
            result = sampleResult(
                siteResults = listOf(
                    site("https://google.com", available = true),
                    site("https://ya.ru", available = false, errorType = SiteCheckErrorType.DNS),
                ),
            ),
            triggerType = CheckTriggerType.MANUAL,
            startedAtMillis = 0L,
            finishedAtMillis = 100L,
            appVersion = "0.8.0",
        )

        assertEquals(CheckRunOverallStatus.PARTIAL_FAILURE, run.overallStatus)
        assertEquals(1, run.successCount)
        assertEquals(1, run.failureCount)
    }

    @Test
    fun `dns error maps to DNS_ERROR target status`() {
        val (_, targets) = mapper.toCheckRun(
            result = sampleResult(
                siteResults = listOf(
                    site("https://google.com", available = false, errorType = SiteCheckErrorType.DNS),
                ),
            ),
            triggerType = CheckTriggerType.MANUAL,
            startedAtMillis = 0L,
            finishedAtMillis = 50L,
            appVersion = "0.8.0",
        )

        assertEquals(CheckTargetResultStatus.DNS_ERROR, targets.single().status)
        assertEquals(SiteCheckErrorType.DNS.name, targets.single().errorCategory)
    }

    private fun sampleResult(
        siteResults: List<SiteCheckResult>,
        state: WhitelistState = WhitelistState.WHITELIST_OFF,
        error: String? = null,
    ): NetworkCheckResult {
        val foreign = TargetGroupSummary(TargetGroup.FOREIGN, 1, 1)
        val local = TargetGroupSummary(TargetGroup.LOCAL, 1, 1)
        return NetworkCheckResult(
            siteResults = siteResults,
            foreignSummary = foreign,
            localSummary = local,
            state = state,
            activeNetworkLabel = "Wi-Fi",
            checkedNetworkLabel = "Mobile",
            checkedAtMillis = 100L,
            error = error,
        )
    }

    private fun site(
        url: String,
        available: Boolean,
        errorType: SiteCheckErrorType = SiteCheckErrorType.NONE,
    ): SiteCheckResult {
        return SiteCheckResult(
            target = CheckTarget(name = url, url = url, group = TargetGroup.FOREIGN),
            available = available,
            httpCode = if (available) 204 else null,
            error = if (available) null else "error",
            errorType = errorType,
            durationMs = 120L,
        )
    }
}
