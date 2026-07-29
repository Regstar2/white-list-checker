package com.whitelistchecker.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.ui.components.CompactDetailRow
import com.whitelistchecker.ui.components.StatusChip
import com.whitelistchecker.ui.components.StatusTone

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
    }
}
