package com.whitelistchecker.ui.notifications

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.whitelistchecker.domain.model.TelegramChatCandidate
import com.whitelistchecker.domain.model.TelegramChatType
import com.whitelistchecker.domain.model.TelegramRecipient
import com.whitelistchecker.ui.TelegramWorkerTemplate
import com.whitelistchecker.ui.components.DetailLine
import com.whitelistchecker.ui.components.InfoCard
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.configurationStatusLabel
import com.whitelistchecker.ui.displayName
import com.whitelistchecker.ui.main.MainUiState
import com.whitelistchecker.ui.permissionStatusLabel
import com.whitelistchecker.ui.toDisplayLabel
import com.whitelistchecker.ui.toLastSendStatusLabel
import com.whitelistchecker.ui.toLastTestStatusLabel

@Composable
fun NotificationsScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onLocalEnabledChange: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onTelegramEnabledChange: (Boolean) -> Unit,
    onWorkerUrlChange: (String) -> Unit,
    onRelaySecretChange: (String) -> Unit,
    onSaveTelegramSettings: () -> Unit,
    onTestWorker: () -> Unit,
    onSendTestMessage: () -> Unit,
    onPrepareChatDiscovery: () -> Unit,
    onFindChatId: () -> Unit,
    onFindRecentChats: () -> Unit,
    onResetChatDiscovery: () -> Unit,
    onAddRecipient: (TelegramChatCandidate) -> Unit,
    onRemoveRecipient: (String) -> Unit,
    onToggleRecipient: (String, Boolean) -> Unit,
    onRetryQueue: () -> Unit,
    onClearQueue: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var showWorkerInstructions by rememberSaveable { mutableStateOf(false) }
    val showPermissionButton = uiState.notificationPermissionRequired &&
        !uiState.notificationsAllowed &&
        uiState.localNotificationSettings.enabled

    ScreenScaffold(title = "Уведомления", onBack = onBack) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            InfoCard(title = "Локальные уведомления") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Локальные уведомления включены")
                    Switch(
                        checked = uiState.localNotificationSettings.enabled,
                        onCheckedChange = onLocalEnabledChange,
                    )
                }
                DetailLine(
                    "Разрешение Android",
                    permissionStatusLabel(
                        permissionRequired = uiState.notificationPermissionRequired,
                        notificationsAllowed = uiState.notificationsAllowed,
                    ),
                )
                if (showPermissionButton) {
                    Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
                        Text("Разрешить уведомления")
                    }
                }
                OutlinedButton(onClick = onOpenBatterySettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Настройки ограничения активности")
                }
                OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Настройки приложения")
                }
            }

            InfoCard(title = "Telegram Worker") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Включить Telegram-уведомления")
                    Switch(
                        checked = uiState.telegramSettings.enabled,
                        onCheckedChange = onTelegramEnabledChange,
                    )
                }
                OutlinedTextField(
                    value = uiState.telegramSettings.workerUrl,
                    onValueChange = onWorkerUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Worker URL") },
                    placeholder = { Text(TelegramWorkerTemplate.EXAMPLE_WORKER_URL) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = uiState.telegramSettings.relaySecret,
                    onValueChange = onRelaySecretChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Relay Secret") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                )
                OutlinedButton(
                    onClick = { showWorkerInstructions = !showWorkerInstructions },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (showWorkerInstructions) "Скрыть инструкцию" else "Показать инструкцию настройки Worker")
                }
                if (showWorkerInstructions) {
                    Text(
                        text = "1. Создай Telegram-бота через BotFather.\n" +
                            "2. Создай Cloudflare Worker.\n" +
                            "3. Добавь secrets BOT_TOKEN и RELAY_SECRET.\n" +
                            "4. Введи Worker URL и Relay Secret здесь.\n" +
                            "5. Добавь получателей через /start.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(TelegramWorkerTemplate.WORKER_JS_CODE))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Скопировать шаблон Worker-кода")
                }
                Button(onClick = onTestWorker, enabled = !uiState.isTestingTelegram, modifier = Modifier.fillMaxWidth()) {
                    Text("Проверить Worker")
                }
                Button(onClick = onSaveTelegramSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Сохранить настройки Telegram")
                }
                uiState.lastTelegramTestMessage?.let { DetailLine("Проверка Worker", it) }
                DetailLine("Telegram", uiState.telegramSettings.configurationStatusLabel())
                DetailLine("Последний тест", uiState.lastTelegramTestResult.toLastTestStatusLabel())
            }

            InfoCard(title = "Получатели Telegram") {
                Button(onClick = onPrepareChatDiscovery, enabled = !uiState.telegramChatDiscovery.isPreparing, modifier = Modifier.fillMaxWidth()) {
                    Text("Начать получение chat_id")
                }
                Button(onClick = onFindChatId, enabled = !uiState.telegramChatDiscovery.isLoading, modifier = Modifier.fillMaxWidth()) {
                    Text("Получить chat_id")
                }
                OutlinedButton(onClick = onFindRecentChats, enabled = !uiState.telegramChatDiscovery.isLoadingRecent, modifier = Modifier.fillMaxWidth()) {
                    Text("Показать последние чаты")
                }
                OutlinedButton(onClick = onResetChatDiscovery, modifier = Modifier.fillMaxWidth()) {
                    Text("Сбросить поиск chat_id")
                }
                if (uiState.telegramChatDiscovery.isPreparing || uiState.telegramChatDiscovery.isLoading || uiState.telegramChatDiscovery.isLoadingRecent) {
                    CircularProgressIndicator()
                }
                uiState.telegramChatDiscovery.statusMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                }
                uiState.telegramChatDiscovery.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
                if (uiState.telegramChatDiscovery.candidates.isNotEmpty()) {
                    Text("Найденные чаты", style = MaterialTheme.typography.titleSmall)
                    uiState.telegramChatDiscovery.candidates.forEach { candidate ->
                        TelegramChatCandidateCard(candidate = candidate, onAdd = onAddRecipient)
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Текущие получатели", style = MaterialTheme.typography.titleSmall)
                if (uiState.telegramSettings.recipients.isEmpty()) {
                    Text("Нет Telegram-получателей")
                } else {
                    uiState.telegramSettings.recipients.forEach { recipient ->
                        RecipientRow(
                            recipient = recipient,
                            onToggle = onToggleRecipient,
                            onRemove = onRemoveRecipient,
                        )
                    }
                }
                Button(onClick = onSendTestMessage, enabled = !uiState.isSendingTelegramTest, modifier = Modifier.fillMaxWidth()) {
                    Text("Отправить тестовое сообщение")
                }
                uiState.lastTelegramSendMessage?.let { DetailLine("Тестовая отправка", it) }
                DetailLine("Последняя отправка", uiState.lastTelegramSendResult.toLastSendStatusLabel())
            }

            InfoCard(title = "Очередь Telegram") {
                TelegramQueueSection(uiState = uiState, onRetryQueue = onRetryQueue, onClearQueue = onClearQueue)
            }

            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("← Назад")
            }
        }
    }
}

