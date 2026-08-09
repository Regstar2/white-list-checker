package com.whitelistchecker.ui.statistics

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.statistics.StatisticsExportBuilder
import com.whitelistchecker.domain.statistics.StatisticsExportDocument
import com.whitelistchecker.domain.statistics.StatisticsExportFormat
import com.whitelistchecker.domain.statistics.StatisticsExportFreshness
import com.whitelistchecker.domain.statistics.StatisticsExportLabels
import com.whitelistchecker.domain.statistics.StatisticsExportPeriodKind
import com.whitelistchecker.domain.statistics.StatisticsExportRequest
import com.whitelistchecker.domain.statistics.StatisticsExportScope
import com.whitelistchecker.domain.statistics.WhitelistBinaryState
import com.whitelistchecker.domain.statistics.WhitelistTimelineDashboard
import com.whitelistchecker.domain.statistics.WhitelistTimelinePeriod
import com.whitelistchecker.domain.statistics.WhitelistTimelinePeriodBucketBuilder
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.ScreenScaffold
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatisticsContent(
    uiState: StatisticsUiState.Content,
    onOpenDiagnostics: () -> Unit,
) {
    val context = LocalContext.current
    val resources = context.resources
    val zoneId = remember { ZoneId.systemDefault() }
    val exportBuilder = remember(zoneId) { StatisticsExportBuilder(zoneId) }
    val exportLabels = statisticsExportLabels()
    val timeline = uiState.whitelistTimeline
    val currentDate = remember(timeline?.generatedAtMillis, zoneId) {
        timeline?.generatedAtMillis?.toLocalDate(zoneId) ?: LocalDate.now(zoneId)
    }
    val periodBucketBuilder = remember(zoneId) { WhitelistTimelinePeriodBucketBuilder(zoneId) }
    var selectedPeriod by remember { mutableStateOf(TimelinePeriodOption.DAY) }
    var selectedDate by remember(timeline?.generatedAtMillis) { mutableStateOf(currentDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var exportFormat by remember { mutableStateOf(StatisticsExportFormat.CSV) }
    var exportScopeChoice by remember { mutableStateOf(ExportScopeChoice.SELECTED_PERIOD) }
    var pendingDocument by remember { mutableStateOf<StatisticsExportDocument?>(null) }

    val selectedTimelinePeriod = remember(timeline?.samples, selectedPeriod, selectedDate, periodBucketBuilder) {
        timeline?.let {
            selectedPeriod.build(
                builder = periodBucketBuilder,
                samples = it.samples,
                anchorDate = selectedDate,
            )
        }
    }

    fun handleExportResult(uri: android.net.Uri?) {
        val document = pendingDocument
        pendingDocument = null
        if (uri == null || document == null) return
        val saved = StatisticsExportFileWriter.write(context.contentResolver, uri, document)
        Toast.makeText(
            context,
            if (saved) {
                resources.getString(R.string.statistics_export_success)
            } else {
                resources.getString(R.string.statistics_export_error)
            },
            Toast.LENGTH_SHORT,
        ).show()
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
        onResult = ::handleExportResult,
    )
    val jsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = ::handleExportResult,
    )
    val txtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = ::handleExportResult,
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatisticsFreshnessHeader(freshness = uiState.freshness)

        if (uiState.whitelistTimelineEmpty || timeline == null || selectedTimelinePeriod == null) {
            WhitelistTimelineEmptyContent()
            TechnicalStatisticsNavigationCard(onOpenDiagnostics = onOpenDiagnostics)
            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.statistics_export_button))
            }
        } else {
            WhitelistStatusHeroCard(
                dashboard = timeline,
                selectedTimelinePeriod = selectedTimelinePeriod,
                freshness = uiState.freshness,
            )
            KeyMetricsRow(
                dashboard = timeline,
                freshness = uiState.freshness,
            )
            Text(
                text = stringResource(R.string.statistics_history_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
            WhitelistTimelineContent(
                selectedPeriod = selectedPeriod,
                selectedDate = selectedDate,
                currentDate = currentDate,
                selectedTimelinePeriod = selectedTimelinePeriod,
                onSelectedPeriodChange = { option -> selectedPeriod = option },
                onSelectedDateChange = { date ->
                    if (!selectedPeriod.periodStart(date).isAfter(selectedPeriod.periodStart(currentDate))) {
                        selectedDate = date
                    }
                },
                onPickDate = { showDatePicker = true },
                onResetCurrentPeriod = { selectedDate = currentDate },
            )
            TechnicalStatisticsNavigationCard(onOpenDiagnostics = onOpenDiagnostics)
            OutlinedButton(
                onClick = { showExportSheet = true },
                enabled = timeline.samples.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp),
            ) {
                Text(stringResource(R.string.statistics_export_button))
            }
        }
    }

    if (showDatePicker) {
        PeriodDatePickerDialog(
            selectedPeriod = selectedPeriod,
            selectedDate = selectedDate,
            onDateSelected = { date ->
                if (!selectedPeriod.periodStart(date).isAfter(selectedPeriod.periodStart(currentDate))) {
                    selectedDate = date
                }
            },
            onDismiss = { showDatePicker = false },
        )
    }

    if (showExportSheet && timeline != null && selectedTimelinePeriod != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showExportSheet = false },
            sheetState = sheetState,
        ) {
            StatisticsExportBottomSheet(
                selectedFormat = exportFormat,
                selectedScope = exportScopeChoice,
                selectedPeriodTitle = selectedTimelinePeriod.title,
                onFormatChange = { exportFormat = it },
                onScopeChange = { exportScopeChoice = it },
                onExport = {
                    val scope = when (exportScopeChoice) {
                        ExportScopeChoice.SELECTED_PERIOD -> StatisticsExportScope.SelectedPeriod(
                            kind = selectedPeriod.toExportKind(),
                            title = selectedTimelinePeriod.title,
                            startMillis = selectedTimelinePeriod.startMillis,
                            endMillis = selectedTimelinePeriod.endMillis,
                            buckets = selectedTimelinePeriod.buckets,
                        )
                        ExportScopeChoice.ALL_HISTORY -> StatisticsExportScope.AllHistory
                    }
                    val document = exportBuilder.build(
                        StatisticsExportRequest(
                            format = exportFormat,
                            scope = scope,
                            dashboard = timeline,
                            checkStatistics = uiState.dashboard,
                            freshness = uiState.freshness.toExportFreshness(resources),
                            generatedAtMillis = System.currentTimeMillis(),
                            labels = exportLabels,
                        ),
                    )
                    if (document == null) {
                        Toast.makeText(
                            context,
                            resources.getString(R.string.statistics_export_no_data),
                            Toast.LENGTH_SHORT,
                        ).show()
                        return@StatisticsExportBottomSheet
                    }
                    pendingDocument = document
                    showExportSheet = false
                    when (exportFormat) {
                        StatisticsExportFormat.CSV -> csvLauncher.launch(document.fileName)
                        StatisticsExportFormat.JSON -> jsonLauncher.launch(document.fileName)
                        StatisticsExportFormat.TXT -> txtLauncher.launch(document.fileName)
                    }
                },
            )
        }
    }
}

