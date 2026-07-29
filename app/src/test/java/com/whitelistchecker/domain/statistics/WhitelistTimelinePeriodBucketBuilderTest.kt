package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.model.WhitelistState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class WhitelistTimelinePeriodBucketBuilderTest {

    private val zoneId = ZoneId.of("UTC")
    private val builder = WhitelistTimelinePeriodBucketBuilder(zoneId)

    @Test
    fun `week period is selected calendar week from monday to sunday`() {
        val samples = listOf(
            sample("on", millis(2026, 7, 27, 10, 0), WhitelistState.WHITELIST_ON),
            sample("off", millis(2026, 8, 3, 10, 0), WhitelistState.WHITELIST_OFF),
        )

        val period = builder.buildWeek(samples, LocalDate.of(2026, 7, 29))

        assertEquals("27.07-02.08.2026", period.title)
        assertEquals(7, period.buckets.size)
        assertEquals(WhitelistBinaryState.ON, period.buckets.first().state)
        assertEquals(WhitelistBinaryState.UNKNOWN, period.buckets.last().state)
    }

    @Test
    fun `month period uses selected full month`() {
        val samples = listOf(
            sample("off", millis(2026, 2, 28, 12, 0), WhitelistState.WHITELIST_OFF),
            sample("on", millis(2026, 3, 1, 12, 0), WhitelistState.WHITELIST_ON),
        )

        val period = builder.buildMonth(samples, YearMonth.of(2026, 2))

        assertEquals("02.2026", period.title)
        assertEquals(28, period.buckets.size)
        assertEquals(WhitelistBinaryState.OFF, period.buckets.last().state)
    }

    @Test
    fun `year period uses selected full year by months`() {
        val samples = listOf(
            sample("on", millis(2026, 12, 31, 23, 0), WhitelistState.WHITELIST_ON),
            sample("off", millis(2027, 1, 1, 0, 0), WhitelistState.WHITELIST_OFF),
        )

        val period = builder.buildYear(samples, 2026)

        assertEquals("2026", period.title)
        assertEquals(12, period.buckets.size)
        assertEquals("01", period.buckets.first().label)
        assertEquals("12", period.buckets.last().label)
        assertEquals(WhitelistBinaryState.ON, period.buckets.last().state)
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
}
