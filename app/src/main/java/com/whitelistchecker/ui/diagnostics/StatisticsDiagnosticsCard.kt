package com.whitelistchecker.ui.diagnostics

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.statistics.StatisticsConsistencyWarningCode
import com.whitelistchecker.domain.statistics.StatisticsDiagnostics
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow
import com.whitelistchecker.ui.components.StatusChip
import com.whitelistchecker.ui.components.StatusTone
import com.whitelistchecker.ui.statistics.StatisticsValueFormatter

@Composable
fun StatisticsDiagnosticsSection(
    uiState: StatisticsDiagnosticsUiState,
    onLoad: () -> Unit,
    onRebuildConfirmed: () -> Unit,
) {
    when (uiState) {
        StatisticsDiagnosticsUiState.Idle -> {
            AppCard(title = stringResource(R.string.diagnostics_statistics)) {
                Text(
                    text = stringResource(R.string.statistics_diagnostics_not_loaded),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onLoad,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.statistics_diagnostics_load))
                }
            }
        }
        StatisticsDiagnosticsUiState.Loading -> {
            AppCard(title = stringResource(R.string.diagnostics_statistics)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                    Text(
                        text = stringResource(R.string.statistics_diagnostics_loading),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        is StatisticsDiagnosticsUiState.Error -> {
            AppCard(title = stringResource(R.string.diagnostics_statistics)) {
                Text(
                    text = stringResource(R.string.statistics_diagnostics_load_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = uiState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onLoad,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp),
                ) {
                    Text(stringResource(R.string.statistics_retry))
                }
            }
        }
        is StatisticsDiagnosticsUiState.Content -> {
            StatisticsDiagnosticsCard(
                diagnostics = uiState.diagnostics,
                rebuildState = uiState.rebuildState,
                onRebuildConfirmed = onRebuildConfirmed,
            )
        }
    }
}

@Composable
private fun StatisticsDiagnosticsCard(
    diagnostics: StatisticsDiagnostics,
    rebuildState: RebuildStatisticsUiState,
    onRebuildConfirmed: () -> Unit,
) {
    val resources = LocalContext.current.resources
    val nowMillis = remember(diagnostics.diagnosticsGeneratedAt) { System.currentTimeMillis() }
    var detailsExpanded by rememberSaveable { mutableStateOf(false) }
    var showRebuildDialog by remember { mutableStateOf(false) }

    if (showRebuildDialog) {
        AlertDialog(
            onDismissRequest = { showRebuildDialog = false },
            title = { Text(stringResource(R.string.statistics_rebuild_confirm_title)) },
            text = { Text(stringResource(R.string.statistics_rebuild_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRebuildDialog = false
                        onRebuildConfirmed()
                    },
                ) {
                    Text(stringResource(R.string.statistics_rebuild_confirm_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebuildDialog = false }) {
                    Text(stringResource(R.string.statistics_rebuild_confirm_cancel))
                }
            },
        )
    }

    AppCard(title = stringResource(R.string.diagnostics_statistics)) {
        Column(
            modifier = Modifier.animateContentSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatisticsDiagnosticsSummary(diagnostics = diagnostics, nowMillis = nowMillis)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable { detailsExpanded = !detailsExpanded },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (detailsExpanded) {
                        stringResource(R.string.statistics_diagnostics_details_hide)
                    } else {
                        stringResource(R.string.statistics_diagnostics_details_show)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = if (detailsExpanded) {
                        stringResource(R.string.statistics_diagnostics_details_hide)
                    } else {
                        stringResource(R.string.statistics_diagnostics_details_show)
                    },
                    modifier = Modifier.rotate(if (detailsExpanded) 90f else 0f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (detailsExpanded) {
                HorizontalDivider()
                StatisticsDiagnosticsDetails(
                    diagnostics = diagnostics,
                    rebuildState = rebuildState,
                    nowMillis = nowMillis,
                    onRebuildClick = { showRebuildDialog = true },
                )
            }
        }
    }
}

@Composable
private fun StatisticsDiagnosticsSummary(
    diagnostics: StatisticsDiagnostics,
    nowMillis: Long,
) {
    val resources = LocalContext.current.resources
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        StatusChip(
            text = if (diagnostics.consistencyReport.hasWarnings) {
                stringResource(R.string.statistics_consistency_warnings)
            } else {
                stringResource(R.string.statistics_consistency_ok)
            },
            tone = if (diagnostics.consistencyReport.hasWarnings) StatusTone.WARNING else StatusTone.SUCCESS,
        )
        Text(
            text = stringResource(R.string.statistics_diagnostics_check_count, diagnostics.checkRunCount),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(
                R.string.statistics_diagnostics_updated_ago,
                StatisticsValueFormatter.formatRelativeTime(
                    resources,
                    diagnostics.lastStatisticsUpdatedAt,
                    nowMillis,
                ),
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (diagnostics.consistencyReport.hasWarnings) {
            WarningBlock(warnings = diagnostics.consistencyReport.warnings)
        }
    }
}

@Composable
private fun StatisticsDiagnosticsDetails(
    diagnostics: StatisticsDiagnostics,
    rebuildState: RebuildStatisticsUiState,
    nowMillis: Long,
    onRebuildClick: () -> Unit,
) {
    val resources = LocalContext.current.resources
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CompactDetailRow(
            stringResource(R.string.statistics_diagnostics_check_runs),
            diagnostics.checkRunCount.toString(),
        )
        CompactDetailRow(
            stringResource(R.string.statistics_diagnostics_target_results),
            diagnostics.targetResultCount.toString(),
        )
        CompactDetailRow(
            stringResource(R.string.statistics_diagnostics_target_aggregates),
            diagnostics.targetStatisticsCount.toString(),
        )
        CompactDetailRow(
            stringResource(R.string.statistics_diagnostics_route_aggregates),
            diagnostics.routeKindStatisticsCount.toString(),
        )
        CompactDetailRow(
            stringResource(R.string.statistics_diagnostics_network_aggregates),
            diagnostics.networkStatisticsCount.toString(),
        )
        CompactDetailRow(
            stringResource(R.string.statistics_diagnostics_daily_aggregates),
            diagnostics.dailyStatisticsCount.toString(),
        )
        CompactDetailRow(
            stringResource(R.string.statistics_last_run),
            StatisticsValueFormatter.formatRelativeTime(resources, diagnostics.lastCheckRunAt, nowMillis),
        )
        CompactDetailRow(
            stringResource(R.string.statistics_diagnostics_last_statistics_update),
            StatisticsValueFormatter.formatRelativeTime(resources, diagnostics.lastStatisticsUpdatedAt, nowMillis),
        )
        CompactDetailRow(
            stringResource(R.string.statistics_diagnostics_last_rebuild),
            StatisticsValueFormatter.formatRelativeTime(resources, diagnostics.lastRebuildAt, nowMillis),
        )
        CompactDetailRow(
            stringResource(R.string.statistics_diagnostics_last_cleanup),
            StatisticsValueFormatter.formatRelativeTime(resources, diagnostics.lastCleanupAt, nowMillis),
        )

        if (diagnostics.consistencyReport.hasWarnings) {
            WarningBlock(warnings = diagnostics.consistencyReport.warnings)
        }

        RebuildStateContent(rebuildState = rebuildState)

        OutlinedButton(
            onClick = onRebuildClick,
            enabled = rebuildState !is RebuildStatisticsUiState.Running,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp),
        ) {
            Text(stringResource(R.string.statistics_rebuild_action))
        }
    }
}

@Composable
private fun RebuildStateContent(rebuildState: RebuildStatisticsUiState) {
    when (rebuildState) {
        RebuildStatisticsUiState.Idle -> Unit
        RebuildStatisticsUiState.Running -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                Text(
                    text = stringResource(R.string.statistics_rebuild_running),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        RebuildStatisticsUiState.Success -> {
            Text(
                text = stringResource(R.string.statistics_rebuild_success),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        is RebuildStatisticsUiState.Failure -> {
            Text(
                text = stringResource(R.string.statistics_rebuild_failure),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
            Text(
                text = rebuildState.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WarningBlock(warnings: List<StatisticsConsistencyWarningCode>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.statistics_warning_block_title),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
        )
        warnings.forEach { warning ->
            Text(
                text = warning.toDisplayString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatisticsConsistencyWarningCode.toDisplayString(): String {
    return when (this) {
        StatisticsConsistencyWarningCode.CHECK_RUNS_WITHOUT_SUMMARY ->
            stringResource(R.string.statistics_warning_check_runs_without_summary)
        StatisticsConsistencyWarningCode.TOTAL_RUNS_EXCEEDS_HISTORY ->
            stringResource(R.string.statistics_warning_total_runs_exceeds_history)
        StatisticsConsistencyWarningCode.OUTCOME_SUM_EXCEEDS_TOTAL ->
            stringResource(R.string.statistics_warning_outcome_sum_exceeds_total)
        StatisticsConsistencyWarningCode.TARGET_STATS_WITHOUT_TARGET_RESULTS ->
            stringResource(R.string.statistics_warning_target_stats_without_results)
        StatisticsConsistencyWarningCode.LAST_RUN_IN_FUTURE ->
            stringResource(R.string.statistics_warning_last_run_in_future)
        StatisticsConsistencyWarningCode.STATISTICS_UPDATED_BEFORE_LAST_RUN ->
            stringResource(R.string.statistics_warning_statistics_updated_before_last_run)
        StatisticsConsistencyWarningCode.INVALID_SUCCESS_RATE ->
            stringResource(R.string.statistics_warning_invalid_success_rate)
        StatisticsConsistencyWarningCode.INVALID_AVERAGE_LATENCY ->
            stringResource(R.string.statistics_warning_invalid_average_latency)
        StatisticsConsistencyWarningCode.NEGATIVE_CONSECUTIVE_FAILURES ->
            stringResource(R.string.statistics_warning_negative_consecutive_failures)
    }
}
