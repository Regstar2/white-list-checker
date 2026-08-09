package com.whitelistchecker.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.TelegramTestResult
import com.whitelistchecker.ui.TelegramWorkerTemplate
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.main.MainUiState

@Composable
fun TelegramWorkerSetupScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onWorkerUrlChange: (String) -> Unit,
    onRelaySecretChange: (String) -> Unit,
    onSaveTelegramSettings: () -> Unit,
    onTestWorker: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    var showRelaySecret by rememberSaveable { mutableStateOf(false) }
    var showInstructions by rememberSaveable { mutableStateOf(false) }

    ScreenScaffold(title = stringResource(R.string.notifications_worker_setup_title), onBack = onBack) {
        AppCard(title = stringResource(R.string.notifications_worker_connection)) {
            OutlinedTextField(
                value = uiState.telegramSettings.workerUrl,
                onValueChange = onWorkerUrlChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.notifications_worker_url)) },
                placeholder = { Text(TelegramWorkerTemplate.EXAMPLE_WORKER_URL) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            )
            Text(
                text = extractHost(uiState.telegramSettings.workerUrl)
                    ?: stringResource(R.string.notifications_worker_host_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            OutlinedTextField(
                value = uiState.telegramSettings.relaySecret,
                onValueChange = onRelaySecretChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.notifications_relay_secret)) },
                singleLine = true,
                visualTransformation = if (showRelaySecret) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    TextButton(onClick = { showRelaySecret = !showRelaySecret }) {
                        Text(
                            text = if (showRelaySecret) {
                                stringResource(R.string.notifications_hide_secret_short)
                            } else {
                                stringResource(R.string.notifications_show_secret_short)
                            },
                        )
                    }
                },
            )
        }

        AppCard(title = stringResource(R.string.notifications_status_title)) {
            val result = uiState.lastTelegramTestResult
            StatusLine(
                text = when (result) {
                    TelegramTestResult.Success -> stringResource(R.string.notifications_worker_connected)
                    is TelegramTestResult.Failure -> stringResource(R.string.notifications_worker_error)
                    null -> stringResource(R.string.notifications_worker_not_tested)
                },
                tone = when (result) {
                    TelegramTestResult.Success -> StatusTone.Success
                    is TelegramTestResult.Failure -> StatusTone.Error
                    null -> StatusTone.Neutral
                },
                detail = (result as? TelegramTestResult.Failure)?.reason,
            )
            uiState.lastTelegramTestMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onTestWorker,
                enabled = !uiState.isTestingTelegram,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.notifications_test_worker))
            }
            Button(
                onClick = onSaveTelegramSettings,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.notifications_save_settings))
            }
        }
        if (uiState.isTestingTelegram) {
            CircularProgressIndicator()
        }

        AppCard(title = stringResource(R.string.notifications_new_worker_setup)) {
            SettingsNavigationRow(
                title = stringResource(R.string.notifications_instruction),
                onClick = { showInstructions = true },
            )
            HorizontalDivider()
            SettingsNavigationRow(
                title = stringResource(R.string.notifications_copy_worker_template),
                onClick = {
                    clipboardManager.setText(AnnotatedString(TelegramWorkerTemplate.WORKER_JS_CODE))
                },
            )
        }
    }

    if (showInstructions) {
        WorkerInstructionsDialog(onDismiss = { showInstructions = false })
    }
}

@Composable
private fun WorkerInstructionsDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notifications_instruction)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.notifications_worker_instruction_1))
                Text(stringResource(R.string.notifications_worker_instruction_2))
                Text(stringResource(R.string.notifications_worker_instruction_3))
                Text(stringResource(R.string.notifications_worker_instruction_4))
                Text(stringResource(R.string.notifications_worker_instruction_5))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.notifications_close))
            }
        },
    )
}
