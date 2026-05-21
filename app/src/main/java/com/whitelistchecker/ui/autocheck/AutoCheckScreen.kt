package com.whitelistchecker.ui.autocheck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whitelistchecker.domain.model.BackgroundCheckInterval
import com.whitelistchecker.domain.model.BackgroundCheckSettings
import com.whitelistchecker.ui.components.DetailLine
import com.whitelistchecker.ui.components.ErrorCard
import com.whitelistchecker.ui.components.InfoCard
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.main.MainUiState
import com.whitelistchecker.ui.toDisplayDateTime
import com.whitelistchecker.ui.toDisplayLabel

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
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoCard(title = "Настройки") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Включить автопроверку")
                    Switch(
                        checked = uiState.backgroundCheckSettings.enabled,
                        onCheckedChange = onEnabledChange,
                    )
                }
                Text(
                    text = if (uiState.backgroundCheckSettings.enabled) {
                        "Автопроверка включена. Android может сдвигать время запуска из-за энергосбережения."
                    } else {
                        "Автопроверка выключена."
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                if (uiState.backgroundCheckSettings.enabled) {
                    Text("Интервал", style = MaterialTheme.typography.titleSmall)
                    BackgroundCheckInterval.entries.forEach { interval ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = !uiState.useCustomInterval &&
                                    uiState.backgroundCheckSettings.intervalMinutes == interval.minutes,
                                onClick = { onPresetIntervalChange(interval.minutes) },
                            )
                            Text(interval.label)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = uiState.useCustomInterval,
                            onClick = { onUseCustomIntervalChange(true) },
                        )
                        Text("Другое")
                    }
                    if (uiState.useCustomInterval) {
                        OutlinedTextField(
                            value = uiState.customIntervalInput,
                            onValueChange = onCustomIntervalInputChange,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Интервал (минуты)") },
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
                Button(onClick = onRunNow, modifier = Modifier.fillMaxWidth()) {
                    Text("Запустить проверку сейчас")
                }
                if (uiState.backgroundCheckSettings.enabled) {
                    OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                        Text("Остановить автопроверку")
                    }
                }
            }

            InfoCard(title = "Статус") {
                val status = uiState.backgroundCheckStatus
                DetailLine("Последний запуск", status.lastRunAtMillis?.toDisplayDateTime() ?: "—")
                DetailLine("Последнее завершение", status.lastFinishedAtMillis?.toDisplayDateTime() ?: "—")
                DetailLine("Последний статус", status.lastState.toDisplayLabel())
                DetailLine("Последняя ошибка", status.lastError ?: "нет")
                DetailLine("Последняя отправка Telegram", status.lastTelegramSendResult ?: "нет")
                status.lastQueueFlushSummary?.let { DetailLine("Последний flush очереди", it) }
                DetailLine("Очередь Telegram", "${uiState.pendingReportsCount} сообщений")
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("← Назад")
            }
        }
    }
}
