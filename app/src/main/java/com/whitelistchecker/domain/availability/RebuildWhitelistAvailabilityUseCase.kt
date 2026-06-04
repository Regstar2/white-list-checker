package com.whitelistchecker.domain.availability

import com.whitelistchecker.domain.history.CheckHistoryConfig
import com.whitelistchecker.domain.history.CheckHistoryRepository
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityEvent
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityState
import com.whitelistchecker.domain.statistics.RebuildStatisticsResult
import java.util.UUID

class RebuildWhitelistAvailabilityUseCase(
    private val checkHistoryRepository: CheckHistoryRepository,
    private val whitelistAvailabilityRepository: WhitelistAvailabilityRepository,
    private val calculator: WhitelistAvailabilityCalculator,
    private val transitionDetector: WhitelistAvailabilityTransitionDetector = WhitelistAvailabilityTransitionDetector(),
) {

    suspend fun rebuildFromHistory(): RebuildStatisticsResult {
        return try {
            val runs = checkHistoryRepository.getRecentCheckRuns(CheckHistoryConfig.MAX_CHECK_RUNS)
                .asReversed()
            val events = mutableListOf<WhitelistAvailabilityEvent>()
            var snapshot = calculator.emptySnapshot()

            runs.forEach { entry ->
                entry.targetResults.forEach { target ->
                    val newState = WhitelistAvailabilityStateMapper.fromCheckTargetStatus(target.status)
                    val previousState = snapshot.lastKnownStates[target.targetId]
                        ?: WhitelistAvailabilityState.UNKNOWN
                    val transition = transitionDetector.detect(previousState, newState)
                    if (transitionDetector.isSignificantTransition(transition)) {
                        events += WhitelistAvailabilityEvent(
                            id = UUID.randomUUID().toString(),
                            targetId = target.targetId,
                            targetLabel = target.targetLabel,
                            previousState = previousState,
                            newState = newState,
                            transitionType = transition,
                            detectedAt = entry.run.finishedAtMillis,
                            checkRunId = entry.run.id,
                            routeKind = target.routeKind,
                            networkType = entry.run.networkType,
                            operatorName = entry.run.operatorName,
                            latencyMs = target.latencyMs,
                            errorCode = target.errorCode,
                            createdAt = entry.run.finishedAtMillis,
                        )
                    }
                }
                snapshot = calculator.applyCheckRun(snapshot, entry.run, entry.targetResults)
            }

            snapshot = calculator.finalizeSummary(snapshot)
            whitelistAvailabilityRepository.replaceAll(snapshot, events)
            RebuildStatisticsResult.Success
        } catch (exception: Exception) {
            RebuildStatisticsResult.Failure(exception)
        }
    }
}
