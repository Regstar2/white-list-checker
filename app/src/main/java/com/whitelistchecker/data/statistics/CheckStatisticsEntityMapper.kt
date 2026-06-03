package com.whitelistchecker.data.statistics

import com.whitelistchecker.domain.model.statistics.CheckStatisticsSnapshot
import com.whitelistchecker.domain.model.statistics.CheckStatisticsSummary
import com.whitelistchecker.domain.model.statistics.DailyCheckStatistics
import com.whitelistchecker.domain.model.statistics.NetworkStatistics
import com.whitelistchecker.domain.model.statistics.RouteKindStatistics
import com.whitelistchecker.domain.model.statistics.TargetStatistics
import com.whitelistchecker.domain.statistics.CheckStatisticsConfig

internal object CheckStatisticsEntityMapper {

    fun defaultSummaryEntity(): CheckStatisticsSummaryEntity {
        return toEntity(CheckStatisticsSummary())
    }

    fun toEntity(summary: CheckStatisticsSummary): CheckStatisticsSummaryEntity {
        return CheckStatisticsSummaryEntity(
            id = CheckStatisticsConfig.SUMMARY_ROW_ID,
            totalRuns = summary.totalRuns,
            successRuns = summary.successRuns,
            partialFailureRuns = summary.partialFailureRuns,
            failureRuns = summary.failureRuns,
            cancelledRuns = summary.cancelledRuns,
            unknownRuns = summary.unknownRuns,
            successRate = summary.successRate,
            averageLatencyMs = summary.averageLatencyMs,
            lastRunAt = summary.lastRunAt,
            lastSuccessAt = summary.lastSuccessAt,
            lastFailureAt = summary.lastFailureAt,
            consecutiveFailureCount = summary.consecutiveFailureCount,
            updatedAt = summary.updatedAt,
            latencySampleCount = summary.latencySampleCount,
            latencySumMs = summary.latencySumMs,
            schemaVersion = CheckStatisticsConfig.SCHEMA_VERSION,
        )
    }

    fun toDomain(entity: CheckStatisticsSummaryEntity): CheckStatisticsSummary {
        return CheckStatisticsSummary(
            totalRuns = entity.totalRuns,
            successRuns = entity.successRuns,
            partialFailureRuns = entity.partialFailureRuns,
            failureRuns = entity.failureRuns,
            cancelledRuns = entity.cancelledRuns,
            unknownRuns = entity.unknownRuns,
            successRate = entity.successRate,
            averageLatencyMs = entity.averageLatencyMs,
            lastRunAt = entity.lastRunAt,
            lastSuccessAt = entity.lastSuccessAt,
            lastFailureAt = entity.lastFailureAt,
            consecutiveFailureCount = entity.consecutiveFailureCount,
            updatedAt = entity.updatedAt,
            latencySampleCount = entity.latencySampleCount,
            latencySumMs = entity.latencySumMs,
        )
    }

    fun toEntity(target: TargetStatistics): TargetStatisticsEntity {
        return TargetStatisticsEntity(
            targetId = target.targetId,
            targetLabel = target.targetLabel,
            targetHost = target.targetHost,
            totalChecks = target.totalChecks,
            successChecks = target.successChecks,
            failureChecks = target.failureChecks,
            timeoutChecks = target.timeoutChecks,
            successRate = target.successRate,
            averageLatencyMs = target.averageLatencyMs,
            lastCheckedAt = target.lastCheckedAt,
            lastSuccessAt = target.lastSuccessAt,
            lastFailureAt = target.lastFailureAt,
            consecutiveFailureCount = target.consecutiveFailureCount,
            updatedAt = target.updatedAt,
            latencySampleCount = target.latencySampleCount,
            latencySumMs = target.latencySumMs,
        )
    }

    fun toDomain(entity: TargetStatisticsEntity): TargetStatistics {
        return TargetStatistics(
            targetId = entity.targetId,
            targetLabel = entity.targetLabel,
            targetHost = entity.targetHost,
            totalChecks = entity.totalChecks,
            successChecks = entity.successChecks,
            failureChecks = entity.failureChecks,
            timeoutChecks = entity.timeoutChecks,
            successRate = entity.successRate,
            averageLatencyMs = entity.averageLatencyMs,
            lastCheckedAt = entity.lastCheckedAt,
            lastSuccessAt = entity.lastSuccessAt,
            lastFailureAt = entity.lastFailureAt,
            consecutiveFailureCount = entity.consecutiveFailureCount,
            updatedAt = entity.updatedAt,
            latencySampleCount = entity.latencySampleCount,
            latencySumMs = entity.latencySumMs,
        )
    }

