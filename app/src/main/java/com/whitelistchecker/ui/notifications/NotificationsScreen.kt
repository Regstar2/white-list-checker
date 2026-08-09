package com.whitelistchecker.ui.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.LocalNotificationResult
import com.whitelistchecker.domain.model.TelegramTestResult
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.main.MainUiState

@Composable
fun NotificationsScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onLocalEnabledChange: (Boolean) -> Unit,
    onTelegramEnabledChange: (Boolean) -> Unit,
    onOpenLocalNotifications: () -> Unit,
    onOpenTelegramNotifications: () -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.notifications_title), onBack = onBack) {
        NotificationOverviewCard(
            title = stringResource(R.string.notifications_local_title),
            checked = uiState.localNotificationSettings.enabled,
            onCheckedChange = onLocalEnabledChange,
            onClick = onOpenLocalNotifications,
            lines = listOfNotNull(
                localPermissionOverview(uiState),
                uiState.lastLocalNotificationResult?.let { localLastResultOverview(it) },
            ),
        )

        val telegramSummary = telegramConnectionSummary(uiState)
        NotificationOverviewCard(
            title = stringResource(R.string.notifications_telegram_title),
            checked = uiState.telegramSettings.enabled,
            onCheckedChange = onTelegramEnabledChange,
            onClick = onOpenTelegramNotifications,
            lines = listOf(
                telegramSummary.title,
                stringResource(
                    R.string.notifications_recipients_count,
                    uiState.telegramSettings.recipients.size,
                ),
                telegramQueueOverview(uiState),
            ) + listOfNotNull(telegramSummary.detail),
            isError = telegramSummary.isError ||
                (uiState.lastQueueFlushResult?.failedCount ?: 0) > 0 ||
                uiState.lastQueueFlushResult?.lastError != null,
        )
    }
}

@Composable
private fun NotificationOverviewCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    lines: List<String>,
    isError: Boolean = false,
) {
    AppCard(
        title = null,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                lines.forEachIndexed { index, line ->
                    Text(
                        text = line,
                        style = if (index == 0) {
                            MaterialTheme.typography.bodyMedium
                        } else {
                            MaterialTheme.typography.bodySmall
                        },
                        color = if (isError && index == 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.notifications_open_details),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun localPermissionOverview(uiState: MainUiState): String {
    return when {
        !uiState.notificationPermissionRequired -> stringResource(R.string.notifications_permission_not_required)
        uiState.notificationsAllowed -> stringResource(R.string.notifications_permission_granted)
        else -> stringResource(R.string.notifications_permission_denied)
    }
}

@Composable
private fun localLastResultOverview(result: LocalNotificationResult): String {
    return when (result) {
        LocalNotificationResult.Success -> stringResource(R.string.notifications_local_last_success)
        LocalNotificationResult.Disabled -> stringResource(R.string.notifications_local_last_disabled)
        LocalNotificationResult.PermissionNotGranted -> stringResource(R.string.notifications_local_last_no_permission)
        is LocalNotificationResult.Failure -> stringResource(R.string.notifications_local_last_error)
    }
}

@Composable
private fun telegramQueueOverview(uiState: MainUiState): String {
    return if (uiState.pendingReportsCount == 0) {
        stringResource(R.string.notifications_queue_empty)
    } else {
        stringResource(R.string.notifications_queue_pending, uiState.pendingReportsCount)
    }
}

private data class TelegramSummary(
    val title: String,
    val detail: String?,
    val isError: Boolean,
)

@Composable
private fun telegramConnectionSummary(uiState: MainUiState): TelegramSummary {
    val settings = uiState.telegramSettings
    return when {
        settings.workerUrl.isBlank() || settings.relaySecret.isBlank() -> TelegramSummary(
            title = stringResource(R.string.notifications_worker_not_configured),
            detail = null,
            isError = false,
        )
        uiState.lastTelegramTestResult == null -> TelegramSummary(
            title = stringResource(R.string.notifications_worker_not_tested),
            detail = null,
            isError = false,
        )
        uiState.lastTelegramTestResult == TelegramTestResult.Success -> TelegramSummary(
            title = stringResource(R.string.notifications_worker_connected),
            detail = null,
            isError = false,
        )
        else -> TelegramSummary(
            title = stringResource(R.string.notifications_worker_error),
            detail = (uiState.lastTelegramTestResult as? TelegramTestResult.Failure)?.reason,
            isError = true,
        )
    }
}
