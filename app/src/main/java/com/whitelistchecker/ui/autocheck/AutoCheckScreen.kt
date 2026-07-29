package com.whitelistchecker.ui.autocheck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.whitelistchecker.domain.model.ActiveMonitoringSettings
import com.whitelistchecker.domain.model.ActiveMonitoringState
import com.whitelistchecker.domain.model.BackgroundCheckInterval
import com.whitelistchecker.domain.model.BackgroundCheckSettings
import com.whitelistchecker.domain.model.NotificationPolicy
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
    onBackgroundNotificationPolicyChange: (NotificationPolicy) -> Unit,
    onSaveAndReschedule: () -> Unit,
    onRunNow: () -> Unit,
    onStop: () -> Unit,
    onActiveIntervalInputChange: (String) -> Unit,
    onSaveActiveInterval: () -> Unit,
    onActiveNotificationPolicyChange: (NotificationPolicy) -> Unit,
    onNotifyOnAccessRestoredChange: (Boolean) -> Unit,
    onTelegramCommandsEnabledChange: (Boolean) -> Unit,
    onStartActiveMonitoring: () -> Unit,
    onStopActiveMonitoring: () -> Unit,
    onRunActiveCheckNow: () -> Unit,
) {
    ScreenScaffold(title = "Автопроверка", onBack = onBack) {
        AppCard(title = "Фоновая автопроверка") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Фоновая автопроверка", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = uiState.backgroundCheckSettings.enabled,
                    onCheckedChange = onEnabledChange,
                )
            }
            Text(
                text = if (uiState.backgroundCheckSettings.enabled) {
                    "Android может сдвигать время запуска из-за энергосбережения."
                } else {
                    "Фоновая автопроверка выключена."
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

            Text("Уведомления", style = MaterialTheme.typography.titleSmall)
            NotificationPolicyChips(
                selectedPolicy = uiState.backgroundCheckSettings.notificationPolicy,
                onPolicyChange = onBackgroundNotificationPolicyChange,
            )

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

        AppCard(title = "Активный мониторинг") {
            val activeStatus = uiState.activeMonitoringStatus
            val activeSettings = uiState.activeMonitoringSettings
            StatusChip(
                text = activeStatus.state.toDisplayText(),
                tone = activeStatus.state.toTone(),
            )
            Text(
                text = "Активный мониторинг работает только пока Android разрешает foreground service. Для постоянных фоновых проверок используется обычная автопроверка.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "На Android 15+ система может ограничивать длительность dataSync foreground service.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = uiState.activeMonitoringIntervalInput,
                onValueChange = onActiveIntervalInputChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Интервал, минут") },
                singleLine = true,
                supportingText = {
                    Text(
                        "От ${ActiveMonitoringSettings.MIN_INTERVAL_MINUTES} до " +
                            "${ActiveMonitoringSettings.MAX_INTERVAL_MINUTES} минут",
                    )
                },
            )
            uiState.activeMonitoringIntervalError?.let { ErrorCard(it) }
            OutlinedButton(onClick = onSaveActiveInterval, modifier = Modifier.fillMaxWidth()) {
                Text("Сохранить интервал")
            }

            Text("Уведомления", style = MaterialTheme.typography.titleSmall)
            NotificationPolicyChips(
                selectedPolicy = activeSettings.notificationPolicy,
                onPolicyChange = onActiveNotificationPolicyChange,
            )
            ToggleRow(
                title = "Уведомлять о восстановлении проверки",
                checked = activeSettings.notifyOnAccessRestored,
                onCheckedChange = onNotifyOnAccessRestoredChange,
            )
            ToggleRow(
                title = "Команды Telegram-бота",
                checked = activeSettings.telegramCommandsEnabled,
                onCheckedChange = onTelegramCommandsEnabledChange,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onStartActiveMonitoring,
                    enabled = activeStatus.state !in runningStates,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Запустить")
                }
                OutlinedButton(
                    onClick = onStopActiveMonitoring,
                    enabled = activeStatus.state in runningStates,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Остановить")
                }
            }
            OutlinedButton(
                onClick = onRunActiveCheckNow,
                enabled = activeStatus.state in runningStates,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Проверить сейчас")
            }
            CompactDetailRow("Последняя проверка", activeStatus.lastCheckAtMillis?.toDisplayDateTime() ?: "—")
            activeStatus.lastStopReason?.let {
                CompactDetailRow("Причина остановки", it)
            }
            activeStatus.lastError?.let {
                CompactDetailRow("Последняя ошибка", it)
            }
            activeStatus.telegramLastError?.let {
                CompactDetailRow("Telegram-команды", it)
            }
        }

        AppCard(title = "Статус фоновой автопроверки") {
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NotificationPolicyChips(
    selectedPolicy: NotificationPolicy,
    onPolicyChange: (NotificationPolicy) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        NotificationPolicy.entries.forEach { policy ->
            FilterChip(
                selected = selectedPolicy == policy,
                onClick = { onPolicyChange(policy) },
                label = { Text(policy.toDisplayText()) },
            )
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

private fun NotificationPolicy.toDisplayText(): String {
    return when (this) {
        NotificationPolicy.NONE -> "Не уведомлять"
        NotificationPolicy.EVERY_ATTEMPT -> "После каждой проверки"
        NotificationPolicy.STATE_CHANGE_ONLY -> "Только при изменении"
    }
}

private fun ActiveMonitoringState.toDisplayText(): String {
    return when (this) {
        ActiveMonitoringState.STOPPED -> "Остановлен"
        ActiveMonitoringState.STARTING -> "Запускается"
        ActiveMonitoringState.RUNNING -> "Запущен"
        ActiveMonitoringState.CHECKING -> "Выполняет проверку"
        ActiveMonitoringState.STOPPING -> "Останавливается"
        ActiveMonitoringState.STOPPED_BY_SYSTEM -> "Остановлен системой"
        ActiveMonitoringState.ERROR -> "Ошибка"
    }
}

private fun ActiveMonitoringState.toTone(): StatusTone {
    return when (this) {
        ActiveMonitoringState.RUNNING,
        ActiveMonitoringState.CHECKING,
        -> StatusTone.SUCCESS
        ActiveMonitoringState.STARTING,
        ActiveMonitoringState.STOPPING,
        ActiveMonitoringState.STOPPED_BY_SYSTEM,
        -> StatusTone.WARNING
        ActiveMonitoringState.ERROR -> StatusTone.ERROR
        ActiveMonitoringState.STOPPED -> StatusTone.NEUTRAL
    }
}

private val runningStates = setOf(
    ActiveMonitoringState.STARTING,
    ActiveMonitoringState.RUNNING,
    ActiveMonitoringState.CHECKING,
)
