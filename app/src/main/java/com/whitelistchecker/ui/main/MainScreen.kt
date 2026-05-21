package com.whitelistchecker.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.SiteCheckResult
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
private fun StatusCard(result: NetworkCheckResult) {
    InfoCard(title = "Статус") {
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

private fun WhitelistState.toDisplayLabel(): String = when (this) {
    WhitelistState.UNKNOWN -> "⚪ Неизвестное состояние"
    WhitelistState.WHITELIST_OFF -> "🟢 Белые списки не обнаружены"
    WhitelistState.WHITELIST_ON -> "🟠 Похоже на включённые белые списки"
    WhitelistState.NO_MOBILE_INTERNET -> "🔴 Мобильного интернета нет"
    WhitelistState.PARTIAL_PROBLEM -> "🟡 Частичная проблема сети"
    WhitelistState.CELLULAR_NETWORK_UNAVAILABLE -> "⚪ Мобильная сеть недоступна"
}

private fun formatCheckedAt(millis: Long): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm", Locale("ru"))
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(formatter)
}
