package com.whitelistchecker.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.availability.WhitelistAvailabilityDashboard
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityEvent
import com.whitelistchecker.domain.model.availability.WhitelistTargetAvailabilityStats
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow
import com.whitelistchecker.ui.components.StatusChip
import com.whitelistchecker.ui.components.StatusTone

private const val TARGET_STATES_COLLAPSED_LIMIT = 8

@Composable
fun StatisticsFreshnessHeader(freshness: StatisticsFreshnessUi) {
    val resources = LocalContext.current.resources
    val nowMillis = remember(freshness.dataUpdatedAt) { System.currentTimeMillis() }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (freshness.isStale) {
            StatusChip(
                text = stringResource(R.string.statistics_data_stale),
                tone = StatusTone.WARNING,
            )
        }
        if (freshness.isLowSample) {
            Text(
                text = stringResource(R.string.statistics_low_sample_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CompactDetailRow(
            stringResource(R.string.statistics_updated_at),
            StatisticsValueFormatter.formatRelativeTime(resources, freshness.dataUpdatedAt, nowMillis),
        )
        CompactDetailRow(
            stringResource(R.string.statistics_last_check),
            StatisticsValueFormatter.formatRelativeTime(resources, freshness.lastCheckAt, nowMillis),
        )
        CompactDetailRow(
            stringResource(R.string.statistics_last_check_status),
            StatisticsValueFormatter.formatTechnicalCheckStatus(resources, freshness.lastCheckStatus),
        )
        if (freshness.targetsCheckedTotal > 0) {
            CompactDetailRow(
                stringResource(R.string.statistics_targets_checked),
                resources.getString(
                    R.string.statistics_targets_checked_value,
                    freshness.targetsCheckedAvailable,
                    freshness.targetsCheckedTotal,
                ),
            )
        }
    }
}

@Composable
fun WhitelistStatisticsContent(
    dashboard: WhitelistAvailabilityDashboard,
) {
    val resources = LocalContext.current.resources
    val nowMillis = remember(dashboard.lastUpdatedAt) { System.currentTimeMillis() }
    val summary = dashboard.summary
    val availabilityLabel = StatisticsValueFormatter.formatPercentFraction(summary.availabilityPercent)
        .ifBlank { stringResource(R.string.statistics_value_not_available) }
    val periodChanges = summary.totalBecameAvailableEvents + summary.totalBecameUnavailableEvents

    AppCard(title = stringResource(R.string.whitelist_availability_summary_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (dashboard.isStale) {
                StatusChip(
                    text = stringResource(R.string.statistics_data_stale),
                    tone = StatusTone.WARNING,
                )
            }
            CompactDetailRow(
                stringResource(R.string.whitelist_availability_current_available),
                summary.currentlyAvailableTargets.toString(),
            )
            CompactDetailRow(
                stringResource(R.string.whitelist_availability_current_unavailable),
                summary.currentlyUnavailableTargets.toString(),
            )
            if (summary.unknownTargets > 0) {
                CompactDetailRow(
                    stringResource(R.string.whitelist_availability_current_unknown),
                    summary.unknownTargets.toString(),
                )
            }
            CompactDetailRow(stringResource(R.string.whitelist_availability_percent), availabilityLabel)
            if (periodChanges > 0) {
                CompactDetailRow(
                    stringResource(R.string.whitelist_availability_period_changes),
                    periodChanges.toString(),
                )
            }
            CompactDetailRow(
                stringResource(R.string.whitelist_availability_became_available),
                summary.totalBecameAvailableEvents.toString(),
            )
            CompactDetailRow(
                stringResource(R.string.whitelist_availability_became_unavailable),
                summary.totalBecameUnavailableEvents.toString(),
            )
            CompactDetailRow(
                stringResource(R.string.whitelist_availability_last_available),
                StatisticsValueFormatter.formatRelativeTime(resources, summary.lastBecameAvailableAt, nowMillis),
            )
            CompactDetailRow(
                stringResource(R.string.whitelist_availability_last_unavailable),
                StatisticsValueFormatter.formatRelativeTime(resources, summary.lastBecameUnavailableAt, nowMillis),
            )
        }
    }

    if (dashboard.targetStates.isNotEmpty()) {
        CurrentTargetStatesSection(targets = dashboard.targetStates)
    }

    RecentWhitelistChangesSection(events = dashboard.recentEvents, nowMillis = nowMillis)

    DailyAvailabilitySection(dashboard = dashboard)

    TransitionsSection(dashboard = dashboard)

    StableUnstableTargetsSection(dashboard = dashboard)
}

@Composable
private fun CurrentTargetStatesSection(targets: List<WhitelistTargetAvailabilityStats>) {
    var expanded by remember(targets.size) { mutableStateOf(false) }
    val canExpand = targets.size > TARGET_STATES_COLLAPSED_LIMIT
    val visible = if (expanded || !canExpand) targets else targets.take(TARGET_STATES_COLLAPSED_LIMIT)
    val resources = LocalContext.current.resources

    AppCard(title = stringResource(R.string.whitelist_current_targets_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            visible.forEach { target ->
                val label = StatisticsValueFormatter.formatEndpointLabel(
                    resources,
                    target.displayLabel,
                    target.targetId,
                )
                val stateLabel = StatisticsValueFormatter.formatWhitelistState(resources, target.currentState)
                CompactDetailRow(label, stateLabel)
            }
            if (canExpand) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (expanded) {
                            stringResource(R.string.statistics_targets_show_less)
                        } else {
                            stringResource(R.string.statistics_targets_show_all, targets.size)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentWhitelistChangesSection(
    events: List<WhitelistAvailabilityEvent>,
    nowMillis: Long,
) {
    val resources = LocalContext.current.resources
    AppCard(title = stringResource(R.string.whitelist_recent_changes_title)) {
        if (events.isEmpty()) {
            Text(
                text = stringResource(R.string.whitelist_recent_changes_empty),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.whitelist_recent_changes_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                events.forEach { event ->
                    Text(
                        text = StatisticsValueFormatter.formatRecentEventLine(resources, event, nowMillis),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyAvailabilitySection(dashboard: WhitelistAvailabilityDashboard) {
    if (dashboard.daily.isEmpty()) return
    val resources = LocalContext.current.resources
    val dailyPercents = dashboard.daily.map { ((it.availabilityPercent ?: 0.0) * 100.0).toFloat() }

    AppCard(title = stringResource(R.string.whitelist_availability_chart_daily_percent)) {
        if (dashboard.daily.size == 1) {
            val day = dashboard.daily.first()
            val percent = StatisticsValueFormatter.formatPercentFraction(day.availabilityPercent)
            CompactDetailRow(day.date, percent.ifBlank { stringResource(R.string.statistics_value_not_available) })
            ChartInsufficientDataMessage()
        } else if (hasMeaningfulPercentChart(dailyPercents)) {
            DailyPercentBars(
                labels = dashboard.daily.map { it.date },
                percents = dailyPercents,
                barColor = MaterialTheme.colorScheme.primary,
            )
        } else {
            ChartInsufficientDataMessage()
        }
    }
}

@Composable
private fun TransitionsSection(dashboard: WhitelistAvailabilityDashboard) {
    if (dashboard.daily.isEmpty()) return
    val transitionCounts = dashboard.daily.flatMap {
        listOf(it.becameAvailableCount, it.becameUnavailableCount)
    }
    if (!hasMeaningfulCountChart(transitionCounts)) {
        AppCard(title = stringResource(R.string.whitelist_availability_chart_transitions)) {
            ChartInsufficientDataMessage()
        }
        return
    }
    val maxTransitions = transitionCounts.maxOrNull()?.coerceAtLeast(1)?.toFloat() ?: 1f
    AppCard(title = stringResource(R.string.whitelist_availability_chart_transitions)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            dashboard.daily.forEach { day ->
                Text(day.date, style = MaterialTheme.typography.labelSmall)
                CountBarChart(
                    label = stringResource(R.string.whitelist_availability_became_available),
                    count = day.becameAvailableCount,
                    maxCount = maxTransitions,
                    barColor = MaterialTheme.colorScheme.primary,
                )
                CountBarChart(
                    label = stringResource(R.string.whitelist_availability_became_unavailable),
                    count = day.becameUnavailableCount,
                    maxCount = maxTransitions,
                    barColor = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun StableUnstableTargetsSection(dashboard: WhitelistAvailabilityDashboard) {
    val resources = LocalContext.current.resources

    if (dashboard.topStableTargets.isNotEmpty()) {
        AppCard(title = stringResource(R.string.whitelist_stable_targets_title)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                dashboard.topStableTargets.forEach { target ->
                    val label = StatisticsValueFormatter.formatTextLabel(resources, target.displayLabel)
                    val percent = StatisticsValueFormatter.formatPercentFraction(target.availabilityPercent)
                    CompactDetailRow(label, percent)
                }
            }
        }
    } else if (dashboard.targetStates.isNotEmpty()) {
        AppCard(title = stringResource(R.string.whitelist_stable_targets_title)) {
            ChartInsufficientDataMessage()
        }
    }

    if (dashboard.topUnstableTargets.isNotEmpty()) {
        AppCard(title = stringResource(R.string.whitelist_availability_chart_top_unstable)) {
            TargetUnstableBarList(targets = dashboard.topUnstableTargets)
        }
    } else if (dashboard.summary.mostUnstableTargetLabel != null) {
        AppCard(title = stringResource(R.string.whitelist_availability_chart_top_unstable)) {
            Text(
                text = stringResource(R.string.statistics_ranking_insufficient_data),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (dashboard.topAvailableTargets.isNotEmpty()) {
        AppCard(title = stringResource(R.string.whitelist_availability_chart_top_available)) {
            TargetAvailabilityBarList(targets = dashboard.topAvailableTargets)
        }
    } else if (dashboard.targetStates.isNotEmpty()) {
        AppCard(title = stringResource(R.string.whitelist_availability_chart_top_available)) {
            Text(
                text = stringResource(R.string.whitelist_all_targets_available_period),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TargetUnstableBarList(targets: List<WhitelistTargetAvailabilityStats>) {
    val resources = LocalContext.current.resources
    val scores = targets.map { it.unstableScore }
    val max = scores.maxOrNull()?.coerceAtLeast(1) ?: 1
    if (allCountValuesEqual(scores)) {
        ChartInsufficientDataMessage()
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        targets.forEach { target ->
            CountBarChart(
                label = StatisticsValueFormatter.formatTextLabel(resources, target.displayLabel),
                count = target.unstableScore,
                maxCount = max.toFloat(),
                barColor = MaterialTheme.colorScheme.tertiary,
                valueLabel = StatisticsValueFormatter.formatUnstableScore(resources, target.unstableScore),
            )
        }
    }
}

@Composable
private fun TargetAvailabilityBarList(targets: List<WhitelistTargetAvailabilityStats>) {
    val resources = LocalContext.current.resources
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        targets.forEach { target ->
            val percent = ((target.availabilityPercent ?: 0.0) * 100.0).toFloat()
            PercentBarChart(
                label = StatisticsValueFormatter.formatTextLabel(resources, target.displayLabel),
                percent = percent,
                barColor = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
