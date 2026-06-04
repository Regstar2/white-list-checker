package com.whitelistchecker.data.availability

import androidx.room.withTransaction
import com.whitelistchecker.data.db.AppDatabase
import com.whitelistchecker.domain.availability.WhitelistAvailabilityCalculator
import com.whitelistchecker.domain.availability.WhitelistAvailabilityConfig
import com.whitelistchecker.domain.availability.WhitelistAvailabilityRepository
import com.whitelistchecker.domain.availability.WhitelistAvailabilityTransitionDetector
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityEvent
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilitySnapshot
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilitySummary
import com.whitelistchecker.domain.model.availability.WhitelistDailyAvailability
import com.whitelistchecker.domain.model.availability.WhitelistTargetAvailabilityStats
import com.whitelistchecker.domain.model.history.CheckRun
import com.whitelistchecker.domain.model.history.CheckTargetResult
import com.whitelistchecker.domain.statistics.CheckStatisticsCalculator
import java.util.UUID

class RoomWhitelistAvailabilityRepository(
    private val database: AppDatabase,
    private val dao: WhitelistAvailabilityDao,
    private val calculator: WhitelistAvailabilityCalculator,
    private val transitionDetector: WhitelistAvailabilityTransitionDetector = WhitelistAvailabilityTransitionDetector(),
    private val dateFormatter: CheckStatisticsCalculator = CheckStatisticsCalculator(),
) : WhitelistAvailabilityRepository {

    override suspend fun updateFromCheckRun(
        checkRun: CheckRun,
        targetResults: List<CheckTargetResult>,
    ) {
        database.withTransaction {
            val before = loadSnapshotLocked()
            val events = mutableListOf<WhitelistAvailabilityEvent>()
            var updated = before

            targetResults.forEach { target ->
                val newState = com.whitelistchecker.domain.availability.WhitelistAvailabilityStateMapper
                    .fromCheckTargetStatus(target.status)
                val previousState = updated.lastKnownStates[target.targetId]
                    ?: com.whitelistchecker.domain.model.availability.WhitelistAvailabilityState.UNKNOWN
                val transition = transitionDetector.detect(previousState, newState)

                if (transitionDetector.isSignificantTransition(transition)) {
                    events += WhitelistAvailabilityEvent(
                        id = UUID.randomUUID().toString(),
                        targetId = target.targetId,
                        targetLabel = target.targetLabel,
                        previousState = previousState,
                        newState = newState,
                        transitionType = transition,
                        detectedAt = checkRun.finishedAtMillis,
                        checkRunId = checkRun.id,
                        routeKind = target.routeKind,
                        networkType = checkRun.networkType,
                        operatorName = checkRun.operatorName,
                        latencyMs = target.latencyMs,
                        errorCode = target.errorCode,
                        createdAt = checkRun.finishedAtMillis,
                    )
                }
            }

            updated = calculator.applyCheckRun(updated, checkRun, targetResults)
            updated = calculator.finalizeSummary(updated)

            if (events.isNotEmpty()) {
                dao.insertEvents(events.map(WhitelistAvailabilityEntityMapper::toEntity))
            }
            persistSnapshotLocked(updated)
            applyRetentionLocked(checkRun.finishedAtMillis)
        }
    }

    override suspend fun getSummary(): WhitelistAvailabilitySummary {
        return dao.getSummary(WhitelistAvailabilityConfig.SUMMARY_ROW_ID)?.let(WhitelistAvailabilityEntityMapper::toDomain)
            ?: WhitelistAvailabilitySummary()
    }

    override suspend fun summaryHasData(): Boolean {
        val summary = getSummary()
        return summary.totalTargets > 0 ||
            summary.totalBecameAvailableEvents > 0 ||
            summary.totalBecameUnavailableEvents > 0
    }

    override suspend fun getDailyStatistics(limit: Int): List<WhitelistDailyAvailability> {
        return dao.getDaily(limit).map(WhitelistAvailabilityEntityMapper::toDomain)
    }

    override suspend fun getTargetStatistics(limit: Int): List<WhitelistTargetAvailabilityStats> {
        return dao.getAllTargets()
            .map(WhitelistAvailabilityEntityMapper::toDomain)
            .sortedWith(
                compareByDescending<WhitelistTargetAvailabilityStats> { it.unstableScore }
                    .thenByDescending { it.becameAvailableCount + it.becameUnavailableCount },
            )
            .take(limit)
    }

    override suspend fun replaceAll(
        snapshot: WhitelistAvailabilitySnapshot,
        events: List<WhitelistAvailabilityEvent>,
    ) {
        database.withTransaction {
            val finalized = calculator.finalizeSummary(snapshot)
            dao.replaceAll(
                summary = WhitelistAvailabilityEntityMapper.toEntity(finalized.summary),
                targets = finalized.targets.values.map(WhitelistAvailabilityEntityMapper::toEntity),
                daily = finalized.daily.values.map(WhitelistAvailabilityEntityMapper::toEntity),
                events = events.map(WhitelistAvailabilityEntityMapper::toEntity),
            )
        }
    }

    override suspend fun clearAll() {
        database.withTransaction {
            dao.replaceAll(
                summary = WhitelistAvailabilityEntityMapper.defaultSummaryEntity(),
                targets = emptyList(),
                daily = emptyList(),
                events = emptyList(),
            )
        }
    }

    override suspend fun applyRetention(nowMillis: Long) {
        database.withTransaction {
            applyRetentionLocked(nowMillis)
        }
    }

    private suspend fun loadSnapshotLocked(): WhitelistAvailabilitySnapshot {
        return WhitelistAvailabilityEntityMapper.toSnapshot(
            summary = dao.getSummary(WhitelistAvailabilityConfig.SUMMARY_ROW_ID),
            targets = dao.getAllTargets(),
            daily = dao.getAllDaily(),
        )
    }

    private suspend fun persistSnapshotLocked(snapshot: WhitelistAvailabilitySnapshot) {
        dao.upsertSummary(WhitelistAvailabilityEntityMapper.toEntity(snapshot.summary))
        dao.upsertTargets(snapshot.targets.values.map(WhitelistAvailabilityEntityMapper::toEntity))
        dao.upsertDaily(snapshot.daily.values.map(WhitelistAvailabilityEntityMapper::toEntity))
    }

    private suspend fun applyRetentionLocked(nowMillis: Long) {
        val cutoffDate = dateFormatter.formatDateKey(
            nowMillis - WhitelistAvailabilityConfig.MAX_EVENT_AGE_MS,
        )
        dao.deleteDailyOlderThan(cutoffDate)
        dao.deleteEventsOlderThan(nowMillis - WhitelistAvailabilityConfig.MAX_EVENT_AGE_MS)

        val eventCount = dao.countEvents()
        if (eventCount > WhitelistAvailabilityConfig.MAX_EVENTS) {
            val excess = eventCount - WhitelistAvailabilityConfig.MAX_EVENTS
            val ids = dao.getOldestEventIds(excess)
            if (ids.isNotEmpty()) {
                dao.deleteEventsByIds(ids)
            }
        }
    }
}
