package com.whitelistchecker.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whitelistchecker.ui.components.ActionGrid
import com.whitelistchecker.ui.components.ActionGridItem
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.ErrorCard
import com.whitelistchecker.ui.components.StatusChip
import com.whitelistchecker.ui.components.StatusTone
import com.whitelistchecker.ui.configurationStatusLabel
import com.whitelistchecker.ui.main.MainUiState
import com.whitelistchecker.ui.navigation.AppScreen
import com.whitelistchecker.ui.statistics.HomeStatisticsSummaryCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    uiState: MainUiState,
    onCheckMobileNetwork: () -> Unit,
    onOpenScreen: (AppScreen) -> Unit,
    onRefreshLastCheckPresentation: () -> Unit,
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "Whitelist Checker", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Проверка через мобильную сеть, даже при активном Wi-Fi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = onCheckMobileNetwork,
                enabled = !uiState.isChecking,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Проверить мобильную сеть")
            }
            if (uiState.isChecking) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            }

            LastCheckResultCard(
                displayState = uiState.lastCheckDisplayState,
                onRefreshPresentation = onRefreshLastCheckPresentation,
            )

            HomeStatisticsSummaryCard(
                uiState = uiState.homeStatisticsUiState,
                onOpenStatistics = { onOpenScreen(AppScreen.STATISTICS) },
            )

            AppCard(title = "Краткий статус") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    StatusChip(
                        text = if (uiState.localNotificationSettings.enabled) "Локальные: вкл" else "Локальные: выкл",
                        tone = if (uiState.localNotificationSettings.enabled) StatusTone.SUCCESS else StatusTone.NEUTRAL,
                    )
                    val telegramTone = when {
                        uiState.telegramSettings.isConfigured -> StatusTone.SUCCESS
                        uiState.telegramSettings.canTestWorker -> StatusTone.WARNING
                        else -> StatusTone.NEUTRAL
                    }
                    StatusChip(
                        text = "Telegram: ${uiState.telegramSettings.configurationStatusLabel()}",
                        tone = telegramTone,
                    )
                    StatusChip(
                        text = if (uiState.backgroundCheckSettings.enabled) "Авто: вкл" else "Авто: выкл",
                        tone = if (uiState.backgroundCheckSettings.enabled) StatusTone.SUCCESS else StatusTone.NEUTRAL,
                    )
                    StatusChip(
                        text = "Очередь: ${uiState.pendingReportsCount}",
                        tone = if (uiState.pendingReportsCount > 0) StatusTone.WARNING else StatusTone.NEUTRAL,
                    )
                }
            }

            Text("Быстрые действия", style = MaterialTheme.typography.titleSmall)
            ActionGrid(
                items = listOf(
                    ActionGridItem("Уведомления", subtitle = "Telegram") { onOpenScreen(AppScreen.NOTIFICATIONS) },
                    ActionGridItem("Проверки", subtitle = "Сайты") { onOpenScreen(AppScreen.CHECK_SETTINGS) },
                    ActionGridItem("Автопроверка", subtitle = "WorkManager") { onOpenScreen(AppScreen.AUTO_CHECK) },
                    ActionGridItem("Диагностика", subtitle = "Подробно") { onOpenScreen(AppScreen.DIAGNOSTICS) },
                ),
            )

            uiState.errorMessage?.let { ErrorCard(it) }
        }
    }
}
