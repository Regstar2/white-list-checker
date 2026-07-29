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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.whitelistchecker.domain.model.CheckPersistenceStatus
import com.whitelistchecker.ui.components.ActionGrid
import com.whitelistchecker.ui.components.ActionGridItem
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.ErrorCard
import com.whitelistchecker.ui.components.StatusChip
import com.whitelistchecker.ui.components.StatusTone
import com.whitelistchecker.ui.main.MainUiState
import com.whitelistchecker.ui.navigation.AppScreen

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

            PersistenceStatusCard(status = uiState.lastPersistenceStatus)

            Text("Быстрые действия", style = MaterialTheme.typography.titleSmall)
            ActionGrid(
                items = listOf(
                    ActionGridItem("Статистика", subtitle = "График БС") { onOpenScreen(AppScreen.STATISTICS) },
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

@Composable
private fun PersistenceStatusCard(status: CheckPersistenceStatus?) {
    if (status == null) return
    AppCard(title = null) {
        StatusChip(
            text = persistenceStatusLabel(status),
            tone = if (status.isComplete) StatusTone.SUCCESS else StatusTone.WARNING,
        )
        status.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

private fun persistenceStatusLabel(status: CheckPersistenceStatus): String {
    return if (status.isComplete) {
        "Статистика: записана"
    } else {
        "Статистика: ошибка"
    }
}
