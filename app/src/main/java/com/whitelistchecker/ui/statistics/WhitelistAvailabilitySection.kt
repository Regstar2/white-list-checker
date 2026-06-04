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
    val availabilityLabel = StatisticsValueFormatter.formatPercentFraction(summary.availabilityPercent)
        .ifBlank { stringResource(R.string.statistics_value_not_available) }

    AppCard(title = stringResource(R.string.whitelist_availability_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (dashboard.isStale) {
                StatusChip(
                    text = stringResource(R.string.statistics_data_stale),
                    tone = StatusTone.WARNING,
                )
            }
            Text(
                text = stringResource(R.string.whitelist_availability_targets_available_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            CompactDetailRow(
                stringResource(R.string.whitelist_availability_percent),
                availabilityLabel,
            )
            if (summary.totalBecameAvailableEvents > 0 || summary.totalBecameUnavailableEvents > 0) {
                CompactDetailRow(
                    stringResource(R.string.whitelist_availability_became_available),
                    summary.totalBecameAvailableEvents.toString(),
                )
                CompactDetailRow(
                    stringResource(R.string.whitelist_availability_became_unavailable),
                    summary.totalBecameUnavailableEvents.toString(),
                )
            }
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
        val dailyPercents = dashboard.daily.map { day ->
            ((day.availabilityPercent ?: 0.0) * 100.0).toFloat()
        }
        AppCard(title = stringResource(R.string.whitelist_availability_chart_daily_percent)) {
            DailyPercentBars(
                labels = dashboard.daily.map { it.date },
                percents = dailyPercents,
                barColor = MaterialTheme.colorScheme.primary,
            )
        }

        val transitionCounts = dashboard.daily.flatMap {
            listOf(it.becameAvailableCount, it.becameUnavailableCount)
        }
        if (hasMeaningfulCountChart(transitionCounts)) {
            AppCard(title = stringResource(R.string.whitelist_availability_chart_transitions)) {
                val maxTransitions = transitionCounts.maxOrNull()?.coerceAtLeast(1)?.toFloat() ?: 1f
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
    }

    if (dashboard.topUnstableTargets.isNotEmpty()) {
        AppCard(title = stringResource(R.string.whitelist_availability_chart_top_unstable)) {
            TargetUnstableBarList(targets = dashboard.topUnstableTargets)
        }
    } else if (dashboard.summary.mostUnstableTargetLabel != null) {
        AppCard(title = stringResource(R.string.whitelist_availability_chart_top_unstable)) {
            ChartInsufficientDataMessage()
        }
    }

    if (dashboard.topAvailableTargets.isNotEmpty()) {
        val percents = dashboard.topAvailableTargets.map {
            ((it.availabilityPercent ?: 0.0) * 100.0).toFloat()
        }
        AppCard(title = stringResource(R.string.whitelist_availability_chart_top_available)) {
            if (allPercentValuesAtLeast(percents, 99f)) {
                ChartInsufficientDataMessage()
            } else {
                TargetAvailabilityBarList(targets = dashboard.topAvailableTargets)
            }
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
        targets.zip(scores).forEach { (target, score) ->
            CountBarChart(
                label = StatisticsValueFormatter.formatTextLabel(resources, target.displayLabel),
                count = score,
                maxCount = max.toFloat(),
                barColor = MaterialTheme.colorScheme.tertiary,
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
