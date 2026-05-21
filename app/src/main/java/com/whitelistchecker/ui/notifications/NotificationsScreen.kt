package com.whitelistchecker.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whitelistchecker.domain.model.TelegramChatCandidate
import com.whitelistchecker.domain.model.TelegramChatType
import com.whitelistchecker.domain.model.TelegramRecipient
import com.whitelistchecker.domain.model.TelegramTestResult
import com.whitelistchecker.ui.TelegramWorkerTemplate
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow
import com.whitelistchecker.ui.components.ExpandableSection
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.displayName
import com.whitelistchecker.ui.main.MainUiState
import com.whitelistchecker.ui.permissionStatusLabel
import com.whitelistchecker.ui.toResultLabel
import java.net.URI

@Composable
fun NotificationsScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onLocalEnabledChange: (Boolean) -> Unit,
    onSendLocalTest: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onTelegramEnabledChange: (Boolean) -> Unit,
    onWorkerUrlChange: (String) -> Unit,
    onRelaySecretChange: (String) -> Unit,
    onSaveTelegramSettings: () -> Unit,
    onTestWorker: () -> Unit,
    onSendTestMessage: () -> Unit,
    onSendCheckReport: () -> Unit,
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
    var showRelaySecret by rememberSaveable { mutableStateOf(false) }
    val showPermissionButton = uiState.notificationPermissionRequired &&
        !uiState.notificationsAllowed &&
        uiState.localNotificationSettings.enabled

    val workerNeedsAttention = !uiState.telegramSettings.isConfigured ||
        uiState.lastTelegramTestResult is TelegramTestResult.Failure
    val queueNeedsAttention = uiState.pendingReportsCount > 0 ||
        (uiState.lastQueueFlushResult?.failedCount ?: 0) > 0 ||
        uiState.lastQueueFlushResult?.lastError != null

    ScreenScaffold(title = "Уведомления", onBack = onBack) {
        ExpandableSection(title = "Локальные уведомления", initiallyExpanded = true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Локальные уведомления", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = uiState.localNotificationSettings.enabled,
                    onCheckedChange = onLocalEnabledChange,
                )
            }
            CompactDetailRow(
                label = "Разрешение",
                value = permissionStatusLabel(
                    permissionRequired = uiState.notificationPermissionRequired,
                    notificationsAllowed = uiState.notificationsAllowed,
                ),
            )
            Text(
                text = "Уведомления только при подтверждённом включении или выключении белых списков.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            uiState.lastLocalNotificationResult?.let { result ->
                CompactDetailRow("Последняя попытка", result.toResultLabel())
            }
            OutlinedButton(
                onClick = onSendLocalTest,
                enabled = !uiState.isSendingLocalTest && uiState.result != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Тестовое локальное уведомление")
            }
            if (uiState.isSendingLocalTest) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            }
            if (showPermissionButton) {
                Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
                    Text("Разрешить уведомления")
                }
            }
            OutlinedButton(onClick = onOpenBatterySettings, modifier = Modifier.fillMaxWidth()) {
                Text("Ограничение активности")
            }
            OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Настройки приложения")
            }
        }

        ExpandableSection(title = "Telegram Worker", initiallyExpanded = workerNeedsAttention) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Telegram-уведомления", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = uiState.telegramSettings.enabled,
                    onCheckedChange = onTelegramEnabledChange,
                )
            }

            AppCard(title = "Статус") {
                CompactDetailRow("Worker", workerStatusLabel(uiState.lastTelegramTestResult))
                CompactDetailRow(
                    "Telegram",
                    if (uiState.telegramSettings.isConfigured) "настроен" else "не настроен",
                )
                uiState.lastTelegramTestMessage?.let {
                    CompactDetailRow("Проверка", it)
                }
            }

            OutlinedTextField(
                value = uiState.telegramSettings.workerUrl,
                onValueChange = onWorkerUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Worker URL") },
                placeholder = { Text(TelegramWorkerTemplate.EXAMPLE_WORKER_URL) },
                singleLine = true,
            )
            extractHost(uiState.telegramSettings.workerUrl)?.let { host ->
                Text(
                    text = "Host: $host",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = uiState.telegramSettings.relaySecret,
                    onValueChange = onRelaySecretChange,
                    modifier = Modifier.weight(1f),
                    label = { Text("Relay Secret") },
                    singleLine = true,
                    visualTransformation = if (showRelaySecret) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                )
                TextButton(onClick = { showRelaySecret = !showRelaySecret }) {
                    Text(if (showRelaySecret) "Скрыть" else "Показать")
                }
            }

            Button(
                onClick = onTestWorker,
                enabled = !uiState.isTestingTelegram,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Проверить Worker")
            }
            if (uiState.isTestingTelegram) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            }
            OutlinedButton(onClick = onSaveTelegramSettings, modifier = Modifier.fillMaxWidth()) {
                Text("Сохранить настройки")
            }
            OutlinedButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(TelegramWorkerTemplate.WORKER_JS_CODE))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Скопировать шаблон Worker")
            }
            OutlinedButton(
                onClick = { showWorkerInstructions = !showWorkerInstructions },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (showWorkerInstructions) "Скрыть инструкцию" else "Показать инструкцию")
            }
            if (showWorkerInstructions) {
                Text(
                    text = "1. Создай Telegram-бота через BotFather.\n" +
                        "2. Создай Cloudflare Worker.\n" +
                        "3. Добавь secrets BOT_TOKEN и RELAY_SECRET.\n" +
                        "4. Введи Worker URL и Relay Secret здесь.\n" +
                        "5. Добавь получателей через /start.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ExpandableSection(title = "Получатели Telegram", initiallyExpanded = true) {
            OutlinedButton(
                onClick = onPrepareChatDiscovery,
                enabled = !uiState.telegramChatDiscovery.isPreparing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Начать получение chat_id")
            }
            OutlinedButton(
                onClick = onFindChatId,
                enabled = !uiState.telegramChatDiscovery.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Получить chat_id")
            }
            OutlinedButton(
                onClick = onFindRecentChats,
                enabled = !uiState.telegramChatDiscovery.isLoadingRecent,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Показать последние чаты")
            }
            TextButton(onClick = onResetChatDiscovery) {
                Text("Сбросить поиск chat_id")
            }
            if (uiState.telegramChatDiscovery.isPreparing ||
                uiState.telegramChatDiscovery.isLoading ||
                uiState.telegramChatDiscovery.isLoadingRecent
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            }
            uiState.telegramChatDiscovery.statusMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }
            uiState.telegramChatDiscovery.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (uiState.telegramChatDiscovery.candidates.isNotEmpty()) {
                Text("Найденные чаты", style = MaterialTheme.typography.titleSmall)
                uiState.telegramChatDiscovery.candidates.forEach { candidate ->
                    TelegramChatCandidateCard(candidate = candidate, onAdd = onAddRecipient)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text("Текущие получатели", style = MaterialTheme.typography.titleSmall)
            if (uiState.telegramSettings.recipients.isEmpty()) {
                Text("Нет Telegram-получателей", style = MaterialTheme.typography.bodySmall)
            } else {
                uiState.telegramSettings.recipients.forEach { recipient ->
                    RecipientRow(
                        recipient = recipient,
                        onToggle = onToggleRecipient,
                        onRemove = onRemoveRecipient,
                    )
                }
            }
            OutlinedButton(
                onClick = onSendTestMessage,
                enabled = !uiState.isSendingTelegramTest && !uiState.isSendingCheckReport,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Тестовое сообщение")
            }
            OutlinedButton(
                onClick = onSendCheckReport,
                enabled = !uiState.isSendingCheckReport &&
                    !uiState.isSendingTelegramTest &&
                    uiState.result != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Отчёт о последней проверке")
            }
            if (uiState.isSendingTelegramTest || uiState.isSendingCheckReport) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            }
            uiState.lastTelegramSendMessage?.let {
                CompactDetailRow("Последняя отправка", it)
            }
        }

        ExpandableSection(title = "Очередь Telegram", initiallyExpanded = queueNeedsAttention) {
            TelegramQueueSection(
                uiState = uiState,
                onRetryQueue = onRetryQueue,
                onClearQueue = onClearQueue,
            )
        }
    }
}

