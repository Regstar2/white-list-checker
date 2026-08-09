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

@Composable
fun StatisticsFreshnessHeader(freshness: StatisticsFreshnessUi) {
    val resources = LocalContext.current.resources
    val nowMillis = remember(freshness.dataUpdatedAt, freshness.lastCheckAt) { System.currentTimeMillis() }
    val updatedAt = StatisticsValueFormatter.formatRelativeTime(resources, freshness.dataUpdatedAt, nowMillis)
    val lastCheck = when (freshness.lastCheckStatus) {
        LastCheckTechnicalStatus.PARTIAL -> stringResource(R.string.statistics_last_check_partial_short)
        else -> StatisticsValueFormatter.formatRelativeTime(resources, freshness.lastCheckAt, nowMillis)
    }
    val freshnessText = if (freshness.isStale) {
        stringResource(R.string.statistics_freshness_stale_compact, updatedAt)
    } else {
        stringResource(R.string.statistics_freshness_compact, updatedAt, lastCheck)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = freshnessText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (freshness.isLowSample) {
            Text(
                text = stringResource(R.string.statistics_low_sample_compact_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
