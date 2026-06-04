package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.history.CheckHistoryConfig
import com.whitelistchecker.domain.history.CheckHistoryRepository
import com.whitelistchecker.domain.history.CheckRunTimeRange
import com.whitelistchecker.domain.model.history.CheckRun
import com.whitelistchecker.domain.model.history.CheckRunOverallStatus
import com.whitelistchecker.domain.model.history.CheckRunWithTargetResults
import com.whitelistchecker.domain.model.history.CheckTargetResult
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.history.CheckTargetResultStatus
import com.whitelistchecker.domain.model.history.CheckTriggerType
import com.whitelistchecker.domain.model.statistics.CheckStatisticsSnapshot
import com.whitelistchecker.domain.model.statistics.CheckStatisticsSummary
import com.whitelistchecker.domain.model.statistics.DailyCheckStatistics
import com.whitelistchecker.domain.model.statistics.NetworkStatistics
import com.whitelistchecker.domain.model.statistics.RouteKindStatistics
import com.whitelistchecker.domain.model.statistics.TargetStatistics
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RebuildCheckStatisticsUseCaseTest {

    @Test
    fun `rebuild restores summary from history`() = runBlocking {
        val historyRepository = FakeHistoryRepository(
            runs = listOf(
                CheckRunWithTargetResults(
                    run = sampleRun(),
                    targetResults = listOf(sampleTarget()),
                ),
            ),
        )
        val statisticsRepository = FakeStatisticsRepository()
        val useCase = RebuildCheckStatisticsUseCase(
            checkHistoryRepository = historyRepository,
            checkStatisticsRepository = statisticsRepository,
            calculator = CheckStatisticsCalculator(),
        )

        val result = useCase.rebuildFromHistory()

        assertTrue(result is RebuildStatisticsResult.Success)
        assertEquals(1, statisticsRepository.lastSnapshot?.summary?.totalRuns)
        assertEquals(1, statisticsRepository.lastSnapshot?.targets?.size)
    }

    @Test
    fun `rebuild failure does not throw`() = runBlocking {
        val useCase = RebuildCheckStatisticsUseCase(
            checkHistoryRepository = FakeHistoryRepository(),
            checkStatisticsRepository = FakeStatisticsRepository(throwOnReplace = true),
            calculator = CheckStatisticsCalculator(),
        )

        val result = useCase.rebuildFromHistory()

        assertTrue(result is RebuildStatisticsResult.Failure)
    }

    private fun sampleRun(): CheckRun {
        return CheckRun(
            id = "run-1",
            startedAtMillis = 900L,
            finishedAtMillis = 1_000L,
            durationMs = 100L,
            triggerType = CheckTriggerType.MANUAL,
            networkType = "CELLULAR",
            operatorName = null,
            routeMode = CheckHistoryConfig.ROUTE_MODE_CELLULAR,
            overallStatus = CheckRunOverallStatus.SUCCESS,
            whitelistState = WhitelistState.WHITELIST_OFF,
            successCount = 1,
            failureCount = 0,
            skippedCount = 0,
            appVersion = "0.8.3",
            schemaVersion = 1,
            createdAtMillis = 1_000L,
            checkError = null,
            diagnosticsMessage = null,
        )
    }

    private fun sampleTarget(): CheckTargetResult {
        return CheckTargetResult(
            id = "target-1",
            checkRunId = "run-1",
            targetId = "https://google.com",
            targetLabel = "Google",
            targetHost = "google.com",
            routeKind = "FOREIGN",
            status = CheckTargetResultStatus.SUCCESS,
            latencyMs = 100L,
            httpStatusCode = 204,
            errorCode = null,
            errorCategory = null,
            startedAtMillis = 900L,
            finishedAtMillis = 1_000L,
            createdAtMillis = 1_000L,
        )
    }

    private class FakeHistoryRepository(
        private val runs: List<CheckRunWithTargetResults> = emptyList(),
    ) : CheckHistoryRepository {

        override suspend fun saveCheckRun(
            checkRun: CheckRun,
            targetResults: List<CheckTargetResult>,
        ) = Unit

        override suspend fun getLatestCheckRun(): CheckRunWithTargetResults? = runs.lastOrNull()

        override suspend fun getRecentCheckRuns(limit: Int): List<CheckRunWithTargetResults> {
            return runs.takeLast(limit)
        }

        override suspend fun countCheckRuns(): Int = runs.size

        override suspend fun countTargetResults(): Int = runs.sumOf { it.targetResults.size }

        override suspend fun getCheckRunTimeRange(): CheckRunTimeRange? = null

        override suspend fun applyRetentionPolicy(nowMillis: Long): Int = 0
    }

    private class FakeStatisticsRepository(
        private val throwOnReplace: Boolean = false,
    ) : CheckStatisticsRepository {

        var lastSnapshot: CheckStatisticsSnapshot? = null

        override suspend fun updateFromCheckRun(
            checkRun: CheckRun,
            targetResults: List<CheckTargetResult>,
        ) = Unit

        override suspend fun getSummary(): CheckStatisticsSummary = CheckStatisticsSummary()

        override suspend fun summaryExists(): Boolean = lastSnapshot != null

        override suspend fun countTargetStatistics(): Int = lastSnapshot?.targets?.size ?: 0

        override suspend fun countRouteKindStatistics(): Int = lastSnapshot?.routeKinds?.size ?: 0

        override suspend fun countNetworkStatistics(): Int = lastSnapshot?.networks?.size ?: 0

        override suspend fun countDailyStatistics(): Int = lastSnapshot?.daily?.size ?: 0

        override suspend fun getTargetStatistics(limit: Int): List<TargetStatistics> = emptyList()

        override suspend fun getRouteKindStatistics(): List<RouteKindStatistics> = emptyList()

        override suspend fun getNetworkStatistics(): List<NetworkStatistics> = emptyList()

        override suspend fun getDailyStatistics(limit: Int): List<DailyCheckStatistics> = emptyList()

        override suspend fun replaceAll(snapshot: CheckStatisticsSnapshot) {
            if (throwOnReplace) error("replace failed")
            lastSnapshot = snapshot
        }

        override suspend fun clearStatistics() = Unit

        override suspend fun applyDailyRetention(nowMillis: Long) = Unit
    }
}
