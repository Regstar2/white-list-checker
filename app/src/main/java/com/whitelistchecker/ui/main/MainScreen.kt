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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.whitelistchecker.ui.permissionStatusLabel
import com.whitelistchecker.ui.toResultLabel
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.SiteCheckResult
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistMonitorState
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.domain.model.WhitelistStateChangeType
import com.whitelistchecker.domain.monitor.StateChangeDetector
import com.whitelistchecker.ui.toDisplayLabel
import com.whitelistchecker.ui.toEventTitle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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

private fun formatCheckedAt(millis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale("ru"))
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}
