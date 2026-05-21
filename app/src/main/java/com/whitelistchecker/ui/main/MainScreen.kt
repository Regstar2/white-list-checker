package com.whitelistchecker.ui.main

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.TelegramChatCandidate
import com.whitelistchecker.domain.model.SiteCheckErrorType
import com.whitelistchecker.domain.model.SiteCheckResult
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistMonitorState
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.domain.model.WhitelistStateChangeType
import com.whitelistchecker.domain.monitor.StateChangeDetector
import com.whitelistchecker.ui.TelegramWorkerTemplate
import com.whitelistchecker.ui.configurationStatusLabel
import com.whitelistchecker.ui.permissionStatusLabel
import com.whitelistchecker.ui.toDisplayDateTime
import com.whitelistchecker.ui.toDescription
import com.whitelistchecker.ui.toDisplayLabel
import com.whitelistchecker.ui.toEventTitle
import com.whitelistchecker.ui.displayName
import com.whitelistchecker.ui.toLastSendStatusLabel
import com.whitelistchecker.ui.toLastTestStatusLabel
import com.whitelistchecker.ui.toResultLabel

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.refreshNotificationPermissionState()
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Whitelist Checker",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Проверка выполняется через мобильную сеть, даже если телефон подключён к Wi-Fi.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                onClick = viewModel::checkMobileNetwork,
                enabled = !uiState.isChecking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Проверить мобильную сеть")
            }

            if (uiState.isChecking) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 4.dp))
            }

            LocalNotificationsCard(
                uiState = uiState,
                onEnabledChange = viewModel::updateLocalNotificationsEnabled,
                onRequestPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onOpenBatterySettings = viewModel::openBatteryOptimizationSettings,
                onOpenAppSettings = viewModel::openAppDetailsSettings,
            )

            TelegramCard(
                uiState = uiState,
                onEnabledChange = viewModel::updateTelegramEnabled,
                onWorkerUrlChange = viewModel::updateTelegramWorkerUrl,
                onRelaySecretChange = viewModel::updateTelegramRelaySecret,
                onChatIdChange = viewModel::updateTelegramChatId,
                onSaveSettings = viewModel::saveTelegramSettings,
                onTestWorker = viewModel::testTelegramWorker,
                onSendTestMessage = viewModel::sendTelegramTestMessage,
                onPrepareChatDiscovery = viewModel::prepareTelegramChatDiscovery,
                onFindChatId = viewModel::findTelegramChatId,
                onFindRecentChats = viewModel::findRecentTelegramChats,
                onResetChatDiscovery = viewModel::resetTelegramChatDiscovery,
                onUseChat = viewModel::useTelegramChat,
                onRetryQueue = viewModel::retryPendingTelegramReports,
                onClearQueue = viewModel::clearPendingTelegramReports,
            )

            MonitoringCard(uiState.monitorState)

            uiState.lastStateChangeEvent?.let { event ->
                if (event.type != WhitelistStateChangeType.NO_CONFIRMED_CHANGE) {
                    StateChangeEventCard(event)
                }
            }

            uiState.result?.let { result ->
                StatusCard(result)
                SummaryCard(result)
                SitesCard(result)
            }

            uiState.errorMessage?.let { message ->
                ErrorCard(message)
            }

            if (uiState.result?.state == WhitelistState.CELLULAR_NETWORK_UNAVAILABLE) {
                uiState.result?.error?.let { message ->
                    ErrorCard(message)
                }
            }
        }
    }
}

