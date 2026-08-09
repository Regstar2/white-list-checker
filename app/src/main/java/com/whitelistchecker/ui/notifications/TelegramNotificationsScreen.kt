package com.whitelistchecker.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.TelegramRecipient
import com.whitelistchecker.domain.model.TelegramSendResult
import com.whitelistchecker.domain.model.TelegramTestResult
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.main.MainUiState

@Composable
fun TelegramNotificationsScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onTelegramEnabledChange: (Boolean) -> Unit,
    onOpenWorkerSetup: () -> Unit,
    onOpenRecipientDiscovery: () -> Unit,
    onOpenQueue: () -> Unit,
    onTestWorker: () -> Unit,
    onSendTestMessage: () -> Unit,
    onSendCheckReport: () -> Unit,
    onRemoveRecipient: (String) -> Unit,
    onToggleRecipient: (String, Boolean) -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.notifications_telegram_title), onBack = onBack) {
        AppCard(title = null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.notifications_telegram_switch_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (uiState.telegramSettings.enabled) {
                            stringResource(R.string.notifications_enabled)
                        } else {
                            stringResource(R.string.notifications_telegram_disabled)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = uiState.telegramSettings.enabled,
                    onCheckedChange = onTelegramEnabledChange,
                )
            }
        }

        TelegramConnectionSummary(uiState = uiState)

        AppCard(title = stringResource(R.string.notifications_connection_title)) {
            SettingsNavigationRow(
                title = stringResource(R.string.notifications_worker_setup_title),
                subtitle = extractHost(uiState.telegramSettings.workerUrl)
                    ?: stringResource(R.string.notifications_worker_not_configured_short),
                onClick = onOpenWorkerSetup,
            )
            HorizontalDivider()
            SettingsNavigationRow(
                title = stringResource(R.string.notifications_check_connection),
                subtitle = uiState.lastTelegramTestMessage,
                onClick = onTestWorker,
                enabled = !uiState.isTestingTelegram,
            )
            if (uiState.isTestingTelegram) {
                CircularProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }

        AppCard(title = stringResource(R.string.notifications_recipients_title)) {
            if (uiState.telegramSettings.recipients.isEmpty()) {
                Text(
                    text = stringResource(R.string.notifications_recipients_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                uiState.telegramSettings.recipients.forEachIndexed { index, recipient ->
                    RecipientRow(
                        recipient = recipient,
                        onToggle = onToggleRecipient,
                        onRemove = onRemoveRecipient,
                    )
                    if (index != uiState.telegramSettings.recipients.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
            OutlinedButton(
                onClick = onOpenRecipientDiscovery,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                )
                Text(stringResource(R.string.notifications_add_recipient))
            }
        }

        AppCard(title = stringResource(R.string.notifications_check_title)) {
            Button(
                onClick = onSendTestMessage,
                enabled = !uiState.isSendingTelegramTest && !uiState.isSendingCheckReport,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.notifications_send_test_message))
            }
            SettingsNavigationRow(
                title = stringResource(R.string.notifications_send_last_report),
                subtitle = uiState.lastTelegramSendMessage,
                onClick = onSendCheckReport,
                enabled = !uiState.isSendingCheckReport &&
                    !uiState.isSendingTelegramTest &&
                    uiState.result != null,
            )
            if (uiState.isSendingTelegramTest || uiState.isSendingCheckReport) {
                CircularProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            uiState.lastTelegramSendResult?.let { result ->
                StatusLine(
                    text = telegramSendResultText(result),
                    tone = when (result) {
                        TelegramSendResult.Success -> StatusTone.Success
                        is TelegramSendResult.Failure -> StatusTone.Error
                    },
                    detail = (result as? TelegramSendResult.Failure)?.reason,
                )
            }
        }

        AppCard(title = stringResource(R.string.notifications_extra_title)) {
            SettingsNavigationRow(
                title = stringResource(R.string.notifications_queue_title),
                subtitle = if (uiState.pendingReportsCount == 0) {
                    stringResource(R.string.notifications_queue_empty)
                } else {
                    stringResource(R.string.notifications_queue_pending, uiState.pendingReportsCount)
                },
                onClick = onOpenQueue,
            )
        }
    }
}

@Composable
private fun TelegramConnectionSummary(uiState: MainUiState) {
    val workerResult = uiState.lastTelegramTestResult
    AppCard(title = null) {
        StatusLine(
            text = telegramConnectionText(uiState),
            tone = when {
                uiState.telegramSettings.workerUrl.isBlank() || uiState.telegramSettings.relaySecret.isBlank() ->
                    StatusTone.Warning
                workerResult == TelegramTestResult.Success -> StatusTone.Success
                workerResult is TelegramTestResult.Failure -> StatusTone.Error
                else -> StatusTone.Neutral
            },
            detail = (workerResult as? TelegramTestResult.Failure)?.reason,
        )
        CompactDetailRow(
            label = stringResource(R.string.notifications_worker_host),
            value = extractHost(uiState.telegramSettings.workerUrl)
                ?: stringResource(R.string.notifications_value_not_set),
        )
        CompactDetailRow(
            label = stringResource(R.string.notifications_recipients_label),
            value = uiState.telegramSettings.recipients.size.toString(),
        )
        CompactDetailRow(
            label = stringResource(R.string.notifications_queue_label),
            value = uiState.pendingReportsCount.toString(),
        )
    }
}

@Composable
private fun RecipientRow(
    recipient: TelegramRecipient,
    onToggle: (String, Boolean) -> Unit,
    onRemove: (String) -> Unit,
) {
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = recipient.displayName,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            recipient.username?.let {
                Text(
                    text = "@$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = recipient.type.displayLabel(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(
            checked = recipient.enabled,
            onCheckedChange = { onToggle(recipient.id, it) },
        )
        IconButton(onClick = { menuExpanded = true }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.notifications_recipient_menu),
            )
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.notifications_delete)) },
                onClick = {
                    menuExpanded = false
                    confirmDelete = true
                },
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.notifications_delete_recipient_title)) },
            text = { Text(stringResource(R.string.notifications_delete_recipient_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onRemove(recipient.id)
                    },
                ) {
                    Text(
                        text = stringResource(R.string.notifications_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.notifications_cancel))
                }
            },
        )
    }
}

@Composable
private fun telegramConnectionText(uiState: MainUiState): String {
    return when {
        uiState.telegramSettings.workerUrl.isBlank() || uiState.telegramSettings.relaySecret.isBlank() ->
            stringResource(R.string.notifications_worker_not_configured)
        uiState.lastTelegramTestResult == TelegramTestResult.Success ->
            stringResource(R.string.notifications_worker_connected)
        uiState.lastTelegramTestResult is TelegramTestResult.Failure ->
            stringResource(R.string.notifications_worker_error)
        else -> stringResource(R.string.notifications_worker_not_tested)
    }
}

@Composable
private fun telegramSendResultText(result: TelegramSendResult): String {
    return when (result) {
        TelegramSendResult.Success -> stringResource(R.string.notifications_send_success)
        is TelegramSendResult.Failure -> stringResource(R.string.notifications_send_error)
    }
}
