package com.whitelistchecker.domain.statistics

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class WhitelistTimelinePeriodBucketBuilder(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {

    fun buildDay(
        samples: List<WhitelistTimelineSample>,
        date: LocalDate,
    ): WhitelistTimelinePeriod {
        val start = date.atStartOfDay()
        val buckets = (0 until HOURS_IN_DAY).map { hour ->
            val bucketStart = start.plusHours(hour.toLong())
            buildBucket(
                label = HOUR_FORMATTER.format(bucketStart),
                start = bucketStart,
                end = bucketStart.plusHours(1),
                scale = WhitelistTimelineBucketScale.HOUR,
                samples = samples,
            )
        }
        return WhitelistTimelinePeriod(
            title = DAY_TITLE_FORMATTER.format(date),
            startMillis = buckets.first().startMillis,
            endMillis = buckets.last().endMillis,
            buckets = buckets,
        )
    }

    fun buildWeek(
        samples: List<WhitelistTimelineSample>,
        dateInsideWeek: LocalDate,
    ): WhitelistTimelinePeriod {
        val startDate = dateInsideWeek.with(DayOfWeek.MONDAY)
        val buckets = (0 until DAYS_IN_WEEK).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            buildBucket(
                label = DAY_FORMATTER.format(date),
                start = date.atStartOfDay(),
                end = date.plusDays(1).atStartOfDay(),
                scale = WhitelistTimelineBucketScale.DAY,
                samples = samples,
            )
        }
        return WhitelistTimelinePeriod(
            title = "${DAY_FORMATTER.format(startDate)}-${DAY_TITLE_FORMATTER.format(startDate.plusDays(DAYS_IN_WEEK - 1L))}",
            startMillis = buckets.first().startMillis,
            endMillis = buckets.last().endMillis,
            buckets = buckets,
        )
    }

    fun buildMonth(
        samples: List<WhitelistTimelineSample>,
        month: YearMonth,
    ): WhitelistTimelinePeriod {
        val buckets = (1..month.lengthOfMonth()).map { dayOfMonth ->
            val date = month.atDay(dayOfMonth)
            buildBucket(
                label = DAY_OF_MONTH_FORMATTER.format(date),
                start = date.atStartOfDay(),
                end = date.plusDays(1).atStartOfDay(),
                scale = WhitelistTimelineBucketScale.DAY,
                samples = samples,
            )
        }
        return WhitelistTimelinePeriod(
            title = MONTH_TITLE_FORMATTER.format(month),
            startMillis = buckets.first().startMillis,
            endMillis = buckets.last().endMillis,
            buckets = buckets,
        )
    }

    fun buildYear(
        samples: List<WhitelistTimelineSample>,
        year: Int,
    ): WhitelistTimelinePeriod {
        val buckets = (1..MONTHS_IN_YEAR).map { monthNumber ->
            val month = YearMonth.of(year, monthNumber)
            buildBucket(
                label = MONTH_LABEL_FORMATTER.format(month),
                start = month.atDay(1).atStartOfDay(),
                end = month.plusMonths(1).atDay(1).atStartOfDay(),
                scale = WhitelistTimelineBucketScale.MONTH,
                samples = samples,
            )
        }
        return WhitelistTimelinePeriod(
            title = year.toString(),
            startMillis = buckets.first().startMillis,
            endMillis = buckets.last().endMillis,
            buckets = buckets,
        )
    }

    private fun buildBucket(
        label: String,
        start: LocalDateTime,
        end: LocalDateTime,
        scale: WhitelistTimelineBucketScale,
        samples: List<WhitelistTimelineSample>,
    ): WhitelistTimelineBucket {
        val startMillis = start.atZone(zoneId).toInstant().toEpochMilli()
        val endMillis = end.atZone(zoneId).toInstant().toEpochMilli()
        val bucketSamples = samples.filter { sample ->
            sample.checkedAtMillis >= startMillis && sample.checkedAtMillis < endMillis
        }
        val onCount = bucketSamples.count { it.binaryState == WhitelistBinaryState.ON }
        val offCount = bucketSamples.count { it.binaryState == WhitelistBinaryState.OFF }
        val state = when {
            onCount > 0 -> WhitelistBinaryState.ON
            offCount > 0 -> WhitelistBinaryState.OFF
            else -> WhitelistBinaryState.UNKNOWN
        }
        return WhitelistTimelineBucket(
            label = label,
            startMillis = startMillis,
            endMillis = endMillis,
            scale = scale,
            state = state,
            sampleCount = bucketSamples.size,
            whitelistOnCount = onCount,
            whitelistOffCount = offCount,
        )
    }

    companion object {
        private const val HOURS_IN_DAY = 24
        private const val DAYS_IN_WEEK = 7L
        private const val MONTHS_IN_YEAR = 12
        private val HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH")
        private val DAY_FORMATTER = DateTimeFormatter.ofPattern("dd.MM")
        private val DAY_TITLE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private val DAY_OF_MONTH_FORMATTER = DateTimeFormatter.ofPattern("dd")
        private val MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM")
        private val MONTH_TITLE_FORMATTER = DateTimeFormatter.ofPattern("MM.yyyy")
    }
}
