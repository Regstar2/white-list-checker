package com.whitelistchecker.domain.availability

import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityEvent
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilitySnapshot
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityState
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilitySummary
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityTransitionType
import com.whitelistchecker.domain.model.availability.WhitelistDailyAvailability
import com.whitelistchecker.domain.model.availability.WhitelistTargetAvailabilityStats
import com.whitelistchecker.domain.model.history.CheckRun
import com.whitelistchecker.domain.model.history.CheckRunWithTargetResults
import com.whitelistchecker.domain.model.history.CheckTargetResult
import com.whitelistchecker.domain.statistics.CheckStatisticsCalculator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class WhitelistAvailabilityCalculator(
    private val stateMapper: WhitelistAvailabilityStateMapper = WhitelistAvailabilityStateMapper,
    private val transitionDetector: WhitelistAvailabilityTransitionDetector = WhitelistAvailabilityTransitionDetector(),
    private val dateFormatter: CheckStatisticsCalculator = CheckStatisticsCalculator(),
) {

    fun emptySnapshot(): WhitelistAvailabilitySnapshot = WhitelistAvailabilitySnapshot()

    fun rebuildFromHistory(
        runs: List<CheckRunWithTargetResults>,
    ): WhitelistAvailabilitySnapshot {
        var snapshot = emptySnapshot()
        runs.sortedBy { it.run.finishedAtMillis }.forEach { entry ->
            snapshot = applyCheckRun(snapshot, entry.run, entry.targetResults)
        }
        return finalizeSummary(snapshot)
    }

    fun applyCheckRun(
        snapshot: WhitelistAvailabilitySnapshot,
        checkRun: CheckRun,
        targetResults: List<CheckTargetResult>,
    ): WhitelistAvailabilitySnapshot {
        var updated = snapshot
        val detectedAt = checkRun.finishedAtMillis
        val dateKey = dateFormatter.formatDateKey(detectedAt)

        targetResults.forEach { target ->
            val newState = stateMapper.fromCheckTargetStatus(target.status)
            val previousState = updated.lastKnownStates[target.targetId]
                ?: WhitelistAvailabilityState.UNKNOWN
            val transition = transitionDetector.detect(previousState, newState)

            if (transitionDetector.isSignificantTransition(transition)) {
                val event = WhitelistAvailabilityEvent(
                    id = UUID.randomUUID().toString(),
                    targetId = target.targetId,
                    targetLabel = target.targetLabel,
                    previousState = previousState,
                    newState = newState,
                    transitionType = transition,
                    detectedAt = detectedAt,
                    checkRunId = checkRun.id,
                    routeKind = target.routeKind,
                    networkType = checkRun.networkType,
                    operatorName = checkRun.operatorName,
                    latencyMs = target.latencyMs,
                    errorCode = target.errorCode,
                    createdAt = detectedAt,
                )
                updated = applyEvent(updated, event, dateKey)
            }

            updated = updateTargetState(
                snapshot = updated,
                target = target,
                newState = newState,
                detectedAt = detectedAt,
                dateKey = dateKey,
            )
        }

        val daily = updated.daily[dateKey]?.copy(
            checkRunCount = (updated.daily[dateKey]?.checkRunCount ?: 0) + 1,
        ) ?: WhitelistDailyAvailability(date = dateKey, checkRunCount = 1)

        return updated.copy(
            daily = updated.daily + (dateKey to daily),
            lastKnownStates = buildLastKnownStates(updated.targets),
        )
    }

    fun finalizeSummary(snapshot: WhitelistAvailabilitySnapshot): WhitelistAvailabilitySnapshot {
        val targets = snapshot.targets.values
        val available = targets.count { it.currentState == WhitelistAvailabilityState.AVAILABLE }
        val unavailable = targets.count { it.currentState == WhitelistAvailabilityState.UNAVAILABLE }
        val unknown = targets.count {
            it.currentState == WhitelistAvailabilityState.UNKNOWN ||
                it.currentState == WhitelistAvailabilityState.ERROR
        }
        val known = available + unavailable
        val percent = if (known > 0) available.toDouble() / known.toDouble() else null

        val mostStable = targets
            .filter { it.unstableScore == 0 && it.availableChecks + it.unavailableChecks > 0 }
            .maxByOrNull { it.availableChecks + it.unavailableChecks }
            ?.displayLabel

        val mostUnstable = targets
            .filter { it.unstableScore > 0 }
            .maxByOrNull { it.unstableScore }
            ?.displayLabel

        val rangeStart = targets.mapNotNull { it.lastSeenAt }.minOrNull()
        val rangeEnd = targets.mapNotNull { it.lastSeenAt }.maxOrNull()

        val summary = snapshot.summary.copy(
            totalTargets = targets.size,
            currentlyAvailableTargets = available,
            currentlyUnavailableTargets = unavailable,
            unknownTargets = unknown,
            availabilityPercent = percent,
            mostStableTargetLabel = mostStable,
            mostUnstableTargetLabel = mostUnstable,
            dataRangeStart = rangeStart,
            dataRangeEnd = rangeEnd,
        )

        val daily = snapshot.daily.mapValues { (_, day) ->
            val knownDay = day.availableTargetCount + day.unavailableTargetCount
            day.copy(
                availabilityPercent = if (knownDay > 0) {
                    day.availableTargetCount.toDouble() / knownDay.toDouble()
                } else {
                    null
                },
            )
        }

        return snapshot.copy(summary = summary, daily = daily)
    }

    private fun applyEvent(
        snapshot: WhitelistAvailabilitySnapshot,
        event: WhitelistAvailabilityEvent,
        dateKey: String,
    ): WhitelistAvailabilitySnapshot {
        var summary = snapshot.summary
        if (transitionDetector.isBecameAvailable(event.transitionType)) {
            summary = summary.copy(
                totalBecameAvailableEvents = summary.totalBecameAvailableEvents + 1,
                lastBecameAvailableAt = event.detectedAt,
            )
        }
        if (transitionDetector.isBecameUnavailable(event.transitionType)) {
            summary = summary.copy(
                totalBecameUnavailableEvents = summary.totalBecameUnavailableEvents + 1,
                lastBecameUnavailableAt = event.detectedAt,
            )
        }
        summary = summary.copy(
            lastUpdatedAt = event.detectedAt,
            dataRangeStart = summary.dataRangeStart ?: event.detectedAt,
            dataRangeEnd = event.detectedAt,
        )

        val day = snapshot.daily[dateKey] ?: WhitelistDailyAvailability(date = dateKey)
        val updatedDay = when {
            transitionDetector.isBecameAvailable(event.transitionType) -> day.copy(
                becameAvailableCount = day.becameAvailableCount + 1,
            )
            transitionDetector.isBecameUnavailable(event.transitionType) -> day.copy(
                becameUnavailableCount = day.becameUnavailableCount + 1,
            )
            else -> day
        }

        return snapshot.copy(
            summary = summary,
            daily = snapshot.daily + (dateKey to updatedDay),
        )
    }

    private fun updateTargetState(
        snapshot: WhitelistAvailabilitySnapshot,
        target: CheckTargetResult,
        newState: WhitelistAvailabilityState,
        detectedAt: Long,
        dateKey: String,
    ): WhitelistAvailabilitySnapshot {
        val existing = snapshot.targets[target.targetId]
        val becameAvailable = existing?.becameAvailableCount ?: 0
        val becameUnavailable = existing?.becameUnavailableCount ?: 0
        var availableChecks = existing?.availableChecks ?: 0
        var unavailableChecks = existing?.unavailableChecks ?: 0

        when (newState) {
            WhitelistAvailabilityState.AVAILABLE -> availableChecks++
            WhitelistAvailabilityState.UNAVAILABLE -> unavailableChecks++
            else -> Unit
        }

        val previousState = snapshot.lastKnownStates[target.targetId]
            ?: WhitelistAvailabilityState.UNKNOWN
        val transition = transitionDetector.detect(previousState, newState)
        val updatedBecameAvailable = if (transitionDetector.isBecameAvailable(transition)) {
            becameAvailable + 1
        } else {
            becameAvailable
        }
        val updatedBecameUnavailable = if (transitionDetector.isBecameUnavailable(transition)) {
            becameUnavailable + 1
        } else {
            becameUnavailable
        }

        val knownChecks = availableChecks + unavailableChecks
        val targetPercent = if (knownChecks > 0) {
            availableChecks.toDouble() / knownChecks.toDouble()
        } else {
            null
        }

        val stats = WhitelistTargetAvailabilityStats(
            targetId = target.targetId,
            displayLabel = target.targetLabel,
            currentState = newState,
            becameAvailableCount = updatedBecameAvailable,
            becameUnavailableCount = updatedBecameUnavailable,
            availabilityPercent = targetPercent,
            lastBecameAvailableAt = if (transitionDetector.isBecameAvailable(transition)) {
                detectedAt
            } else {
                existing?.lastBecameAvailableAt
            },
            lastBecameUnavailableAt = if (transitionDetector.isBecameUnavailable(transition)) {
                detectedAt
            } else {
                existing?.lastBecameUnavailableAt
            },
            lastSeenAt = detectedAt,
            unstableScore = updatedBecameAvailable + updatedBecameUnavailable,
            availableChecks = availableChecks,
            unavailableChecks = unavailableChecks,
        )

        val day = snapshot.daily[dateKey] ?: WhitelistDailyAvailability(date = dateKey)
        val updatedDay = when (newState) {
            WhitelistAvailabilityState.AVAILABLE -> day.copy(
                availableTargetCount = day.availableTargetCount + 1,
            )
            WhitelistAvailabilityState.UNAVAILABLE -> day.copy(
                unavailableTargetCount = day.unavailableTargetCount + 1,
            )
            else -> day
        }

        return snapshot.copy(
            targets = snapshot.targets + (target.targetId to stats),
            daily = snapshot.daily + (dateKey to updatedDay),
            lastKnownStates = snapshot.lastKnownStates + (target.targetId to newState),
        )
    }

    private fun buildLastKnownStates(
        targets: Map<String, WhitelistTargetAvailabilityStats>,
    ): Map<String, WhitelistAvailabilityState> {
        return targets.mapValues { (_, stats) -> stats.currentState }
    }
}
