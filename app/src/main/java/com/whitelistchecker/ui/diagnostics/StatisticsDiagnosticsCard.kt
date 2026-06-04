package com.whitelistchecker.ui.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
            AppCard(title = stringResource(R.string.statistics_diagnostics_title)) {
                Button(onClick = onLoad, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.statistics_diagnostics_load))
                }
            }
        }
        StatisticsDiagnosticsUiState.Loading -> {
            AppCard(title = stringResource(R.string.statistics_diagnostics_title)) {
                CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            }
        }
        is StatisticsDiagnosticsUiState.Error -> {
            AppCard(title = stringResource(R.string.statistics_diagnostics_title)) {
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
                Button(onClick = onLoad, modifier = Modifier.fillMaxWidth()) {
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

    AppCard(title = stringResource(R.string.statistics_diagnostics_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ConsistencyStatusRow(diagnostics)

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
                StatisticsValueFormatter.formatRelativeTime(
                    resources,
                    diagnostics.lastCheckRunAt,
                    nowMillis,
                ),
            )
            CompactDetailRow(
                stringResource(R.string.statistics_diagnostics_last_statistics_update),
                StatisticsValueFormatter.formatRelativeTime(
                    resources,
                    diagnostics.lastStatisticsUpdatedAt,
                    nowMillis,
                ),
            )
            CompactDetailRow(
                stringResource(R.string.statistics_diagnostics_last_rebuild),
                StatisticsValueFormatter.formatRelativeTime(
                    resources,
                    diagnostics.lastRebuildAt,
                    nowMillis,
                ),
            )
            CompactDetailRow(
                stringResource(R.string.statistics_diagnostics_last_cleanup),
                StatisticsValueFormatter.formatRelativeTime(
                    resources,
                    diagnostics.lastCleanupAt,
                    nowMillis,
                ),
            )

            diagnostics.consistencyReport.warnings.forEach { warning ->
                Text(
                    text = warning.toDisplayString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            when (rebuildState) {
                RebuildStatisticsUiState.Idle -> Unit
                RebuildStatisticsUiState.Running -> {
                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
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

            Button(
                onClick = { showRebuildDialog = true },
                enabled = rebuildState !is RebuildStatisticsUiState.Running,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.statistics_rebuild_action))
            }
        }
    }
}

@Composable
private fun ConsistencyStatusRow(diagnostics: StatisticsDiagnostics) {
    if (diagnostics.consistencyReport.hasWarnings) {
        StatusChip(
            text = stringResource(R.string.statistics_consistency_warnings),
            tone = StatusTone.WARNING,
        )
    } else {
        StatusChip(
            text = stringResource(R.string.statistics_consistency_ok),
            tone = StatusTone.SUCCESS,
        )
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
