package com.whitelistchecker.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.whitelistchecker.domain.statistics.WhitelistBinaryState
import com.whitelistchecker.domain.statistics.WhitelistTimelineBucket
import com.whitelistchecker.domain.statistics.WhitelistTimelineDashboard
import com.whitelistchecker.domain.statistics.WhitelistTimelinePeriod
import com.whitelistchecker.domain.statistics.WhitelistTimelinePeriodBucketBuilder
import com.whitelistchecker.domain.statistics.WhitelistTimelineSample
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WhitelistTimelineContent(
    dashboard: WhitelistTimelineDashboard,
) {
    val resources = LocalContext.current.resources
    val zoneId = remember { ZoneId.systemDefault() }
    val periodBucketBuilder = remember(zoneId) { WhitelistTimelinePeriodBucketBuilder(zoneId) }
    val currentDate = remember(dashboard.generatedAtMillis, zoneId) {
        Instant.ofEpochMilli(dashboard.generatedAtMillis).atZone(zoneId).toLocalDate()
    }
    val nowMillis = remember(dashboard.lastUpdatedAt) { System.currentTimeMillis() }
    var selectedPeriod by remember { mutableStateOf(TimelinePeriodOption.DAY) }
    var selectedDate by remember(dashboard.generatedAtMillis) { mutableStateOf(currentDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    val onPercent = StatisticsValueFormatter.formatPercentFraction(dashboard.whitelistOnPercent)
        .ifBlank { "Нет данных" }
    val selectedTimelinePeriod = remember(
        dashboard.samples,
        periodBucketBuilder,
        selectedPeriod,
        selectedDate,
    ) {
        selectedPeriod.build(
            builder = periodBucketBuilder,
            samples = dashboard.samples,
            anchorDate = selectedDate,
        )
    }

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
            PeriodTypeSelector(
                selectedPeriod = selectedPeriod,
                onSelectedPeriodChange = { selectedPeriod = it },
            )
            PeriodNavigator(
                selectedPeriod = selectedPeriod,
                selectedDate = selectedDate,
                currentDate = currentDate,
                selectedTimelinePeriod = selectedTimelinePeriod,
                onSelectedDateChange = { selectedDate = it },
                onPickDate = { showDatePicker = true },
            )
            BinaryTimelineChart(buckets = selectedTimelinePeriod.buckets)
        }
    }

    if (showDatePicker) {
        PeriodDatePickerDialog(
            selectedPeriod = selectedPeriod,
            selectedDate = selectedDate,
            onDateSelected = { selectedDate = it },
            onDismiss = { showDatePicker = false },
        )
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodTypeSelector(
    selectedPeriod: TimelinePeriodOption,
    onSelectedPeriodChange: (TimelinePeriodOption) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TimelinePeriodOption.entries.forEach { option ->
            FilterChip(
                selected = selectedPeriod == option,
                onClick = { onSelectedPeriodChange(option) },
                label = { Text(option.label) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeriodNavigator(
    selectedPeriod: TimelinePeriodOption,
    selectedDate: LocalDate,
    currentDate: LocalDate,
    selectedTimelinePeriod: WhitelistTimelinePeriod,
    onSelectedDateChange: (LocalDate) -> Unit,
    onPickDate: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { onSelectedDateChange(selectedPeriod.shift(selectedDate, -1)) },
                modifier = Modifier.width(52.dp),
            ) {
                Text("<")
            }
            Text(
                text = "${selectedPeriod.title}: ${selectedTimelinePeriod.title}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
            )
            OutlinedButton(
                onClick = { onSelectedDateChange(selectedPeriod.shift(selectedDate, 1)) },
                enabled = selectedPeriod.isBeforeCurrent(selectedDate, currentDate),
                modifier = Modifier.width(52.dp),
            ) {
                Text(">")
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            TextButton(onClick = onPickDate) {
                Text("Выбрать")
            }
            TextButton(
                onClick = { onSelectedDateChange(currentDate) },
                enabled = selectedPeriod.periodStart(selectedDate) != selectedPeriod.periodStart(currentDate),
            ) {
                Text("Текущий")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodDatePickerDialog(
    selectedPeriod: TimelinePeriodOption,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toUtcDateMillis(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(millis.toUtcLocalDate())
                    }
                    onDismiss()
                },
            ) {
                Text("Выбрать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    ) {
        DatePicker(
            state = pickerState,
            title = {
                Text(
                    text = selectedPeriod.pickerTitle,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
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
    val pickerTitle: String,
) {
    DAY("День", "День", "Выбрать день"),
    WEEK("Неделя", "Неделя", "Выбрать неделю"),
    MONTH("Месяц", "Месяц", "Выбрать месяц"),
    YEAR("Год", "Год", "Выбрать год");

    fun build(
        builder: WhitelistTimelinePeriodBucketBuilder,
        samples: List<WhitelistTimelineSample>,
        anchorDate: LocalDate,
    ): WhitelistTimelinePeriod {
        return when (this) {
            DAY -> builder.buildDay(samples, anchorDate)
            WEEK -> builder.buildWeek(samples, anchorDate)
            MONTH -> builder.buildMonth(samples, YearMonth.from(anchorDate))
            YEAR -> builder.buildYear(samples, anchorDate.year)
        }
    }

    fun shift(date: LocalDate, direction: Int): LocalDate {
        val steps = direction.toLong()
        return when (this) {
            DAY -> date.plusDays(steps)
            WEEK -> date.plusWeeks(steps)
            MONTH -> date.plusMonths(steps)
            YEAR -> date.plusYears(steps)
        }
    }

    fun isBeforeCurrent(date: LocalDate, currentDate: LocalDate): Boolean {
        return periodStart(date).isBefore(periodStart(currentDate))
    }

    fun periodStart(date: LocalDate): LocalDate {
        return when (this) {
            DAY -> date
            WEEK -> date.with(DayOfWeek.MONDAY)
            MONTH -> date.withDayOfMonth(1)
            YEAR -> LocalDate.of(date.year, 1, 1)
        }
    }
}

private fun LocalDate.toUtcDateMillis(): Long {
    return atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun Long.toUtcLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
}
