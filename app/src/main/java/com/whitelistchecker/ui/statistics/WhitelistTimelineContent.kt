package com.whitelistchecker.ui.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.statistics.WhitelistTimelinePeriod
import com.whitelistchecker.domain.statistics.WhitelistTimelinePeriodBucketBuilder
import com.whitelistchecker.domain.statistics.WhitelistTimelineSample
import com.whitelistchecker.ui.components.AppCard
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

@Composable
fun WhitelistTimelineContent(
    selectedPeriod: TimelinePeriodOption,
    selectedDate: LocalDate,
    currentDate: LocalDate,
    selectedTimelinePeriod: WhitelistTimelinePeriod,
    onSelectedPeriodChange: (TimelinePeriodOption) -> Unit,
    onSelectedDateChange: (LocalDate) -> Unit,
    onPickDate: () -> Unit,
    onResetCurrentPeriod: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PeriodTypeSelector(
            selectedPeriod = selectedPeriod,
            onSelectedPeriodChange = onSelectedPeriodChange,
        )
        PeriodNavigator(
            selectedPeriod = selectedPeriod,
            selectedDate = selectedDate,
            currentDate = currentDate,
            selectedTimelinePeriod = selectedTimelinePeriod,
            onSelectedDateChange = onSelectedDateChange,
            onPickDate = onPickDate,
            onResetCurrentPeriod = onResetCurrentPeriod,
        )
        AppCard(title = null) {
            BinaryTimelineChart(buckets = selectedTimelinePeriod.buckets)
        }
    }
}

@Composable
fun WhitelistTimelineEmptyContent() {
    AppCard(title = stringResource(R.string.statistics_history_title)) {
        Text(
            text = stringResource(R.string.statistics_timeline_empty_message),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.statistics_timeline_empty_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PeriodTypeSelector(
    selectedPeriod: TimelinePeriodOption,
    onSelectedPeriodChange: (TimelinePeriodOption) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        TimelinePeriodOption.entries.forEach { option ->
            FilterChip(
                selected = selectedPeriod == option,
                onClick = { onSelectedPeriodChange(option) },
                label = {
                    Text(
                        text = option.label(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PeriodNavigator(
    selectedPeriod: TimelinePeriodOption,
    selectedDate: LocalDate,
    currentDate: LocalDate,
    selectedTimelinePeriod: WhitelistTimelinePeriod,
    onSelectedDateChange: (LocalDate) -> Unit,
    onPickDate: () -> Unit,
    onResetCurrentPeriod: () -> Unit,
) {
    val previousDescription = stringResource(R.string.statistics_previous_period)
    val nextDescription = stringResource(R.string.statistics_next_period)
    val isCurrentPeriod = selectedPeriod.periodStart(selectedDate) == selectedPeriod.periodStart(currentDate)

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { onSelectedDateChange(selectedPeriod.shift(selectedDate, -1)) },
                modifier = Modifier
                    .width(52.dp)
                    .defaultMinSize(minHeight = 48.dp)
                    .semantics { contentDescription = previousDescription },
            ) {
                Text("<")
            }
            TextButton(
                onClick = onPickDate,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp),
            ) {
                Text(
                    text = selectedTimelinePeriod.title,
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            OutlinedButton(
                onClick = { onSelectedDateChange(selectedPeriod.shift(selectedDate, 1)) },
                enabled = selectedPeriod.isBeforeCurrent(selectedDate, currentDate),
                modifier = Modifier
                    .width(52.dp)
                    .defaultMinSize(minHeight = 48.dp)
                    .semantics { contentDescription = nextDescription },
            ) {
                Text(">")
            }
        }
        if (!isCurrentPeriod) {
            TextButton(
                onClick = onResetCurrentPeriod,
                modifier = Modifier.widthIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.statistics_go_to_current_period))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodDatePickerDialog(
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
                Text(stringResource(R.string.statistics_select_period_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.statistics_select_period_cancel))
            }
        },
    ) {
        DatePicker(
            state = pickerState,
            title = {
                Text(
                    text = selectedPeriod.pickerTitle(),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            },
        )
    }
}

enum class TimelinePeriodOption {
    DAY,
    WEEK,
    MONTH,
    YEAR;

    @Composable
    fun label(): String {
        return when (this) {
            DAY -> stringResource(R.string.statistics_period_day)
            WEEK -> stringResource(R.string.statistics_period_week)
            MONTH -> stringResource(R.string.statistics_period_month)
            YEAR -> stringResource(R.string.statistics_period_year)
        }
    }

    @Composable
    fun pickerTitle(): String {
        return when (this) {
            DAY -> stringResource(R.string.statistics_pick_day)
            WEEK -> stringResource(R.string.statistics_pick_week)
            MONTH -> stringResource(R.string.statistics_pick_month)
            YEAR -> stringResource(R.string.statistics_pick_year)
        }
    }

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

fun LocalDate.toUtcDateMillis(): Long {
    return atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

fun Long.toUtcLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
}
