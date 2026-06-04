package com.whitelistchecker.ui.diagnostics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.SiteCheckErrorType
import com.whitelistchecker.domain.model.SiteCheckResult
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistMonitorState
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.domain.model.WhitelistStateChangeType
import com.whitelistchecker.domain.monitor.StateChangeDetector
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.components.StatusChip
import com.whitelistchecker.ui.components.StatusTone
import com.whitelistchecker.ui.main.MainUiState
import com.whitelistchecker.ui.toDescription
import com.whitelistchecker.ui.toDisplayDateTime
import com.whitelistchecker.ui.toDisplayLabel
import com.whitelistchecker.ui.toEventTitle

@Composable
fun DiagnosticsScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    detailedReport: String,
    onLoadStatisticsDiagnostics: () -> Unit,
    onRebuildStatistics: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    ScreenScaffold(title = "Диагностика", onBack = onBack) {
        uiState.result?.let { result ->
            StatusCard(result)
            SummaryCard(result)
            SitesCard(result)
        } ?: run {
            AppCard(title = "Результат проверки") {
                Text("Проверка ещё не выполнялась.", style = MaterialTheme.typography.bodyMedium)
            }
        }

        MonitoringCard(uiState.monitorState)

        uiState.lastStateChangeEvent?.let { event ->
            if (event.type != WhitelistStateChangeType.NO_CONFIRMED_CHANGE) {
                StateChangeEventCard(event)
            }
        }

        StatisticsDiagnosticsSection(
            uiState = uiState.statisticsDiagnosticsUiState,
            onLoad = onLoadStatisticsDiagnostics,
            onRebuildConfirmed = onRebuildStatistics,
        )

        Button(
            onClick = { clipboardManager.setText(AnnotatedString(detailedReport)) },
            enabled = detailedReport.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Скопировать подробный отчёт")
        }
    }
}

@Composable
private fun StatusCard(result: NetworkCheckResult) {
    AppCard(title = "Текущий результат") {
        Text(text = result.state.toDisplayLabel(), style = MaterialTheme.typography.titleMedium)
        result.state.toDescription()?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        result.diagnosticsMessage?.let { diagnostics ->
            CompactDetailRow("Диагностика TCP", diagnostics)
        }
    }
}

@Composable
private fun SummaryCard(result: NetworkCheckResult) {
    AppCard(title = "Сводка") {
        CompactDetailRow("Активная сеть", result.activeNetworkLabel)
        CompactDetailRow("Проверяемая сеть", result.checkedNetworkLabel)
        if (result.siteResults.isEmpty()) {
            Text(
                "Проверка сайтов не выполнена: мобильная сеть недоступна.",
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            GroupSummaryLine("Внешние", result.foreignSummary)
            GroupSummaryLine("Локальные", result.localSummary)
        }
        CompactDetailRow("Последняя проверка", result.checkedAtMillis.toDisplayDateTime())
    }
}

@Composable
private fun SitesCard(result: NetworkCheckResult) {
    if (result.siteResults.isEmpty()) return
    AppCard(title = "Результаты по сайтам") {
        val foreignResults = result.siteResults.filter { it.target.group == TargetGroup.FOREIGN }
        val localResults = result.siteResults.filter { it.target.group == TargetGroup.LOCAL }
        Text("Внешние сайты", style = MaterialTheme.typography.titleSmall)
        foreignResults.forEach { site ->
            CompactSiteRow(site)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text("Локальные сайты", style = MaterialTheme.typography.titleSmall)
        localResults.forEach { site ->
            CompactSiteRow(site)
        }
    }
}

@Composable
private fun CompactSiteRow(site: SiteCheckResult) {
    var expanded by rememberSaveable(site.target.name, site.target.url) { mutableStateOf(false) }
    val icon = if (site.available) "✅" else "❌"
    val summary = buildCompactSummary(site)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "$icon ${site.target.name} · $summary",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (expanded) {
            CompactDetailRow("URL", site.target.url)
            CompactDetailRow("HTTP", site.httpCode?.toString() ?: "—")
            if (site.errorType != SiteCheckErrorType.NONE) {
                ErrorTypeChip(site.errorType)
            }
            site.error?.let { CompactDetailRow("Ошибка", it) }
            CompactDetailRow("Время", "${site.durationMs} мс")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ErrorTypeChip(errorType: SiteCheckErrorType) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        StatusChip(
            text = errorType.name,
            tone = when (errorType) {
                SiteCheckErrorType.DNS,
                SiteCheckErrorType.TIMEOUT,
                SiteCheckErrorType.CONNECTION,
                -> StatusTone.WARNING
                SiteCheckErrorType.TLS,
                SiteCheckErrorType.HTTP,
                SiteCheckErrorType.UNKNOWN,
                -> StatusTone.ERROR
                SiteCheckErrorType.NONE -> StatusTone.NEUTRAL
            },
        )
    }
}

@Composable
private fun MonitoringCard(monitorState: WhitelistMonitorState?) {
    AppCard(title = "Мониторинг БС") {
        if (monitorState == null) {
            Text("Мониторинг ещё не инициализирован.", style = MaterialTheme.typography.bodySmall)
            return@AppCard
        }
        CompactDetailRow("Подтверждённое", monitorState.lastConfirmedState.toDisplayLabel())
        val pendingLabel = if (monitorState.pendingState == WhitelistState.UNKNOWN) {
            "нет"
        } else {
            monitorState.pendingState.toDisplayLabel()
        }
        CompactDetailRow("Ожидает", pendingLabel)
        val confirmationsLabel = if (monitorState.pendingState == WhitelistState.UNKNOWN) {
            "—"
        } else {
            "${monitorState.pendingStateCount}/${StateChangeDetector.REQUIRED_CONFIRMATION_COUNT}"
        }
        CompactDetailRow("Подтверждений", confirmationsLabel)
        CompactDetailRow(
            "Последнее подтверждение",
            monitorState.lastConfirmedAtMillis?.toDisplayDateTime() ?: "—",
        )
    }
}

@Composable
private fun StateChangeEventCard(event: WhitelistStateChangeEvent) {
    AppCard(title = "Событие состояния") {
        Text(text = event.type.toEventTitle(), style = MaterialTheme.typography.titleMedium)
        CompactDetailRow("Было", event.oldState.toDisplayLabel())
        CompactDetailRow("Стало", event.newState.toDisplayLabel())
        CompactDetailRow("Время", event.changedAtMillis.toDisplayDateTime())
    }
}

@Composable
private fun GroupSummaryLine(label: String, summary: TargetGroupSummary) {
    CompactDetailRow(label, "${summary.availableCount}/${summary.totalCount} доступно")
}

private fun buildCompactSummary(site: SiteCheckResult): String {
    return when {
        site.available -> "${site.httpCode ?: "—"} · ${site.durationMs} мс"
        site.errorType != SiteCheckErrorType.NONE -> "${site.errorType.name} · ${site.durationMs} мс"
        else -> "${site.httpCode ?: "—"} · ${site.durationMs} мс"
    }
}
