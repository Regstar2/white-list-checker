package com.whitelistchecker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whitelistchecker.ui.components.DetailLine
import com.whitelistchecker.ui.components.ErrorCard
import com.whitelistchecker.ui.components.InfoCard
import com.whitelistchecker.ui.configurationStatusLabel
import com.whitelistchecker.ui.main.MainUiState
import com.whitelistchecker.ui.navigation.AppScreen
import com.whitelistchecker.ui.toDisplayDateTime
import com.whitelistchecker.ui.toDisplayLabel

@Composable
fun HomeScreen(
    uiState: MainUiState,
    onCheckMobileNetwork: () -> Unit,
    onOpenScreen: (AppScreen) -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "Whitelist Checker", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Проверка выполняется через мобильную сеть, даже если телефон подключён к Wi-Fi.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Button(
                onClick = onCheckMobileNetwork,
                enabled = !uiState.isChecking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Проверить мобильную сеть")
            }
            if (uiState.isChecking) {
                CircularProgressIndicator()
            }

            InfoCard(title = "Последний результат") {
                val result = uiState.result
                if (result == null) {
                    Text("Проверка ещё не выполнялась.")
                } else {
                    Text(result.state.toDisplayLabel(), style = MaterialTheme.typography.titleMedium)
                    DetailLine("Внешние сайты", "${result.foreignSummary.availableCount}/${result.foreignSummary.totalCount}")
                    DetailLine("Локальные сайты", "${result.localSummary.availableCount}/${result.localSummary.totalCount}")
                    DetailLine("Проверяемая сеть", result.checkedNetworkLabel)
                    DetailLine("Активная сеть", result.activeNetworkLabel)
                    DetailLine("Время", result.checkedAtMillis.toDisplayDateTime())
                }
            }

            InfoCard(title = "Краткий статус") {
                val localStatus = if (uiState.localNotificationSettings.enabled) "включены" else "выключены"
                DetailLine("Локальные уведомления", localStatus)
                DetailLine("Telegram", uiState.telegramSettings.configurationStatusLabel())
                DetailLine(
                    "Автопроверка",
                    if (uiState.backgroundCheckSettings.enabled) "включена" else "выключена",
                )
                DetailLine("Очередь Telegram", "${uiState.pendingReportsCount} сообщений")
            }

            Text("Быстрые действия", style = MaterialTheme.typography.titleSmall)
            OutlinedButton(onClick = { onOpenScreen(AppScreen.NOTIFICATIONS) }, modifier = Modifier.fillMaxWidth()) {
                Text("Уведомления")
            }
            OutlinedButton(onClick = { onOpenScreen(AppScreen.CHECK_SETTINGS) }, modifier = Modifier.fillMaxWidth()) {
                Text("Настройки проверки")
            }
            OutlinedButton(onClick = { onOpenScreen(AppScreen.AUTO_CHECK) }, modifier = Modifier.fillMaxWidth()) {
                Text("Автопроверка")
            }
            OutlinedButton(onClick = { onOpenScreen(AppScreen.DIAGNOSTICS) }, modifier = Modifier.fillMaxWidth()) {
                Text("Диагностика")
            }

            uiState.errorMessage?.let { ErrorCard(it) }
        }
    }
}
