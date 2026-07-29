package com.whitelistchecker.ui.statistics

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.whitelistchecker.domain.statistics.WhitelistBinaryState
import com.whitelistchecker.domain.statistics.WhitelistTimelineBucket
import com.whitelistchecker.domain.statistics.WhitelistTimelineDashboard
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhitelistTimelineContent(
    dashboard: WhitelistTimelineDashboard,
) {
    val resources = LocalContext.current.resources
    val nowMillis = remember(dashboard.lastUpdatedAt) { System.currentTimeMillis() }
    var selectedPeriod by remember { mutableStateOf(TimelinePeriodOption.TODAY) }
    val onPercent = StatisticsValueFormatter.formatPercentFraction(dashboard.whitelistOnPercent)
        .ifBlank { "Нет данных" }
    val selectedBuckets = selectedPeriod.buckets(dashboard)

    AppCard(title = "Белые списки во времени") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactDetailRow("Сейчас", dashboard.currentState.toDisplayLabel())
            CompactDetailRow(
                "Последний бинарный статус",
                StatisticsValueFormatter.formatRelativeTime(resources, dashboard.currentStateAtMillis, nowMillis),
            )
            CompactDetailRow("БС были за период", onPercent)
            CompactDetailRow("Сэмплов ON/OFF", "${dashboard.binarySamples}/${dashboard.totalSamples}")
        }
    }

    AppCard(title = "График") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TimelinePeriodOption.entries.forEach { option ->
                    FilterChip(
                        selected = selectedPeriod == option,
                        onClick = { selectedPeriod = option },
                        label = { Text(option.label) },
                    )
                }
            }
            Text(
                text = selectedPeriod.title,
                style = MaterialTheme.typography.titleSmall,
            )
            BinaryTimelineChart(buckets = selectedBuckets)
        }
    }
}

@Composable
fun WhitelistTimelineEmptyContent() {
    AppCard(title = "Белые списки во времени") {
        Text(
            text = "Пока нет сохранённых бинарных статусов WHITELIST_ON / WHITELIST_OFF.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "Выполни несколько проверок, чтобы появился график.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun WhitelistBinaryState.toDisplayLabel(): String {
    return when (this) {
        WhitelistBinaryState.ON -> "Похоже на включённые белые списки"
        WhitelistBinaryState.OFF -> "Белые списки не обнаружены"
        WhitelistBinaryState.UNKNOWN -> "Нет бинарного статуса"
    }
}

private enum class TimelinePeriodOption(
    val label: String,
    val title: String,
) {
    TODAY("Сегодня", "Сегодня по часам"),
    DAYS("14 дней", "Последние 14 дней"),
    WEEKS("12 недель", "Последние 12 недель"),
    MONTHS("12 месяцев", "Последние 12 месяцев");

    fun buckets(dashboard: WhitelistTimelineDashboard): List<WhitelistTimelineBucket> {
        return when (this) {
            TODAY -> dashboard.todayHourly
            DAYS -> dashboard.last14Days
            WEEKS -> dashboard.last12Weeks
            MONTHS -> dashboard.last12Months
        }
    }
}
