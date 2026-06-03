package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.model.history.CheckRun
import com.whitelistchecker.domain.model.history.CheckTargetResult
import com.whitelistchecker.domain.model.statistics.CheckStatisticsSnapshot
import com.whitelistchecker.domain.model.statistics.CheckStatisticsSummary
import com.whitelistchecker.domain.model.statistics.DailyCheckStatistics
import com.whitelistchecker.domain.model.statistics.NetworkStatistics
import com.whitelistchecker.domain.model.statistics.RouteKindStatistics
import com.whitelistchecker.domain.model.statistics.TargetStatistics
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadStatisticsDashboardUseCaseTest {

    @Test
    fun `empty summary returns Empty`() = runBlocking {
        val useCase = LoadStatisticsDashboardUseCase(checkStatisticsRepository = FakeCheckStatisticsRepository())
        val result = useCase.load(nowMillis = 1_000L)
        assertEquals(StatisticsLoadResult.Empty, result)
    }

    @Test
    fun `summary with runs returns Success dashboard`() = runBlocking {
        val repository = FakeCheckStatisticsRepository(
            summary = CheckStatisticsSummary(
                totalRuns = 5,
                successRuns = 4,
                successRate = 0.8,
                lastRunAt = 900L,
                lastSuccessAt = 900L,
            ),
            targets = listOf(
                TargetStatistics(
                    targetId = "google",
                    targetLabel = "Google",
                    targetHost = "google.com",
                    totalChecks = 5,
                    successChecks = 5,
                    failureChecks = 0,
                    successRate = 1.0,
                ),
            ),
        )
        val useCase = LoadStatisticsDashboardUseCase(checkStatisticsRepository = repository)
        val result = useCase.load(nowMillis = 2_000L)
        assertTrue(result is StatisticsLoadResult.Success)
        val dashboard = (result as StatisticsLoadResult.Success).dashboard
        assertEquals(5, dashboard.summary.totalRuns)
        assertEquals(1, dashboard.targets.size)
        assertFalse(dashboard.isStale)
    }

    @Test
    fun `repository failure returns Failure`() = runBlocking {
        val useCase = LoadStatisticsDashboardUseCase(
            checkStatisticsRepository = FakeCheckStatisticsRepository(throwOnSummary = true),
        )
        val result = useCase.load()
        assertTrue(result is StatisticsLoadResult.Failure)
    }

    private class FakeCheckStatisticsRepository(
        private val summary: CheckStatisticsSummary = CheckStatisticsSummary(),
        private val targets: List<TargetStatistics> = emptyList(),
        private val throwOnSummary: Boolean = false,
    ) : CheckStatisticsRepository {

        override suspend fun updateFromCheckRun(
            checkRun: CheckRun,
            targetResults: List<CheckTargetResult>,
        ) = Unit

        override suspend fun getSummary(): CheckStatisticsSummary {
            if (throwOnSummary) error("db error")
            return summary
        }

        override suspend fun getTargetStatistics(limit: Int): List<TargetStatistics> = targets.take(limit)

        override suspend fun getRouteKindStatistics(): List<RouteKindStatistics> = emptyList()

        override suspend fun getNetworkStatistics(): List<NetworkStatistics> = emptyList()

        override suspend fun getDailyStatistics(limit: Int): List<DailyCheckStatistics> = emptyList()

        override suspend fun replaceAll(snapshot: CheckStatisticsSnapshot) = Unit

        override suspend fun clearStatistics() = Unit

        override suspend fun applyDailyRetention(nowMillis: Long) = Unit
    }
}
