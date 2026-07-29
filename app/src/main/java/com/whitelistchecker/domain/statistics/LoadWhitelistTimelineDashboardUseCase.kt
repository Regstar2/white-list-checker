package com.whitelistchecker.domain.statistics

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

class LoadWhitelistTimelineDashboardUseCase(
    private val repository: WhitelistTimelineRepository,
    private val staleResolver: StatisticsStaleResolver = StatisticsStaleResolver(),
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {

    private val periodBucketBuilder = WhitelistTimelinePeriodBucketBuilder(zoneId)

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
        val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()

        return WhitelistTimelineDashboard(
            currentState = latestBinary?.binaryState ?: WhitelistBinaryState.UNKNOWN,
            currentStateAtMillis = latestBinary?.checkedAtMillis,
            totalSamples = samples.size,
            binarySamples = binarySamples.size,
            whitelistOnSamples = onCount,
            whitelistOffSamples = offCount,
            whitelistOnPercent = if (binarySamples.isEmpty()) null else onCount.toDouble() / binarySamples.size.toDouble(),
            generatedAtMillis = nowMillis,
            samples = samples,
            dayHourly = periodBucketBuilder.buildDay(samples, nowDate).buckets,
            weekDaily = periodBucketBuilder.buildWeek(samples, nowDate).buckets,
            monthDaily = periodBucketBuilder.buildMonth(samples, YearMonth.from(nowDate)).buckets,
            yearMonthly = periodBucketBuilder.buildYear(samples, nowDate.year).buckets,
            lastUpdatedAt = latest?.checkedAtMillis,
            isStale = staleResolver.isStale(latest?.checkedAtMillis, nowMillis),
        )
    }
}