@Composable
private fun RecipientRow(
    recipient: TelegramRecipient,
    onToggle: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = recipient.enabled, onCheckedChange = { onToggle(recipient.id, it) })
                Column {
                    Text(recipient.displayName, style = MaterialTheme.typography.titleSmall)
                    recipient.username?.let { Text("@$it", style = MaterialTheme.typography.bodySmall) }
                }
            }
            DetailLine("Chat ID", recipient.chatId)
            DetailLine("Тип", recipient.type.toDisplayLabel())
            OutlinedButton(onClick = { onRemove(recipient.id) }, modifier = Modifier.fillMaxWidth()) {
                Text("Удалить")
            }
        }
    }
}

@Composable
private fun TelegramChatCandidateCard(
    candidate: TelegramChatCandidate,
    onAdd: (TelegramChatCandidate) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DetailLine("Тип", candidate.type.toDisplayLabel())
            DetailLine("Название", candidate.displayName())
            candidate.username?.let { DetailLine("Username", "@$it") }
            DetailLine("Chat ID", candidate.chatId)
            OutlinedButton(onClick = { onAdd(candidate) }, modifier = Modifier.fillMaxWidth()) {
                Text("Добавить получателя")
            }
        }
    }
}

@Composable
private fun TelegramQueueSection(
    uiState: MainUiState,
    onRetryQueue: () -> Unit,
    onClearQueue: () -> Unit,
) {
    when {
        !uiState.telegramSettings.enabled -> {
            Text("Telegram-уведомления выключены.")
        }
        uiState.pendingReportsCount == 0 -> {
            Text("Очередь Telegram пуста")
        }
        else -> {
            DetailLine("Неотправленных сообщений", uiState.pendingReportsCount.toString())
        }
    }
    uiState.lastQueueFlushResult?.let { flushResult ->
        DetailLine("Отправлено", flushResult.sentCount.toString())
        DetailLine("Ошибок", flushResult.failedCount.toString())
        if (flushResult.skippedCount > 0) DetailLine("Пропущено", flushResult.skippedCount.toString())
        flushResult.lastError?.let { DetailLine("Последняя ошибка", it) }
    }
    OutlinedButton(
        onClick = onRetryQueue,
        enabled = !uiState.isFlushingTelegramQueue && uiState.pendingReportsCount > 0,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Повторить отправку очереди")
    }
    if (uiState.pendingReportsCount > 0) {
        OutlinedButton(onClick = onClearQueue, enabled = !uiState.isFlushingTelegramQueue, modifier = Modifier.fillMaxWidth()) {
            Text("Очистить очередь")
        }
    }
    if (uiState.isFlushingTelegramQueue) CircularProgressIndicator()
}

private fun TelegramChatType.toDisplayLabel(): String = when (this) {
    TelegramChatType.PRIVATE -> "Личный чат"
    TelegramChatType.GROUP -> "Группа"
    TelegramChatType.SUPERGROUP -> "Супергруппа"
    TelegramChatType.CHANNEL -> "Канал"
    TelegramChatType.UNKNOWN -> "Неизвестно"
}
