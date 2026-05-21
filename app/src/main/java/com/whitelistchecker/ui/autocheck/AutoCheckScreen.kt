package com.whitelistchecker.ui.autocheck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whitelistchecker.domain.model.BackgroundCheckInterval
import com.whitelistchecker.domain.model.BackgroundCheckSettings
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow
import com.whitelistchecker.ui.components.ErrorCard
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.components.StatusChip
import com.whitelistchecker.ui.components.StatusTone
import com.whitelistchecker.ui.main.MainUiState
import com.whitelistchecker.ui.toDisplayDateTime
import com.whitelistchecker.ui.toDisplayLabel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AutoCheckScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onPresetIntervalChange: (Long) -> Unit,
    onUseCustomIntervalChange: (Boolean) -> Unit,
    onCustomIntervalInputChange: (String) -> Unit,
    onSaveAndReschedule: () -> Unit,
    onRunNow: () -> Unit,
    onStop: () -> Unit,
) {
    ScreenScaffold(title = "Автопроверка", onBack = onBack) {
        AppCard(title = "Настройки") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Автопроверка", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = uiState.backgroundCheckSettings.enabled,
                    onCheckedChange = onEnabledChange,
                )
            }
            Text(
                text = if (uiState.backgroundCheckSettings.enabled) {
                    "Android может сдвигать время запуска из-за энергосбережения."
                } else {
                    "Автопроверка выключена."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (uiState.backgroundCheckSettings.enabled) {
                Text("Интервал", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    BackgroundCheckInterval.entries.forEach { interval ->
                        FilterChip(
                            selected = !uiState.useCustomInterval &&
                                uiState.backgroundCheckSettings.intervalMinutes == interval.minutes,
                            onClick = { onPresetIntervalChange(interval.minutes) },
                            label = { Text(interval.label) },
                        )
                    }
                    FilterChip(
                        selected = uiState.useCustomInterval,
                        onClick = { onUseCustomIntervalChange(true) },
                        label = { Text("Другое") },
                    )
                }
                if (uiState.useCustomInterval) {
                    OutlinedTextField(
                        value = uiState.customIntervalInput,
                        onValueChange = onCustomIntervalInputChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Интервал, минут") },
                        singleLine = true,
                        supportingText = {
                            Text("Минимум ${BackgroundCheckSettings.MIN_INTERVAL_MINUTES} минут")
                        },
                    )
                }
                uiState.intervalError?.let { ErrorCard(it) }
            }
            Button(
                onClick = onSaveAndReschedule,
                enabled = uiState.backgroundCheckSettings.enabled && !uiState.isSavingBackgroundSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Сохранить и перепланировать")
            }
            OutlinedButton(onClick = onRunNow, modifier = Modifier.fillMaxWidth()) {
                Text("Запустить проверку сейчас")
            }
            if (uiState.backgroundCheckSettings.enabled) {
                OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                    Text("Остановить автопроверку")
                }
            }
        }

        AppCard(title = "Статус") {
            val status = uiState.backgroundCheckStatus
            CompactDetailRow("Последний запуск", status.lastRunAtMillis?.toDisplayDateTime() ?: "—")
            CompactDetailRow("Завершение", status.lastFinishedAtMillis?.toDisplayDateTime() ?: "—")
            CompactDetailRow("Статус", status.lastState.toDisplayLabel())
            val errorText = status.lastError
            if (errorText.isNullOrBlank()) {
                StatusChip(text = "Ошибок нет", tone = StatusTone.SUCCESS)
            } else {
                CompactDetailRow("Последняя ошибка", errorText)
            }
            CompactDetailRow(
                "Telegram",
                status.lastTelegramSendResult ?: "нет",
            )
            status.lastQueueFlushSummary?.let {
                CompactDetailRow("Flush очереди", it)
            }
            CompactDetailRow("Очередь", uiState.pendingReportsCount.toString())
        }
    }
}
