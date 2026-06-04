package com.whitelistchecker.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.whitelistchecker.domain.model.statistics.DailyCheckStatistics
import com.whitelistchecker.domain.model.statistics.NetworkStatistics
import com.whitelistchecker.domain.model.statistics.RouteKindStatistics
import com.whitelistchecker.domain.model.statistics.TargetStatistics
import com.whitelistchecker.domain.statistics.StatisticsDashboard
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.components.StatusChip
import com.whitelistchecker.ui.components.StatusTone

private const val TARGETS_COLLAPSED_LIMIT = 5

@Composable
fun StatisticsScreen(
    uiState: StatisticsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.statistics_title), onBack = onBack) {
        when (uiState) {
            StatisticsUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            }
            StatisticsUiState.Empty -> {
                AppCard(title = stringResource(R.string.statistics_title)) {
                    Text(
                        text = stringResource(R.string.statistics_empty_message),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.statistics_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            is StatisticsUiState.Error -> {
                AppCard(title = stringResource(R.string.statistics_title)) {
                    Text(
                        text = stringResource(R.string.statistics_load_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.statistics_retry))
                    }
                }
            }
            is StatisticsUiState.Content -> {
                StatisticsContent(
                    dashboard = uiState.dashboard,
                    whitelistAvailability = uiState.whitelistAvailability,
                    whitelistAvailabilityEmpty = uiState.whitelistAvailabilityEmpty,
                )
            }
        }
    }
}

@Composable
private fun StatisticsContent(
    dashboard: StatisticsDashboard,
    whitelistAvailability: WhitelistAvailabilityDashboard?,
    whitelistAvailabilityEmpty: Boolean,
) {
    val resources = LocalContext.current.resources
    val nowMillis = remember(dashboard.lastUpdatedAt) { System.currentTimeMillis() }

    StatisticsFreshnessHeader(dashboard = dashboard, nowMillis = nowMillis)
    CheckStatisticsSection(dashboard = dashboard, resources = resources, nowMillis = nowMillis)

    WhitelistAvailabilitySection(
        dashboard = whitelistAvailability,
        isEmpty = whitelistAvailabilityEmpty && whitelistAvailability == null,
    )

    if (dashboard.targets.isNotEmpty()) {
        CollapsibleTargetsSection(
            targets = dashboard.targets,
            resources = resources,
            nowMillis = nowMillis,
        )
    }

    if (dashboard.routeKinds.isNotEmpty()) {
        AppCard(title = stringResource(R.string.statistics_section_route_kinds)) {
            dashboard.routeKinds.forEach { route ->
                RouteKindBlock(route, resources, nowMillis)
            }
        }
    }

    if (dashboard.networks.isNotEmpty()) {
        AppCard(title = stringResource(R.string.statistics_section_network)) {
            dashboard.networks.forEach { network ->
                NetworkBlock(network, resources, nowMillis)
            }
        }
    }

    if (dashboard.daily.isNotEmpty()) {
        AppCard(title = stringResource(R.string.statistics_section_daily)) {
            dashboard.daily.forEach { day ->
                DailyBlock(day, resources)
            }
        }
    }
}

