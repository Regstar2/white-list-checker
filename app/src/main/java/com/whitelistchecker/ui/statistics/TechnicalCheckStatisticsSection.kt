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
import com.whitelistchecker.domain.statistics.StatisticsDashboard
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow

@Composable
fun TechnicalCheckStatisticsSection(
    dashboard: StatisticsDashboard,
    onOpenDiagnostics: () -> Unit,
) {
    val resources = LocalContext.current.resources
    val nowMillis = remember(dashboard.lastUpdatedAt) { System.currentTimeMillis() }
    var expanded by remember { mutableStateOf(false) }
    val summary = dashboard.summary

    AppCard(title = stringResource(R.string.statistics_technical_checks_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.statistics_technical_checks_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(
                onClick = onOpenDiagnostics,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.statistics_open_diagnostics))
            }
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (expanded) {
                        stringResource(R.string.statistics_targets_show_less)
                    } else {
                        stringResource(R.string.statistics_technical_checks_expand)
                    },
                )
            }
            if (expanded && summary.totalRuns > 0) {
                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    CompactDetailRow(
                        stringResource(R.string.statistics_total_runs),
                        summary.totalRuns.toString(),
                    )
                    CompactDetailRow(
                        stringResource(R.string.statistics_success_runs),
                        summary.successRuns.toString(),
                    )
                    CompactDetailRow(
                        stringResource(R.string.statistics_partial_failure_runs),
                        summary.partialFailureRuns.toString(),
                    )
                    CompactDetailRow(
                        stringResource(R.string.statistics_failure_runs),
                        summary.failureRuns.toString(),
                    )
                    CompactDetailRow(
                        stringResource(R.string.statistics_fully_successful_rate),
                        StatisticsValueFormatter.formatSuccessRate(summary.successRate).ifBlank {
                            stringResource(R.string.statistics_value_not_available)
                        },
                    )
                    CompactDetailRow(
                        stringResource(R.string.statistics_average_latency),
                        StatisticsValueFormatter.formatLatency(resources, summary.averageLatencyMs),
                    )
                    CompactDetailRow(
                        stringResource(R.string.statistics_last_run),
                        StatisticsValueFormatter.formatRelativeTime(resources, summary.lastRunAt, nowMillis),
                    )
                    if (summary.consecutiveFailureCount > 0) {
                        CompactDetailRow(
                            stringResource(R.string.statistics_consecutive_full_failures),
                            summary.consecutiveFailureCount.toString(),
                        )
                    }
                }
            }
        }
    }
}
