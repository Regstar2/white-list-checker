package com.whitelistchecker.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.ScreenScaffold

@Composable
fun StatisticsScreen(
    uiState: StatisticsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenDiagnostics: () -> Unit,
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
                    uiState = uiState,
                    onOpenDiagnostics = onOpenDiagnostics,
                )
            }
        }
    }
}

@Composable
private fun StatisticsContent(
    uiState: StatisticsUiState.Content,
    onOpenDiagnostics: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatisticsFreshnessHeader(freshness = uiState.freshness)

        val whitelist = uiState.whitelistAvailability
        if (uiState.whitelistAvailabilityEmpty || whitelist == null) {
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
        } else {
            WhitelistStatisticsContent(dashboard = whitelist)
        }

        TechnicalCheckStatisticsSection(
            dashboard = uiState.dashboard,
            onOpenDiagnostics = onOpenDiagnostics,
        )
    }
}
