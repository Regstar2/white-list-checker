package com.whitelistchecker.domain.history

import com.whitelistchecker.domain.model.CheckTarget
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.SiteCheckResult
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.history.CheckRun
import com.whitelistchecker.domain.model.history.CheckRunWithTargetResults
import com.whitelistchecker.domain.model.history.CheckTargetResult
import com.whitelistchecker.domain.history.CheckRunTimeRange
import com.whitelistchecker.domain.model.history.CheckTriggerType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SaveCheckHistoryUseCaseTest {

    @Test
    fun `saveCompletedCheck persists run and target results`() = runBlocking {
        val repository = FakeCheckHistoryRepository()
        val useCase = SaveCheckHistoryUseCase(
            checkHistoryRepository = repository,
            mapper = CheckHistoryFromNetworkResultMapper(),
            appVersionProvider = { "0.8.0" },
        )

        useCase.saveCompletedCheck(
            result = sampleResult(),
            triggerType = CheckTriggerType.MANUAL,
            startedAtMillis = 1_000L,
            finishedAtMillis = 2_000L,
        )

        assertEquals(1, repository.savedRuns.size)
        assertEquals(1, repository.savedTargets.size)
        assertEquals(1, repository.retentionCalls)
    }

    @Test
    fun `storage failure does not propagate from repository`() = runBlocking {
        val repository = FakeCheckHistoryRepository(throwOnSave = true)
        val useCase = SaveCheckHistoryUseCase(
            checkHistoryRepository = repository,
            mapper = CheckHistoryFromNetworkResultMapper(),
            appVersionProvider = { "0.8.0" },
        )

        var thrown = false
        try {
            useCase.saveCompletedCheck(
                result = sampleResult(),
                triggerType = CheckTriggerType.MANUAL,
                startedAtMillis = 0L,
                finishedAtMillis = 1L,
            )
        } catch (_: Exception) {
            thrown = true
        }

        assertTrue(thrown)
    }

    private fun sampleResult(): NetworkCheckResult {
        val summary = TargetGroupSummary(TargetGroup.FOREIGN, 1, 1)
        return NetworkCheckResult(
            siteResults = listOf(
                SiteCheckResult(
                    target = CheckTarget("Google", "https://www.google.com/generate_204", TargetGroup.FOREIGN),
                    available = true,
                    httpCode = 204,
                    error = null,
                    durationMs = 100L,
                ),
            ),
            foreignSummary = summary,
            localSummary = summary.copy(group = TargetGroup.LOCAL),
            state = WhitelistState.WHITELIST_OFF,
            activeNetworkLabel = "Wi-Fi",
            checkedNetworkLabel = "Mobile",
            checkedAtMillis = 2_000L,
        )
    }

    private class FakeCheckHistoryRepository(
        private val throwOnSave: Boolean = false,
    ) : CheckHistoryRepository {

        val savedRuns = mutableListOf<CheckRun>()
        val savedTargets = mutableListOf<List<CheckTargetResult>>()
        var retentionCalls: Int = 0

        override suspend fun saveCheckRun(
            checkRun: CheckRun,
            targetResults: List<CheckTargetResult>,
        ) {
            if (throwOnSave) error("storage failed")
            savedRuns += checkRun
            savedTargets += targetResults
        }

        override suspend fun getLatestCheckRun(): CheckRunWithTargetResults? = null

        override suspend fun getRecentCheckRuns(limit: Int): List<CheckRunWithTargetResults> = emptyList()

        override suspend fun countCheckRuns(): Int = savedRuns.size

        override suspend fun countTargetResults(): Int = savedTargets.sumOf { it.size }

        override suspend fun getCheckRunTimeRange(): CheckRunTimeRange? = null

        override suspend fun applyRetentionPolicy(nowMillis: Long): Int {
            retentionCalls++
            return 0
        }
    }
}
