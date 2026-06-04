package com.whitelistchecker.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.availability.WhitelistAvailabilityDashboard
import com.whitelistchecker.domain.model.availability.WhitelistTargetAvailabilityStats
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow
import com.whitelistchecker.ui.components.StatusChip
import com.whitelistchecker.ui.components.StatusTone

@Composable
fun WhitelistAvailabilitySection(
    dashboard: WhitelistAvailabilityDashboard?,
    isEmpty: Boolean,
) {
    if (isEmpty || dashboard == null) {
        AppCard(title = stringResource(R.string.whitelist_availability_title)) {
            Text(
                text = stringResource(R.string.whitelist_availability_empty_message),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.whitelist_availability_empty_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val resources = LocalContext.current.resources
    val nowMillis = System.currentTimeMillis()
    val summary = dashboard.summary
    val availabilityLabel = StatisticsValueFormatter.formatSuccessRate(summary.availabilityPercent).ifBlank {
        stringResource(R.string.statistics_value_not_available)
    }

    AppCard(title = stringResource(R.string.whitelist_availability_title)) {
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
            CompactDetailRow(
                stringResource(R.string.whitelist_availability_current_unknown),
                summary.unknownTargets.toString(),
            )
            CompactDetailRow(
                stringResource(R.string.whitelist_availability_percent),
                availabilityLabel,
            )
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
            summary.mostStableTargetLabel?.let { label ->
                CompactDetailRow(
                    stringResource(R.string.whitelist_availability_most_stable),
                    StatisticsValueFormatter.formatTextLabel(resources, label),
                )
            }
            summary.mostUnstableTargetLabel?.let { label ->
                CompactDetailRow(
                    stringResource(R.string.whitelist_availability_most_unstable),
                    StatisticsValueFormatter.formatTextLabel(resources, label),
                )
            }
        }
    }

    if (dashboard.daily.isNotEmpty()) {
        AppCard(title = stringResource(R.string.whitelist_availability_chart_daily_percent)) {
            DailyPercentBars(
                labels = dashboard.daily.map { it.date },
                values = dashboard.daily.map { day ->
                    ((day.availabilityPercent ?: 0.0) * 100.0).toFloat()
                },
                barColor = MaterialTheme.colorScheme.primary,
            )
        }
        AppCard(title = stringResource(R.string.whitelist_availability_chart_transitions)) {
            val maxTransitions = dashboard.daily.maxOf {
                maxOf(it.becameAvailableCount, it.becameUnavailableCount)
            }.coerceAtLeast(1).toFloat()
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dashboard.daily.forEach { day ->
                    Text(day.date, style = MaterialTheme.typography.labelSmall)
                    HorizontalBarChart(
                        label = stringResource(R.string.whitelist_availability_became_available),
                        value = day.becameAvailableCount.toFloat(),
                        maxValue = maxTransitions,
                        barColor = MaterialTheme.colorScheme.primary,
                    )
                    HorizontalBarChart(
                        label = stringResource(R.string.whitelist_availability_became_unavailable),
                        value = day.becameUnavailableCount.toFloat(),
                        maxValue = maxTransitions,
                        barColor = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (dashboard.topUnstableTargets.isNotEmpty()) {
        AppCard(title = stringResource(R.string.whitelist_availability_chart_top_unstable)) {
            TargetBarList(
                targets = dashboard.topUnstableTargets,
                valueSelector = { it.unstableScore.toFloat() },
            )
        }
    }

    if (dashboard.topAvailableTargets.isNotEmpty()) {
        AppCard(title = stringResource(R.string.whitelist_availability_chart_top_available)) {
            TargetBarList(
                targets = dashboard.topAvailableTargets,
                valueSelector = { ((it.availabilityPercent ?: 0.0) * 100.0).toFloat() },
            )
        }
    }
}

@Composable
private fun TargetBarList(
    targets: List<WhitelistTargetAvailabilityStats>,
    valueSelector: (WhitelistTargetAvailabilityStats) -> Float,
) {
    val resources = LocalContext.current.resources
    val values = targets.map(valueSelector)
    val max = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        targets.zip(values).forEach { (target, value) ->
            HorizontalBarChart(
                label = StatisticsValueFormatter.formatTextLabel(resources, target.displayLabel),
                value = value,
                maxValue = max,
                barColor = MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}
