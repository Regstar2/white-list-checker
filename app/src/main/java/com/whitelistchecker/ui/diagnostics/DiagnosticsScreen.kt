package com.whitelistchecker.ui.diagnostics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
import com.whitelistchecker.ui.components.DetailLine
import com.whitelistchecker.ui.components.InfoCard
import com.whitelistchecker.ui.components.ScreenScaffold
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
) {
    val clipboardManager = LocalClipboardManager.current
    ScreenScaffold(title = "Диагностика", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            uiState.result?.let { result ->
                StatusCard(result)
                SummaryCard(result)
                SitesCard(result)
            } ?: run {
                InfoCard(title = "Результат проверки") {
                    Text("Проверка ещё не выполнялась.")
                }
            }

            MonitoringCard(uiState.monitorState)

            uiState.lastStateChangeEvent?.let { event ->
                if (event.type != WhitelistStateChangeType.NO_CONFIRMED_CHANGE) {
                    StateChangeEventCard(event)
                }
            }

            Button(
                onClick = { clipboardManager.setText(AnnotatedString(detailedReport)) },
                enabled = detailedReport.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Скопировать подробный отчёт")
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("← Назад")
            }
        }
    }
}

@Composable
private fun StatusCard(result: NetworkCheckResult) {
    InfoCard(title = "Текущий результат проверки") {
        Text(text = result.state.toDisplayLabel(), style = MaterialTheme.typography.titleMedium)
        result.state.toDescription()?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        result.diagnosticsMessage?.let { diagnostics ->
            DetailLine("Диагностика TCP", diagnostics)
        }
    }
}

@Composable
private fun SummaryCard(result: NetworkCheckResult) {
    InfoCard(title = "Сводка") {
        DetailLine("Активная сеть телефона", result.activeNetworkLabel)
        DetailLine("Проверяемая сеть", result.checkedNetworkLabel)
        if (result.siteResults.isEmpty()) {
            Text("Проверка сайтов не выполнена: мобильная сеть недоступна.")
        } else {
            GroupSummaryLine("Внешние сайты", result.foreignSummary)
            GroupSummaryLine("Локальные сайты", result.localSummary)
        }
        DetailLine("Последняя проверка", result.checkedAtMillis.toDisplayDateTime())
    }
}

@Composable
private fun SitesCard(result: NetworkCheckResult) {
    if (result.siteResults.isEmpty()) return
    InfoCard(title = "Результаты по сайтам") {
        val foreignResults = result.siteResults.filter { it.target.group == TargetGroup.FOREIGN }
        val localResults = result.siteResults.filter { it.target.group == TargetGroup.LOCAL }
        Text("Внешние сайты", style = MaterialTheme.typography.titleSmall)
        foreignResults.forEach { site ->
            SiteResultBlock(site)
            if (site != foreignResults.last()) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        Text("Локальные сайты", style = MaterialTheme.typography.titleSmall)
        localResults.forEach { site ->
            SiteResultBlock(site)
            if (site != localResults.last()) HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun MonitoringCard(monitorState: WhitelistMonitorState?) {
    InfoCard(title = "Мониторинг БС") {
        if (monitorState == null) {
            Text("Мониторинг ещё не инициализирован.")
            return@InfoCard
        }
        DetailLine("Последнее подтверждённое состояние", monitorState.lastConfirmedState.toDisplayLabel())
        val pendingLabel = if (monitorState.pendingState == WhitelistState.UNKNOWN) {
            "нет"
        } else {
            monitorState.pendingState.toDisplayLabel()
        }
        DetailLine("Ожидающее подтверждение", pendingLabel)
        val confirmationsLabel = if (monitorState.pendingState == WhitelistState.UNKNOWN) {
            "—"
        } else {
            "${monitorState.pendingStateCount}/${StateChangeDetector.REQUIRED_CONFIRMATION_COUNT}"
        }
        DetailLine("Подтверждений", confirmationsLabel)
        DetailLine(
            "Последнее подтверждение",
            monitorState.lastConfirmedAtMillis?.toDisplayDateTime() ?: "—",
        )
    }
}

@Composable
private fun StateChangeEventCard(event: WhitelistStateChangeEvent) {
    InfoCard(title = "Событие состояния") {
        Text(text = event.type.toEventTitle(), style = MaterialTheme.typography.titleMedium)
        DetailLine("Было", event.oldState.toDisplayLabel())
        DetailLine("Стало", event.newState.toDisplayLabel())
        DetailLine("Время", event.changedAtMillis.toDisplayDateTime())
    }
}

@Composable
private fun GroupSummaryLine(label: String, summary: TargetGroupSummary) {
    DetailLine(label, "${summary.availableCount}/${summary.totalCount} доступно")
}

@Composable
private fun SiteResultBlock(site: SiteCheckResult) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = site.target.name, style = MaterialTheme.typography.titleSmall)
        DetailLine("Группа", site.target.group.toGroupLabel())
        DetailLine("Статус", if (site.available) "доступен" else "недоступен")
        DetailLine("HTTP", site.httpCode?.toString() ?: "—")
        if (site.errorType != SiteCheckErrorType.NONE) {
            DetailLine("Тип ошибки", site.errorType.name)
        }
        DetailLine("Ошибка", site.error ?: "—")
        DetailLine("Время", "${site.durationMs} мс")
    }
}

private fun TargetGroup.toGroupLabel(): String = when (this) {
    TargetGroup.FOREIGN -> "Внешние"
    TargetGroup.LOCAL -> "Локальные"
}
