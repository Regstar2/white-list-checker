package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.model.WhitelistState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class LoadWhitelistTimelineDashboardUseCaseTest {

    private val zoneId: ZoneId = ZoneId.of("UTC")

    @Test
    fun `empty repository returns Empty`() = runBlocking {
        val useCase = LoadWhitelistTimelineDashboardUseCase(
            repository = FakeWhitelistTimelineRepository(),
            zoneId = zoneId,
        )

        val result = useCase.load(nowMillis = millis(2026, 7, 29, 12, 0))

        assertEquals(WhitelistTimelineLoadResult.Empty, result)
    }

    @Test
    fun `hour bucket is ON when at least one whitelist on sample exists`() = runBlocking {
        val now = millis(2026, 7, 29, 12, 0)
        val repository = FakeWhitelistTimelineRepository(
            samples = listOf(
                sample("off", millis(2026, 7, 29, 10, 10), WhitelistState.WHITELIST_OFF),
                sample("on", millis(2026, 7, 29, 10, 40), WhitelistState.WHITELIST_ON),
            ),
        )
        val useCase = LoadWhitelistTimelineDashboardUseCase(
            repository = repository,
            zoneId = zoneId,
        )

        val result = useCase.load(nowMillis = now)

        assertTrue(result is WhitelistTimelineLoadResult.Success)
        val dashboard = (result as WhitelistTimelineLoadResult.Success).dashboard
        val tenHour = dashboard.dayHourly.single { it.label == "10" }
        assertEquals(WhitelistBinaryState.ON, tenHour.state)
        assertEquals(2, tenHour.sampleCount)
        assertEquals(1, tenHour.whitelistOnCount)
        assertEquals(1, tenHour.whitelistOffCount)
        assertEquals(7, dashboard.weekDaily.size)
        assertEquals(30, dashboard.monthDaily.size)
        assertEquals(12, dashboard.yearMonthly.size)
    }

    @Test
    fun `non binary states stay unknown in buckets`() = runBlocking {
        val now = millis(2026, 7, 29, 12, 0)
        val repository = FakeWhitelistTimelineRepository(
            samples = listOf(
                sample("dns", millis(2026, 7, 29, 9, 5), WhitelistState.MOBILE_DNS_FAILURE),
            ),
        )
        val useCase = LoadWhitelistTimelineDashboardUseCase(
            repository = repository,
            zoneId = zoneId,
        )

        val result = useCase.load(nowMillis = now)

        assertTrue(result is WhitelistTimelineLoadResult.Success)
        val dashboard = (result as WhitelistTimelineLoadResult.Success).dashboard
        val nineHour = dashboard.dayHourly.single { it.label == "09" }
        assertEquals(WhitelistBinaryState.UNKNOWN, nineHour.state)
        assertEquals(1, nineHour.sampleCount)
        assertEquals(0, dashboard.binarySamples)
    }

    private fun sample(
        id: String,
        checkedAtMillis: Long,
        state: WhitelistState,
    ): WhitelistTimelineSample {
        return WhitelistTimelineSample(
            checkRunId = id,
            checkedAtMillis = checkedAtMillis,
            whitelistState = state,
            binaryState = state.toBinaryWhitelistState(),
            createdAtMillis = checkedAtMillis,
        )
    }

    private fun millis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long {
        return LocalDateTime.of(year, month, day, hour, minute)
            .atZone(zoneId)
            .toInstant()
            .toEpochMilli()
    }

    private class FakeWhitelistTimelineRepository(
        private val samples: List<WhitelistTimelineSample> = emptyList(),
    ) : WhitelistTimelineRepository {

        override suspend fun saveSample(sample: WhitelistTimelineSample) = Unit

        override suspend fun getSamplesSince(cutoffMillis: Long): List<WhitelistTimelineSample> {
            return samples.filter { it.checkedAtMillis >= cutoffMillis }
        }

        override suspend fun countSamples(): Int = samples.size

        override suspend fun replaceAll(samples: List<WhitelistTimelineSample>) = Unit

        override suspend fun clear() = Unit

        override suspend fun applyRetentionPolicy(nowMillis: Long): Int = 0
    }
}