@Composable
private fun LocalNotificationsCard(
    uiState: MainUiState,
    onEnabledChange: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val showPermissionButton = uiState.notificationPermissionRequired &&
        !uiState.notificationsAllowed &&
        uiState.localNotificationSettings.enabled

    InfoCard(title = "Локальные уведомления") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Локальные уведомления включены",
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = uiState.localNotificationSettings.enabled,
                onCheckedChange = onEnabledChange,
            )
        }
        Text(
            text = "При каждой проверке отправляется тестовое уведомление (если включено и есть разрешение). События БС — только при подтверждённом включении или выключении.",
            style = MaterialTheme.typography.bodySmall,
        )
        DetailLine(
            "Разрешение Android",
            permissionStatusLabel(
                permissionRequired = uiState.notificationPermissionRequired,
                notificationsAllowed = uiState.notificationsAllowed,
            ),
        )
        val lastAttemptLabel = uiState.lastLocalNotificationResult?.let { result ->
            result.toResultLabel()
        } ?: "Локальные уведомления ещё не отправлялись"
        DetailLine("Последняя попытка", lastAttemptLabel)

        if (showPermissionButton) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Разрешить уведомления")
            }
        }
        OutlinedButton(
            onClick = onOpenBatterySettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Настройки ограничения активности")
        }
        OutlinedButton(
            onClick = onOpenAppSettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Настройки приложения")
        }
    }
}

