package com.whitelistchecker.domain.statistics

import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class LoadWhitelistTimelineDashboardUseCase(
    private val repository: WhitelistTimelineRepository,
    private val staleResolver: StatisticsStaleResolver = StatisticsStaleResolver(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {

    suspend fun load(nowMillis: Long = System.currentTimeMillis()): WhitelistTimelineLoadResult {
        return try {
            val samples = repository.getSamplesSince(nowMillis - WhitelistTimelineConfig.LOAD_RANGE_MS)
                .sortedBy { it.checkedAtMillis }
            if (samples.isEmpty()) {
                return WhitelistTimelineLoadResult.Empty
            }
            WhitelistTimelineLoadResult.Success(buildDashboard(samples, nowMillis))
        } catch (exception: Exception) {
            WhitelistTimelineLoadResult.Failure(exception)
        }
    }

    private fun buildDashboard(
        samples: List<WhitelistTimelineSample>,
        nowMillis: Long,
    ): WhitelistTimelineDashboard {
        val binarySamples = samples.filter { it.binaryState != WhitelistBinaryState.UNKNOWN }
        val latest = samples.maxByOrNull { it.checkedAtMillis }
        val onCount = binarySamples.count { it.binaryState == WhitelistBinaryState.ON }
        val offCount = binarySamples.count { it.binaryState == WhitelistBinaryState.OFF }
        val latestBinary = samples.asReversed().firstOrNull { it.binaryState != WhitelistBinaryState.UNKNOWN }

        return WhitelistTimelineDashboard(
            currentState = latestBinary?.binaryState ?: WhitelistBinaryState.UNKNOWN,
            currentStateAtMillis = latestBinary?.checkedAtMillis,
            totalSamples = samples.size,
            binarySamples = binarySamples.size,
            whitelistOnSamples = onCount,
            whitelistOffSamples = offCount,
            whitelistOnPercent = if (binarySamples.isEmpty()) null else onCount.toDouble() / binarySamples.size.toDouble(),
            dayHourly = buildTodayHourly(samples, nowMillis),
            weekDaily = buildLastDays(samples, nowMillis, days = 7),
            monthDaily = buildLastDays(samples, nowMillis, days = 30),
            yearMonthly = buildLastMonths(samples, nowMillis, months = 12),
            lastUpdatedAt = latest?.checkedAtMillis,
            isStale = staleResolver.isStale(latest?.checkedAtMillis, nowMillis),
        )
    }

    private fun buildTodayHourly(
        samples: List<WhitelistTimelineSample>,
        nowMillis: Long,
    ): List<WhitelistTimelineBucket> {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val start = today.atStartOfDay()
        return (0 until HOURS_IN_DAY).map { hour ->
            val bucketStart = start.plusHours(hour.toLong())
            val bucketEnd = bucketStart.plusHours(1)
            buildBucket(
                label = HOUR_FORMATTER.format(bucketStart),
                start = bucketStart,
                end = bucketEnd,
                scale = WhitelistTimelineBucketScale.HOUR,
                samples = samples,
            )
        }
    }

    private fun buildLastDays(
        samples: List<WhitelistTimelineSample>,
        nowMillis: Long,
        days: Long,
    ): List<WhitelistTimelineBucket> {
        val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
        val startDate = today.minusDays(days - 1)
        return (0 until days).map { offset ->
            val date = startDate.plusDays(offset)
            buildBucket(
                label = DAY_FORMATTER.format(date),
                start = date.atStartOfDay(),
                end = date.plusDays(1).atStartOfDay(),
                scale = WhitelistTimelineBucketScale.DAY,
                samples = samples,
            )
        }
    }

    private fun buildLastMonths(
        samples: List<WhitelistTimelineSample>,
        nowMillis: Long,
        months: Long,
    ): List<WhitelistTimelineBucket> {
        val currentMonth = YearMonth.from(Instant.ofEpochMilli(nowMillis).atZone(zoneId))
        val startMonth = currentMonth.minusMonths(months - 1)
        return (0 until months).map { offset ->
            val month = startMonth.plusMonths(offset)
            buildBucket(
                label = MONTH_FORMATTER.format(month),
                start = month.atDay(1).atStartOfDay(),
                end = month.plusMonths(1).atDay(1).atStartOfDay(),
                scale = WhitelistTimelineBucketScale.MONTH,
                samples = samples,
            )
        }
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
        private val HOUR_FORMATTER = DateTimeFormatter.ofPattern("HH")
        private val DAY_FORMATTER = DateTimeFormatter.ofPattern("dd.MM")
        private val MONTH_FORMATTER = DateTimeFormatter.ofPattern("MM.yy")
    }
}