@Composable
private fun StatisticsFreshnessHeader(dashboard: StatisticsDashboard, nowMillis: Long) {
    val resources = LocalContext.current.resources
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
        if (dashboard.isStale) {
            StatusChip(
                text = stringResource(R.string.statistics_data_stale),
                tone = StatusTone.WARNING,
            )
            Text(
                text = stringResource(R.string.statistics_stale_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (dashboard.summary.totalRuns < 3) {
            Text(
                text = stringResource(R.string.statistics_low_sample_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        CompactDetailRow(
            stringResource(R.string.statistics_updated_at),
            StatisticsValueFormatter.formatRelativeTime(resources, dashboard.lastUpdatedAt, nowMillis),
        )
    }
}

@Composable
private fun CheckStatisticsSection(
    dashboard: StatisticsDashboard,
    resources: android.content.res.Resources,
    nowMillis: Long,
) {
    val summary = dashboard.summary
    AppCard(title = stringResource(R.string.statistics_section_checks)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CompactDetailRow(
                stringResource(R.string.statistics_total_runs),
                summary.totalRuns.toString(),
            )
            CompactDetailRow(
                stringResource(R.string.statistics_success_runs),
                summary.successRuns.toString(),
            )
            if (summary.partialFailureRuns > 0) {
                CompactDetailRow(
                    stringResource(R.string.statistics_partial_failure_runs),
                    summary.partialFailureRuns.toString(),
                )
            }
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

@Composable
private fun CollapsibleTargetsSection(
    targets: List<TargetStatistics>,
    resources: android.content.res.Resources,
    nowMillis: Long,
) {
    var expanded by remember(targets.size) { mutableStateOf(false) }
    val canExpand = targets.size > TARGETS_COLLAPSED_LIMIT
    val visibleTargets = if (expanded || !canExpand) targets else targets.take(TARGETS_COLLAPSED_LIMIT)

    AppCard(title = stringResource(R.string.statistics_section_targets)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            visibleTargets.forEach { target ->
                TargetStatisticsBlock(target, resources, nowMillis)
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
private fun TargetStatisticsBlock(
    target: TargetStatistics,
    resources: android.content.res.Resources,
    nowMillis: Long,
) {
    val endpointLabel = StatisticsValueFormatter.formatEndpointLabel(
        resources,
        target.targetLabel,
        target.targetHost,
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = endpointLabel, style = MaterialTheme.typography.titleSmall)
        CompactDetailRow(
            stringResource(R.string.statistics_total_checks),
            target.totalChecks.toString(),
        )
        CompactDetailRow(
            stringResource(R.string.statistics_target_success_rate),
            StatisticsValueFormatter.formatSuccessRate(target.successRate).ifBlank {
                stringResource(R.string.statistics_value_not_available)
            },
        )
        CompactDetailRow(
            stringResource(R.string.statistics_average_latency),
            StatisticsValueFormatter.formatLatency(resources, target.averageLatencyMs),
        )
        CompactDetailRow(
            stringResource(R.string.statistics_last_check),
            StatisticsValueFormatter.formatRelativeTime(resources, target.lastCheckedAt, nowMillis),
        )
        CompactDetailRow(
            stringResource(R.string.statistics_last_status),
            targetLastStatusLabel(target),
        )
    }
}

@Composable
private fun targetLastStatusLabel(target: TargetStatistics): String {
    val lastSuccess = target.lastSuccessAt
    val lastFailure = target.lastFailureAt
    return when {
        lastSuccess != null && (lastFailure == null || lastSuccess >= lastFailure) -> {
            stringResource(R.string.statistics_status_success)
        }
        lastFailure != null -> stringResource(R.string.statistics_status_failure)
        else -> stringResource(R.string.statistics_value_not_available)
    }
}

@Composable
private fun RouteKindBlock(
    route: RouteKindStatistics,
    resources: android.content.res.Resources,
    nowMillis: Long,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = StatisticsValueFormatter.formatTextLabel(resources, route.routeKind),
            style = MaterialTheme.typography.titleSmall,
        )
        CompactDetailRow(stringResource(R.string.statistics_total_checks), route.totalChecks.toString())
        CompactDetailRow(
            stringResource(R.string.statistics_target_success_rate),
            StatisticsValueFormatter.formatSuccessRate(route.successRate).ifBlank {
                stringResource(R.string.statistics_value_not_available)
            },
        )
        CompactDetailRow(
            stringResource(R.string.statistics_average_latency),
            StatisticsValueFormatter.formatLatency(resources, route.averageLatencyMs),
        )
        CompactDetailRow(
            stringResource(R.string.statistics_last_check),
            StatisticsValueFormatter.formatRelativeTime(resources, route.lastCheckedAt, nowMillis),
        )
    }
}

@Composable
private fun NetworkBlock(
    network: NetworkStatistics,
    resources: android.content.res.Resources,
    nowMillis: Long,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = StatisticsValueFormatter.formatTextLabel(resources, network.networkType),
            style = MaterialTheme.typography.titleSmall,
        )
        CompactDetailRow(
            stringResource(R.string.statistics_operator),
            network.operatorName?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.statistics_operator_unknown),
        )
        CompactDetailRow(stringResource(R.string.statistics_total_runs), network.totalRuns.toString())
        CompactDetailRow(
            stringResource(R.string.statistics_fully_successful_rate),
            StatisticsValueFormatter.formatSuccessRate(network.successRate).ifBlank {
                stringResource(R.string.statistics_value_not_available)
            },
        )
        CompactDetailRow(
            stringResource(R.string.statistics_average_latency),
            StatisticsValueFormatter.formatLatency(resources, network.averageLatencyMs),
        )
        CompactDetailRow(
            stringResource(R.string.statistics_last_run),
            StatisticsValueFormatter.formatRelativeTime(resources, network.lastRunAt, nowMillis),
        )
    }
}

@Composable
private fun DailyBlock(day: DailyCheckStatistics, resources: android.content.res.Resources) {
    val successRate = if (day.totalRuns > 0) {
        day.successRuns.toDouble() / day.totalRuns.toDouble()
    } else {
        null
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = day.date, style = MaterialTheme.typography.titleSmall)
        CompactDetailRow(stringResource(R.string.statistics_total_runs), day.totalRuns.toString())
        CompactDetailRow(stringResource(R.string.statistics_success_runs), day.successRuns.toString())
        CompactDetailRow(stringResource(R.string.statistics_failure_runs), day.failureRuns.toString())
        CompactDetailRow(
            stringResource(R.string.statistics_fully_successful_rate),
            StatisticsValueFormatter.formatSuccessRate(successRate).ifBlank {
                stringResource(R.string.statistics_value_not_available)
            },
        )
        CompactDetailRow(
            stringResource(R.string.statistics_average_latency),
            StatisticsValueFormatter.formatLatency(resources, day.averageLatencyMs),
        )
    }
}
