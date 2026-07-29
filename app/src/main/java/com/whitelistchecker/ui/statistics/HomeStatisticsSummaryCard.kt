package com.whitelistchecker.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.statistics.WhitelistBinaryState
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow
import com.whitelistchecker.ui.components.StatusChip
import com.whitelistchecker.ui.components.StatusTone

@Composable
fun HomeStatisticsSummaryCard(
    uiState: HomeStatisticsUiState,
    onOpenStatistics: () -> Unit,
) {
    when (uiState) {
        HomeStatisticsUiState.Hidden -> Unit
        HomeStatisticsUiState.Loading -> {
            AppCard(title = stringResource(R.string.statistics_title)) {
                CircularProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
        HomeStatisticsUiState.Error -> {
            AppCard(title = stringResource(R.string.statistics_title)) {
                Text(
                    text = stringResource(R.string.statistics_home_load_error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        is HomeStatisticsUiState.Content -> {
            val resources = LocalContext.current.resources
            val nowMillis = remember(uiState.lastUpdatedAt) { System.currentTimeMillis() }
            val onPercentLabel = StatisticsValueFormatter.formatPercentFraction(uiState.whitelistOnPercent)
                .ifBlank { stringResource(R.string.statistics_value_not_available) }
            val lastUpdatedLabel = StatisticsValueFormatter.formatRelativeTime(
                resources,
                uiState.lastUpdatedAt,
                nowMillis,
            )
            AppCard(title = "Статистика БС") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (uiState.isStale) {
                        StatusChip(
                            text = stringResource(R.string.statistics_data_stale),
                            tone = StatusTone.WARNING,
                        )
                    }
                    CompactDetailRow(
                        "Сейчас",
                        uiState.currentState.toShortLabel(),
                    )
                    CompactDetailRow(
                        "Последний сэмпл",
                        lastUpdatedLabel,
                    )
                    CompactDetailRow("БС были за период", onPercentLabel)
                    CompactDetailRow("Бинарных сэмплов", uiState.binarySamples.toString())
                    Button(
                        onClick = onOpenStatistics,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.statistics_open_screen))
                    }
                }
            }
        }
    }
}

private fun WhitelistBinaryState.toShortLabel(): String {
    return when (this) {
        WhitelistBinaryState.ON -> "Похоже на БС"
        WhitelistBinaryState.OFF -> "БС не обнаружены"
        WhitelistBinaryState.UNKNOWN -> "Нет бинарного статуса"
    }
}