@Composable
private fun WhitelistStatusHeroCard(
    dashboard: WhitelistTimelineDashboard,
    selectedTimelinePeriod: WhitelistTimelinePeriod,
    freshness: StatisticsFreshnessUi,
) {
    val resources = LocalContext.current.resources
    val nowMillis = remember(dashboard.lastUpdatedAt) { System.currentTimeMillis() }
    val accentColor = dashboard.currentState.accentColor()
    val periodBinarySamples = selectedTimelinePeriod.buckets.sumOf { it.whitelistOnCount + it.whitelistOffCount }
    val periodOnSamples = selectedTimelinePeriod.buckets.sumOf { it.whitelistOnCount }
    val periodPercent = if (periodBinarySamples > 0) {
        periodOnSamples.toDouble() / periodBinarySamples.toDouble()
    } else {
        null
    }
    AppCard(
        title = null,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.statistics_current_status_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = dashboard.currentState.statusLabel(),
                    style = MaterialTheme.typography.titleLarge,
                    color = accentColor,
                    modifier = Modifier.weight(1f),
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = StatisticsValueFormatter.formatPercentFraction(periodPercent)
                            .ifBlank { stringResource(R.string.statistics_dash_value) },
                        style = MaterialTheme.typography.headlineSmall,
                        color = accentColor,
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(R.string.statistics_whitelist_on_period_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.statistics_last_binary_status),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = StatisticsValueFormatter.formatRelativeTime(
                        resources,
                        dashboard.currentStateAtMillis,
                        nowMillis,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (freshness.isLowSample) {
                Text(
                    text = stringResource(R.string.statistics_low_sample_compact_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun KeyMetricsRow(
    dashboard: WhitelistTimelineDashboard,
    freshness: StatisticsFreshnessUi,
) {
    val resources = LocalContext.current.resources
    val nowMillis = remember(freshness.lastCheckAt) { System.currentTimeMillis() }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MetricCard(
            label = stringResource(R.string.statistics_binary_samples),
            value = "${dashboard.binarySamples} / ${dashboard.totalSamples}",
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            label = stringResource(R.string.statistics_last_check),
            value = StatisticsValueFormatter.formatRelativeTime(resources, freshness.lastCheckAt, nowMillis),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    AppCard(title = null, modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TechnicalStatisticsNavigationCard(onOpenDiagnostics: () -> Unit) {
    AppCard(title = null) {
        TextButton(
            onClick = onOpenDiagnostics,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.statistics_technical_checks_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.statistics_technical_checks_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(">", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun StatisticsExportBottomSheet(
    selectedFormat: StatisticsExportFormat,
    selectedScope: ExportScopeChoice,
    selectedPeriodTitle: String,
    onFormatChange: (StatisticsExportFormat) -> Unit,
    onScopeChange: (ExportScopeChoice) -> Unit,
    onExport: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.statistics_export_title),
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = stringResource(R.string.statistics_export_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = stringResource(R.string.statistics_export_format), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatisticsExportFormat.entries.forEach { format ->
                FilterChip(
                    selected = selectedFormat == format,
                    onClick = { onFormatChange(format) },
                    label = { Text(format.displayLabel()) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(text = stringResource(R.string.statistics_export_period), style = MaterialTheme.typography.titleSmall)
        ExportScopeRow(
            selected = selectedScope == ExportScopeChoice.SELECTED_PERIOD,
            title = stringResource(R.string.statistics_export_selected_period),
            subtitle = selectedPeriodTitle,
            onClick = { onScopeChange(ExportScopeChoice.SELECTED_PERIOD) },
        )
        ExportScopeRow(
            selected = selectedScope == ExportScopeChoice.ALL_HISTORY,
            title = stringResource(R.string.statistics_export_all_history),
            subtitle = stringResource(R.string.statistics_export_all_history_hint),
            onClick = { onScopeChange(ExportScopeChoice.ALL_HISTORY) },
        )
        Button(
            onClick = onExport,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp),
        ) {
            Text(stringResource(R.string.statistics_export_confirm))
        }
    }
}

@Composable
private fun ExportScopeRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private enum class ExportScopeChoice {
    SELECTED_PERIOD,
    ALL_HISTORY,
}

@Composable
private fun StatisticsExportFormat.displayLabel(): String {
    return when (this) {
        StatisticsExportFormat.CSV -> stringResource(R.string.statistics_export_format_csv)
        StatisticsExportFormat.JSON -> stringResource(R.string.statistics_export_format_json)
        StatisticsExportFormat.TXT -> stringResource(R.string.statistics_export_format_txt)
    }
}

@Composable
private fun WhitelistBinaryState.statusLabel(): String {
    return when (this) {
        WhitelistBinaryState.ON -> stringResource(R.string.statistics_status_whitelist_on)
        WhitelistBinaryState.OFF -> stringResource(R.string.statistics_status_whitelist_off)
        WhitelistBinaryState.UNKNOWN -> stringResource(R.string.statistics_status_unknown_binary)
    }
}

@Composable
private fun WhitelistBinaryState.accentColor(): Color {
    return when (this) {
        WhitelistBinaryState.ON -> MaterialTheme.colorScheme.tertiary
        WhitelistBinaryState.OFF -> MaterialTheme.colorScheme.primary
        WhitelistBinaryState.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

private fun StatisticsFreshnessUi.toExportFreshness(resources: android.content.res.Resources): StatisticsExportFreshness {
    return StatisticsExportFreshness(
        dataUpdatedAt = dataUpdatedAt,
        isStale = isStale,
        isLowSample = isLowSample,
        lastCheckAt = lastCheckAt,
        lastCheckStatus = StatisticsValueFormatter.formatTechnicalCheckStatus(resources, lastCheckStatus),
        targetsCheckedAvailable = targetsCheckedAvailable,
        targetsCheckedTotal = targetsCheckedTotal,
    )
}

@Composable
private fun statisticsExportLabels(): StatisticsExportLabels {
    return StatisticsExportLabels(
        appTitle = stringResource(R.string.statistics_export_txt_title),
        selectedPeriodScope = stringResource(R.string.statistics_export_selected_period),
        allHistoryScope = stringResource(R.string.statistics_export_all_history),
        allHistoryFilePart = stringResource(R.string.statistics_export_file_all),
        currentState = stringResource(R.string.statistics_export_current_state),
        currentStateAt = stringResource(R.string.statistics_last_binary_status),
        whitelistOnPercent = stringResource(R.string.statistics_whitelist_on_period_label),
        binarySamples = stringResource(R.string.statistics_binary_samples),
        lastCheck = stringResource(R.string.statistics_last_check),
        history = stringResource(R.string.statistics_history_title),
        noData = stringResource(R.string.statistics_value_not_available),
        statusOn = stringResource(R.string.statistics_status_whitelist_on),
        statusOff = stringResource(R.string.statistics_status_whitelist_off),
        statusUnknown = stringResource(R.string.statistics_status_unknown_binary),
        scopeDayFilePart = stringResource(R.string.statistics_export_file_day),
        scopeWeekFilePart = stringResource(R.string.statistics_export_file_week),
        scopeMonthFilePart = stringResource(R.string.statistics_export_file_month),
        scopeYearFilePart = stringResource(R.string.statistics_export_file_year),
    )
}

private fun TimelinePeriodOption.toExportKind(): StatisticsExportPeriodKind {
    return when (this) {
        TimelinePeriodOption.DAY -> StatisticsExportPeriodKind.DAY
        TimelinePeriodOption.WEEK -> StatisticsExportPeriodKind.WEEK
        TimelinePeriodOption.MONTH -> StatisticsExportPeriodKind.MONTH
        TimelinePeriodOption.YEAR -> StatisticsExportPeriodKind.YEAR
    }
}

private fun Long.toLocalDate(zoneId: ZoneId): LocalDate {
    return Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
}