@Composable
private fun RecipientRow(
    recipient: TelegramRecipient,
    onToggle: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
) {
    AppCard(title = null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(recipient.displayName, style = MaterialTheme.typography.titleSmall)
                recipient.username?.let {
                    Text("@$it", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    text = "${recipient.type.toDisplayLabel()} · ${recipient.chatId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = recipient.enabled,
                onCheckedChange = { onToggle(recipient.id, it) },
            )
        }
        TextButton(onClick = { onRemove(recipient.id) }) {
            Text("Удалить")
        }
    }
}

@Composable
private fun TelegramChatCandidateCard(
    candidate: TelegramChatCandidate,
    onAdd: (TelegramChatCandidate) -> Unit,
) {
    AppCard(title = null) {
        Text(candidate.displayName(), style = MaterialTheme.typography.titleSmall)
        candidate.username?.let {
            Text("@$it", style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = "${candidate.type.toDisplayLabel()} · ${candidate.chatId}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        candidate.sourceMessageText?.let {
            Text(
                text = "Последнее: $it",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        OutlinedButton(onClick = { onAdd(candidate) }, modifier = Modifier.fillMaxWidth()) {
            Text("Добавить")
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
            Text("Telegram-уведомления выключены.", style = MaterialTheme.typography.bodySmall)
        }
        uiState.pendingReportsCount == 0 -> {
            Text("Очередь пуста", style = MaterialTheme.typography.bodyMedium)
        }
        else -> {
            CompactDetailRow("Сообщений", uiState.pendingReportsCount.toString())
            uiState.lastQueueFlushResult?.let { flushResult ->
                CompactDetailRow("Отправлено", flushResult.sentCount.toString())
                CompactDetailRow("Ошибок", flushResult.failedCount.toString())
                if (flushResult.skippedCount > 0) {
                    CompactDetailRow("Пропущено", flushResult.skippedCount.toString())
                }
                flushResult.lastError?.let { CompactDetailRow("Ошибка", it) }
            }
            OutlinedButton(
                onClick = onRetryQueue,
                enabled = !uiState.isFlushingTelegramQueue,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Повторить отправку")
            }
            OutlinedButton(
                onClick = onClearQueue,
                enabled = !uiState.isFlushingTelegramQueue,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Очистить очередь")
            }
            if (uiState.isFlushingTelegramQueue) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            }
        }
    }
}

private fun workerStatusLabel(result: TelegramTestResult?): String = when (result) {
    null -> "не проверялся"
    TelegramTestResult.Success -> "работает"
    is TelegramTestResult.Failure -> "ошибка"
}

private fun extractHost(url: String): String? {
    if (url.isBlank()) return null
    return try {
        URI(url.trim()).host
    } catch (_: Exception) {
        null
    }
}

private fun TelegramChatType.toDisplayLabel(): String = when (this) {
    TelegramChatType.PRIVATE -> "Личный чат"
    TelegramChatType.GROUP -> "Группа"
    TelegramChatType.SUPERGROUP -> "Супергруппа"
    TelegramChatType.CHANNEL -> "Канал"
    TelegramChatType.UNKNOWN -> "Неизвестно"
}
