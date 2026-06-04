package com.whitelistchecker.data.availability

import com.whitelistchecker.domain.availability.WhitelistAvailabilityConfig
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityEvent
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilitySnapshot
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityState
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilitySummary
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityTransitionType
import com.whitelistchecker.domain.model.availability.WhitelistDailyAvailability
import com.whitelistchecker.domain.model.availability.WhitelistTargetAvailabilityStats

internal object WhitelistAvailabilityEntityMapper {

    fun defaultSummaryEntity(): WhitelistAvailabilitySummaryEntity {
        return toEntity(WhitelistAvailabilitySummary())
    }

    fun toEntity(summary: WhitelistAvailabilitySummary): WhitelistAvailabilitySummaryEntity {
        return WhitelistAvailabilitySummaryEntity(
            id = WhitelistAvailabilityConfig.SUMMARY_ROW_ID,
            totalTargets = summary.totalTargets,
            currentlyAvailableTargets = summary.currentlyAvailableTargets,
            currentlyUnavailableTargets = summary.currentlyUnavailableTargets,
            unknownTargets = summary.unknownTargets,
            totalBecameAvailableEvents = summary.totalBecameAvailableEvents,
            totalBecameUnavailableEvents = summary.totalBecameUnavailableEvents,
            availabilityPercent = summary.availabilityPercent,
            lastBecameAvailableAt = summary.lastBecameAvailableAt,
            lastBecameUnavailableAt = summary.lastBecameUnavailableAt,
            lastUpdatedAt = summary.lastUpdatedAt,
            dataRangeStart = summary.dataRangeStart,
            dataRangeEnd = summary.dataRangeEnd,
            mostStableTargetLabel = summary.mostStableTargetLabel,
            mostUnstableTargetLabel = summary.mostUnstableTargetLabel,
            schemaVersion = WhitelistAvailabilityConfig.SCHEMA_VERSION,
        )
    }

    fun toDomain(entity: WhitelistAvailabilitySummaryEntity): WhitelistAvailabilitySummary {
        return WhitelistAvailabilitySummary(
            totalTargets = entity.totalTargets,
            currentlyAvailableTargets = entity.currentlyAvailableTargets,
            currentlyUnavailableTargets = entity.currentlyUnavailableTargets,
            unknownTargets = entity.unknownTargets,
            totalBecameAvailableEvents = entity.totalBecameAvailableEvents,
            totalBecameUnavailableEvents = entity.totalBecameUnavailableEvents,
            availabilityPercent = entity.availabilityPercent,
            lastBecameAvailableAt = entity.lastBecameAvailableAt,
            lastBecameUnavailableAt = entity.lastBecameUnavailableAt,
            lastUpdatedAt = entity.lastUpdatedAt,
            dataRangeStart = entity.dataRangeStart,
            dataRangeEnd = entity.dataRangeEnd,
            mostStableTargetLabel = entity.mostStableTargetLabel,
            mostUnstableTargetLabel = entity.mostUnstableTargetLabel,
        )
    }

    fun toEntity(event: WhitelistAvailabilityEvent): WhitelistAvailabilityEventEntity {
        return WhitelistAvailabilityEventEntity(
            id = event.id,
            targetId = event.targetId,
            targetLabel = event.targetLabel,
            previousState = event.previousState.name,
            newState = event.newState.name,
            transitionType = event.transitionType.name,
            detectedAt = event.detectedAt,
            checkRunId = event.checkRunId,
            routeKind = event.routeKind,
            networkType = event.networkType,
            operatorName = event.operatorName,
            latencyMs = event.latencyMs,
            errorCode = event.errorCode,
            createdAt = event.createdAt,
        )
    }

    fun toEntity(target: WhitelistTargetAvailabilityStats): WhitelistTargetAvailabilityEntity {
        return WhitelistTargetAvailabilityEntity(
            targetId = target.targetId,
            displayLabel = target.displayLabel,
            currentState = target.currentState.name,
            becameAvailableCount = target.becameAvailableCount,
            becameUnavailableCount = target.becameUnavailableCount,
            availabilityPercent = target.availabilityPercent,
            lastBecameAvailableAt = target.lastBecameAvailableAt,
            lastBecameUnavailableAt = target.lastBecameUnavailableAt,
            lastSeenAt = target.lastSeenAt,
            unstableScore = target.unstableScore,
            availableChecks = target.availableChecks,
            unavailableChecks = target.unavailableChecks,
        )
    }

    fun toDomain(entity: WhitelistTargetAvailabilityEntity): WhitelistTargetAvailabilityStats {
        return WhitelistTargetAvailabilityStats(
            targetId = entity.targetId,
            displayLabel = entity.displayLabel,
            currentState = WhitelistAvailabilityState.valueOf(entity.currentState),
            becameAvailableCount = entity.becameAvailableCount,
            becameUnavailableCount = entity.becameUnavailableCount,
            availabilityPercent = entity.availabilityPercent,
            lastBecameAvailableAt = entity.lastBecameAvailableAt,
            lastBecameUnavailableAt = entity.lastBecameUnavailableAt,
            lastSeenAt = entity.lastSeenAt,
            unstableScore = entity.unstableScore,
            availableChecks = entity.availableChecks,
            unavailableChecks = entity.unavailableChecks,
        )
    }

    fun toEntity(daily: WhitelistDailyAvailability): WhitelistDailyAvailabilityEntity {
        return WhitelistDailyAvailabilityEntity(
            date = daily.date,
            availableTargetCount = daily.availableTargetCount,
            unavailableTargetCount = daily.unavailableTargetCount,
            becameAvailableCount = daily.becameAvailableCount,
            becameUnavailableCount = daily.becameUnavailableCount,
            availabilityPercent = daily.availabilityPercent,
            checkRunCount = daily.checkRunCount,
        )
    }

    fun toDomain(entity: WhitelistDailyAvailabilityEntity): WhitelistDailyAvailability {
        return WhitelistDailyAvailability(
            date = entity.date,
            availableTargetCount = entity.availableTargetCount,
            unavailableTargetCount = entity.unavailableTargetCount,
            becameAvailableCount = entity.becameAvailableCount,
            becameUnavailableCount = entity.becameUnavailableCount,
            availabilityPercent = entity.availabilityPercent,
            checkRunCount = entity.checkRunCount,
        )
    }

    fun toSnapshot(
        summary: WhitelistAvailabilitySummaryEntity?,
        targets: List<WhitelistTargetAvailabilityEntity>,
        daily: List<WhitelistDailyAvailabilityEntity>,
    ): WhitelistAvailabilitySnapshot {
        val domainTargets = targets.associate { entity ->
            entity.targetId to toDomain(entity)
        }
        return WhitelistAvailabilitySnapshot(
            summary = summary?.let(::toDomain) ?: WhitelistAvailabilitySummary(),
            targets = domainTargets,
            daily = daily.associate { entity -> entity.date to toDomain(entity) },
            lastKnownStates = domainTargets.mapValues { (_, stats) -> stats.currentState },
        )
    }
}
