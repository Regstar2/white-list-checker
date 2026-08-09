package com.whitelistchecker.ui.notifications

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.main.MainUiState

@Composable
fun TelegramQueueScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onRetryQueue: () -> Unit,
    onClearQueue: () -> Unit,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    val flushResult = uiState.lastQueueFlushResult

    ScreenScaffold(title = stringResource(R.string.notifications_queue_title), onBack = onBack) {
        AppCard(title = stringResource(R.string.notifications_queue_state_title)) {
            CompactDetailRow(
                label = stringResource(R.string.notifications_queue_pending_label),
                value = uiState.pendingReportsCount.toString(),
            )
            CompactDetailRow(
                label = stringResource(R.string.notifications_queue_sent_label),
                value = (flushResult?.sentCount ?: 0).toString(),
            )
            CompactDetailRow(
                label = stringResource(R.string.notifications_queue_errors_label),
                value = (flushResult?.failedCount ?: 0).toString(),
            )
            flushResult?.lastError?.let {
                Text(
                    text = stringResource(R.string.notifications_queue_last_error),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (uiState.pendingReportsCount == 0 && flushResult?.lastError == null) {
                Text(
                    text = stringResource(R.string.notifications_queue_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(
            onClick = onRetryQueue,
            enabled = !uiState.isFlushingTelegramQueue && uiState.pendingReportsCount > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.notifications_retry_queue))
        }
        TextButton(
            onClick = { confirmClear = true },
            enabled = !uiState.isFlushingTelegramQueue && uiState.pendingReportsCount > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.notifications_clear_queue),
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (uiState.isFlushingTelegramQueue) {
            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.notifications_clear_queue_title)) },
            text = { Text(stringResource(R.string.notifications_clear_queue_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmClear = false
                        onClearQueue()
                    },
                ) {
                    Text(
                        text = stringResource(R.string.notifications_clear_queue),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.notifications_cancel))
                }
            },
        )
    }
}