@Composable
private fun TelegramCard(
    uiState: MainUiState,
    onEnabledChange: (Boolean) -> Unit,
    onWorkerUrlChange: (String) -> Unit,
    onRelaySecretChange: (String) -> Unit,
    onChatIdChange: (String) -> Unit,
    onSaveSettings: () -> Unit,
    onTestWorker: () -> Unit,
    onSendTestMessage: () -> Unit,
    onPrepareChatDiscovery: () -> Unit,
    onFindChatId: () -> Unit,
    onFindRecentChats: () -> Unit,
    onResetChatDiscovery: () -> Unit,
    onUseChat: (TelegramChatCandidate) -> Unit,
    onRetryQueue: () -> Unit,
    onClearQueue: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    InfoCard(title = "Telegram-уведомления") {
        Text(
            text = "Для Telegram-уведомлений нужен ваш собственный Cloudflare Worker relay. " +
                "Bot token хранится только в Worker secrets и не вводится в приложение.",
            style = MaterialTheme.typography.bodySmall,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Включить Telegram-уведомления")
            Switch(
                checked = uiState.telegramSettings.enabled,
                onCheckedChange = onEnabledChange,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text(
            text = "Как настроить Telegram через Cloudflare Worker",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "Как настроить:\n" +
                "1. Создай Telegram-бота через BotFather.\n" +
                "2. Создай Cloudflare Worker.\n" +
                "3. Добавь в Worker secrets:\n" +
                "   BOT_TOKEN — токен Telegram-бота\n" +
                "   RELAY_SECRET — секрет доступа к Worker\n" +
                "4. Введи Worker URL и Relay Secret в приложении.\n" +
                "5. Получи Chat ID через /start или введи вручную.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(
            onClick = {
                clipboardManager.setText(AnnotatedString(TelegramWorkerTemplate.WORKER_JS_CODE))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Скопировать шаблон Worker-кода")
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
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text(
            text = "Получение chat_id",
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = "Чтобы получить chat_id автоматически:\n" +
                "1. Нажми «Начать получение chat_id».\n" +
                "2. Открой Telegram.\n" +
                "3. Напиши своему боту /start в личном чате.\n" +
                "   Для группы: добавь бота в группу и напиши /start в группе.\n" +
                "4. Вернись в приложение.\n" +
                "5. Нажми «Получить chat_id».",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = "Для групп chat_id обычно отрицательный и может начинаться с -100.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onPrepareChatDiscovery,
            enabled = !uiState.telegramChatDiscovery.isPreparing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Начать получение chat_id")
        }
        Button(
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
        OutlinedButton(
            onClick = onResetChatDiscovery,
            enabled = !uiState.telegramChatDiscovery.isPreparing &&
                !uiState.telegramChatDiscovery.isLoading &&
                !uiState.telegramChatDiscovery.isLoadingRecent,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сбросить поиск chat_id")
        }
        if (
            uiState.telegramChatDiscovery.isPreparing ||
            uiState.telegramChatDiscovery.isLoading ||
            uiState.telegramChatDiscovery.isLoadingRecent
        ) {
            CircularProgressIndicator()
        }
        uiState.telegramChatDiscovery.discoveryOffset?.let { offset ->
            DetailLine("Offset поиска", offset.toString())
        }
        uiState.telegramChatDiscovery.statusMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        uiState.telegramChatDiscovery.errorMessage?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        uiState.telegramChatDiscovery.candidates.forEach { candidate ->
            TelegramChatCandidateCard(
                candidate = candidate,
                onUseChat = onUseChat,
            )
        }
        if (uiState.telegramSettings.chatId.isNotBlank()) {
            DetailLine("Текущий chat_id", uiState.telegramSettings.chatId)
        }
        OutlinedTextField(
            value = uiState.telegramSettings.chatId,
            onValueChange = onChatIdChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Chat ID") },
            singleLine = true,
            supportingText = {
                Text("Можно ввести вручную или получить через /start выше")
            },
        )
        Button(
            onClick = onSaveSettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сохранить настройки")
        }
        Button(
            onClick = onTestWorker,
            enabled = !uiState.isTestingTelegram,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Проверить Worker")
        }
        Button(
            onClick = onSendTestMessage,
            enabled = !uiState.isSendingTelegramTest,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Отправить тестовое сообщение")
        }
        if (uiState.isTestingTelegram || uiState.isSendingTelegramTest) {
            CircularProgressIndicator()
        }
        uiState.lastTelegramTestMessage?.let { message ->
            DetailLine("Проверка Worker", message)
        }
        uiState.lastTelegramSendMessage?.let { message ->
            DetailLine("Тестовая отправка", message)
        }
        DetailLine("Telegram", uiState.telegramSettings.configurationStatusLabel())
        DetailLine("Последний тест", uiState.lastTelegramTestResult.toLastTestStatusLabel())
        DetailLine("Последняя отправка", uiState.lastTelegramSendResult.toLastSendStatusLabel())
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        TelegramQueueSection(
            uiState = uiState,
            onRetryQueue = onRetryQueue,
            onClearQueue = onClearQueue,
        )
    }
}

@Composable
private fun TelegramQueueSection(
    uiState: MainUiState,
    onRetryQueue: () -> Unit,
    onClearQueue: () -> Unit,
) {
    Text(
        text = "Очередь Telegram",
        style = MaterialTheme.typography.titleSmall,
    )
    when {
        !uiState.telegramSettings.enabled -> {
            Text(
                text = "Telegram-уведомления выключены. События БС не сохраняются в очередь.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        !uiState.telegramSettings.isConfigured -> {
            Text(
                text = "Telegram не настроен. Новые события БС будут сохраняться в очередь, " +
                    "но отправить их не получится до настройки Worker URL, Relay Secret и Chat ID.",
                style = MaterialTheme.typography.bodySmall,
            )
            DetailLine("Неотправленных сообщений", uiState.pendingReportsCount.toString())
        }
        uiState.pendingReportsCount == 0 -> {
            Text(
                text = "Очередь Telegram пуста",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        else -> {
            DetailLine("Неотправленных сообщений", uiState.pendingReportsCount.toString())
            Text(
                text = "Worker недоступен или отправка не удалась. Сообщения сохраняются в очередь. " +
                    "Прямое подключение к Telegram не используется.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    uiState.lastQueueFlushResult?.let { flushResult ->
        DetailLine("Отправлено", flushResult.sentCount.toString())
        DetailLine("Ошибок", flushResult.failedCount.toString())
        if (flushResult.skippedCount > 0) {
            DetailLine("Пропущено", flushResult.skippedCount.toString())
        }
        flushResult.lastError?.let { error ->
            DetailLine("Последняя ошибка", error)
        }
    }
    OutlinedButton(
        onClick = onRetryQueue,
        enabled = !uiState.isFlushingTelegramQueue && uiState.pendingReportsCount > 0,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Повторить отправку очереди")
    }
    if (uiState.pendingReportsCount > 0) {
        Text(
            text = "Очистка удалит все неотправленные сообщения без возможности восстановления.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = onClearQueue,
            enabled = !uiState.isFlushingTelegramQueue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Очистить очередь")
        }
    }
    if (uiState.isFlushingTelegramQueue) {
        CircularProgressIndicator()
    }
}

@Composable
private fun TelegramChatCandidateCard(
    candidate: TelegramChatCandidate,
    onUseChat: (TelegramChatCandidate) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DetailLine("Тип", candidate.type.toDisplayLabel())
            DetailLine("Название", candidate.displayName())
            candidate.username?.let { username ->
                DetailLine("Username", "@$username")
            }
            DetailLine("Chat ID", candidate.chatId)
            candidate.sourceMessageText?.let { message ->
                DetailLine("Последнее сообщение", message)
            }
            OutlinedButton(
                onClick = { onUseChat(candidate) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Использовать этот чат")
            }
        }
    }
}

@Composable
private fun MonitoringCard(monitorState: WhitelistMonitorState?) {
    InfoCard(title = "Мониторинг БС") {
        if (monitorState == null) {
            Text(
                text = "Мониторинг ещё не инициализирован.",
                style = MaterialTheme.typography.bodyMedium,
            )
            return@InfoCard
        }

        DetailLine(
            "Последнее подтверждённое состояние",
            monitorState.lastConfirmedState.toDisplayLabel(),
        )
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
            monitorState.lastConfirmedAtMillis?.let { formatCheckedAt(it) } ?: "—",
        )
    }
}

@Composable
private fun StateChangeEventCard(event: WhitelistStateChangeEvent) {
    InfoCard(title = "Событие состояния") {
        Text(
            text = event.type.toEventTitle(),
            style = MaterialTheme.typography.titleMedium,
        )
        DetailLine("Было", event.oldState.toDisplayLabel())
        DetailLine("Стало", event.newState.toDisplayLabel())
        DetailLine("Время", formatCheckedAt(event.changedAtMillis))
    }
}

@Composable
private fun StatusCard(result: NetworkCheckResult) {
    InfoCard(title = "Текущий результат проверки") {
        Text(
            text = result.state.toDisplayLabel(),
            style = MaterialTheme.typography.titleMedium,
        )
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
            Text(
                text = "Проверка сайтов не выполнена: мобильная сеть недоступна.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            GroupSummaryLine("Внешние сайты", result.foreignSummary)
            GroupSummaryLine("Локальные сайты", result.localSummary)
        }

        DetailLine(
            "Последняя проверка",
            formatCheckedAt(result.checkedAtMillis),
        )
    }
}

@Composable
private fun SitesCard(result: NetworkCheckResult) {
    if (result.siteResults.isEmpty()) return

    InfoCard(title = "Результаты по сайтам") {
        val foreignResults = result.siteResults.filter { it.target.group == TargetGroup.FOREIGN }
        val localResults = result.siteResults.filter { it.target.group == TargetGroup.LOCAL }

        Text(
            text = "Внешние сайты",
            style = MaterialTheme.typography.titleSmall,
        )
        foreignResults.forEach { site ->
            SiteResultBlock(site)
            if (site != foreignResults.last()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

        Text(
            text = "Локальные сайты",
            style = MaterialTheme.typography.titleSmall,
        )
        localResults.forEach { site ->
            SiteResultBlock(site)
            if (site != localResults.last()) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun GroupSummaryLine(label: String, summary: TargetGroupSummary) {
    DetailLine(
        label,
        "${summary.availableCount}/${summary.totalCount} доступно",
    )
}

@Composable
private fun SiteResultBlock(site: SiteCheckResult) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = site.target.name, style = MaterialTheme.typography.titleSmall)
        DetailLine("Группа", site.target.group.toDisplayLabel())
        DetailLine(
            "Статус",
            if (site.available) "доступен" else "недоступен",
        )
        DetailLine("HTTP", site.httpCode?.toString() ?: "—")
        if (site.errorType != SiteCheckErrorType.NONE) {
            DetailLine("Тип ошибки", site.errorType.name)
        }
        DetailLine("Ошибка", site.error ?: "—")
        DetailLine("Время", "${site.durationMs} мс")
    }
}

@Composable
private fun ErrorCard(message: String) {
    InfoCard(
        title = "Ошибка",
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun InfoCard(
    title: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = "$label:", style = MaterialTheme.typography.labelMedium)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun TargetGroup.toDisplayLabel(): String = when (this) {
    TargetGroup.FOREIGN -> "Внешние"
    TargetGroup.LOCAL -> "Локальные"
}

private fun formatCheckedAt(millis: Long): String = millis.toDisplayDateTime()
