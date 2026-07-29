package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.model.WhitelistState

enum class WhitelistBinaryState {
    ON,
    OFF,
    UNKNOWN,
}

enum class WhitelistTimelineBucketScale {
    HOUR,
    DAY,
    MONTH,
}

data class WhitelistTimelineSample(
    val checkRunId: String,
    val checkedAtMillis: Long,
    val whitelistState: WhitelistState,
    val binaryState: WhitelistBinaryState,
    val createdAtMillis: Long,
)

data class WhitelistTimelineBucket(
    val label: String,
    val startMillis: Long,
    val endMillis: Long,
    val scale: WhitelistTimelineBucketScale,
    val state: WhitelistBinaryState,
    val sampleCount: Int,
    val whitelistOnCount: Int,
    val whitelistOffCount: Int,
)

data class WhitelistTimelineDashboard(
    val currentState: WhitelistBinaryState,
    val currentStateAtMillis: Long?,
    val totalSamples: Int,
    val binarySamples: Int,
    val whitelistOnSamples: Int,
    val whitelistOffSamples: Int,
    val whitelistOnPercent: Double?,
    val dayHourly: List<WhitelistTimelineBucket>,
    val weekDaily: List<WhitelistTimelineBucket>,
    val monthDaily: List<WhitelistTimelineBucket>,
    val yearMonthly: List<WhitelistTimelineBucket>,
    val lastUpdatedAt: Long?,
    val isStale: Boolean,
)

sealed class WhitelistTimelineLoadResult {
    data object Empty : WhitelistTimelineLoadResult()

    data class Success(val dashboard: WhitelistTimelineDashboard) : WhitelistTimelineLoadResult()

    data class Failure(val cause: Throwable) : WhitelistTimelineLoadResult()
}

fun WhitelistState.toBinaryWhitelistState(): WhitelistBinaryState {
    return when (this) {
        WhitelistState.WHITELIST_ON -> WhitelistBinaryState.ON
        WhitelistState.WHITELIST_OFF -> WhitelistBinaryState.OFF
        else -> WhitelistBinaryState.UNKNOWN
    }
}
