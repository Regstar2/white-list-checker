package com.whitelistchecker.domain.statistics

import com.whitelistchecker.domain.model.history.CheckRun

class WhitelistTimelineWriter(
    private val repository: WhitelistTimelineRepository,
) {

    suspend fun onCheckRunSaved(checkRun: CheckRun): Boolean {
        return try {
            repository.saveSample(checkRun.toTimelineSample())
            repository.applyRetentionPolicy(checkRun.finishedAtMillis)
            true
        } catch (_: Exception) {
            false
        }
    }
}

fun CheckRun.toTimelineSample(): WhitelistTimelineSample {
    return WhitelistTimelineSample(
        checkRunId = id,
        checkedAtMillis = finishedAtMillis,
        whitelistState = whitelistState,
        binaryState = whitelistState.toBinaryWhitelistState(),
        createdAtMillis = createdAtMillis,
    )
}