    fun toEntity(route: RouteKindStatistics): RouteKindStatisticsEntity {
        return RouteKindStatisticsEntity(
            routeKind = route.routeKind,
            totalChecks = route.totalChecks,
            successChecks = route.successChecks,
            failureChecks = route.failureChecks,
            successRate = route.successRate,
            averageLatencyMs = route.averageLatencyMs,
            lastCheckedAt = route.lastCheckedAt,
            updatedAt = route.updatedAt,
            latencySampleCount = route.latencySampleCount,
            latencySumMs = route.latencySumMs,
        )
    }

    fun toDomain(entity: RouteKindStatisticsEntity): RouteKindStatistics {
        return RouteKindStatistics(
            routeKind = entity.routeKind,
            totalChecks = entity.totalChecks,
            successChecks = entity.successChecks,
            failureChecks = entity.failureChecks,
            successRate = entity.successRate,
            averageLatencyMs = entity.averageLatencyMs,
            lastCheckedAt = entity.lastCheckedAt,
            updatedAt = entity.updatedAt,
            latencySampleCount = entity.latencySampleCount,
            latencySumMs = entity.latencySumMs,
        )
    }

    fun toEntity(network: NetworkStatistics): NetworkStatisticsEntity {
        return NetworkStatisticsEntity(
            networkKey = network.networkKey,
            networkType = network.networkType,
            operatorName = network.operatorName,
            totalRuns = network.totalRuns,
            successRuns = network.successRuns,
            failureRuns = network.failureRuns,
            successRate = network.successRate,
            averageLatencyMs = network.averageLatencyMs,
            lastRunAt = network.lastRunAt,
            updatedAt = network.updatedAt,
            latencySampleCount = network.latencySampleCount,
            latencySumMs = network.latencySumMs,
        )
    }

    fun toDomain(entity: NetworkStatisticsEntity): NetworkStatistics {
        return NetworkStatistics(
            networkKey = entity.networkKey,
            networkType = entity.networkType,
            operatorName = entity.operatorName,
            totalRuns = entity.totalRuns,
            successRuns = entity.successRuns,
            failureRuns = entity.failureRuns,
            successRate = entity.successRate,
            averageLatencyMs = entity.averageLatencyMs,
            lastRunAt = entity.lastRunAt,
            updatedAt = entity.updatedAt,
            latencySampleCount = entity.latencySampleCount,
            latencySumMs = entity.latencySumMs,
        )
    }

    fun toEntity(daily: DailyCheckStatistics): DailyCheckStatisticsEntity {
        return DailyCheckStatisticsEntity(
            date = daily.date,
            totalRuns = daily.totalRuns,
            successRuns = daily.successRuns,
            partialFailureRuns = daily.partialFailureRuns,
            failureRuns = daily.failureRuns,
            totalTargetChecks = daily.totalTargetChecks,
            successTargetChecks = daily.successTargetChecks,
            failureTargetChecks = daily.failureTargetChecks,
            averageLatencyMs = daily.averageLatencyMs,
            updatedAt = daily.updatedAt,
            latencySampleCount = daily.latencySampleCount,
            latencySumMs = daily.latencySumMs,
        )
    }

    fun toDomain(entity: DailyCheckStatisticsEntity): DailyCheckStatistics {
        return DailyCheckStatistics(
            date = entity.date,
            totalRuns = entity.totalRuns,
            successRuns = entity.successRuns,
            partialFailureRuns = entity.partialFailureRuns,
            failureRuns = entity.failureRuns,
            totalTargetChecks = entity.totalTargetChecks,
            successTargetChecks = entity.successTargetChecks,
            failureTargetChecks = entity.failureTargetChecks,
            averageLatencyMs = entity.averageLatencyMs,
            updatedAt = entity.updatedAt,
            latencySampleCount = entity.latencySampleCount,
            latencySumMs = entity.latencySumMs,
        )
    }

    fun toSnapshot(
        summary: CheckStatisticsSummaryEntity?,
        targets: List<TargetStatisticsEntity>,
        routeKinds: List<RouteKindStatisticsEntity>,
        networks: List<NetworkStatisticsEntity>,
        daily: List<DailyCheckStatisticsEntity>,
    ): CheckStatisticsSnapshot {
        return CheckStatisticsSnapshot(
            summary = summary?.let(::toDomain) ?: CheckStatisticsSummary(),
            targets = targets.associate { it.targetId to toDomain(it) },
            routeKinds = routeKinds.associate { it.routeKind to toDomain(it) },
            networks = networks.associate { it.networkKey to toDomain(it) },
            daily = daily.associate { it.date to toDomain(it) },
        )
    }
}
