package com.whitelistchecker.ui.autocheck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.ActiveMonitoringSettings
import com.whitelistchecker.domain.model.ActiveMonitoringState
import com.whitelistchecker.domain.model.ActiveMonitoringStatus
import com.whitelistchecker.domain.model.NotificationPolicy
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow
import com.whitelistchecker.ui.components.ErrorCard
import com.whitelistchecker.ui.components.StatusChip
import com.whitelistchecker.ui.main.MainUiState
import com.whitelistchecker.ui.toDisplayDateTime

@Composable
internal fun ActiveMonitoringTab(
    uiState: MainUiState,
    onIntervalInputChange: (String) -> Unit,
    onSaveInterval: () -> Unit,
    onNotificationPolicyChange: (NotificationPolicy) -> Unit,
    onNotifyOnAccessRestoredChange: (Boolean) -> Unit,
    onTelegramCommandsEnabledChange: (Boolean) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRunCheckNow: () -> Unit,
) {
    val activeStatus = uiState.activeMonitoringStatus
    val activeSettings = uiState.activeMonitoringSettings

    AppCard(title = null) {
        ActiveHeader(activeStatus.state)
        ActiveIntervalEditor(
            intervalInput = uiState.activeMonitoringIntervalInput,
            intervalError = uiState.activeMonitoringIntervalError,
            onIntervalInputChange = onIntervalInputChange,
            onSaveInterval = onSaveInterval,
        )
        Text(
            text = stringResource(R.string.autocheck_notifications_title),
            style = MaterialTheme.typography.titleSmall,
        )
        NotificationPolicySelector(
            selectedPolicy = activeSettings.notificationPolicy,
            onPolicyChange = onNotificationPolicyChange,
        )
        SwitchSettingRow(
            title = stringResource(R.string.autocheck_notify_restored),
            checked = activeSettings.notifyOnAccessRestored,
            onCheckedChange = onNotifyOnAccessRestoredChange,
        )
        SwitchSettingRow(
            title = stringResource(R.string.autocheck_telegram_commands),
            checked = activeSettings.telegramCommandsEnabled,
            onCheckedChange = onTelegramCommandsEnabledChange,
        )
        if (activeSettings.telegramCommandsEnabled) {
            activeStatus.telegramLastError?.takeIf { it.isNotBlank() }?.let { error ->
                CompactWarning(text = error)
            }
        }
        ActiveActions(
            state = activeStatus.state,
            onStart = onStart,
            onStop = onStop,
            onRunCheckNow = onRunCheckNow,
        )
    }

    ActiveStatusSummary(activeStatus)
    ActiveMonitoringExplanation()
}

@Composable
private fun ActiveHeader(state: ActiveMonitoringState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.autocheck_active_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        StatusChip(
            text = stringResource(state.labelRes()),
            tone = state.toTone(),
        )
    }
}

@Composable
private fun ActiveIntervalEditor(
    intervalInput: String,
    intervalError: String?,
    onIntervalInputChange: (String) -> Unit,
    onSaveInterval: () -> Unit,
) {
    Text(
        text = stringResource(R.string.autocheck_interval_title),
        style = MaterialTheme.typography.titleSmall,
    )
    OutlinedTextField(
        value = intervalInput,
        onValueChange = onIntervalInputChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.autocheck_interval_minutes_label)) },
        singleLine = true,
        isError = intervalError != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        supportingText = {
            Text(
                text = intervalError ?: stringResource(
                    R.string.autocheck_active_interval_supporting,
                    ActiveMonitoringSettings.MIN_INTERVAL_MINUTES,
                    ActiveMonitoringSettings.MAX_INTERVAL_MINUTES,
                ),
            )
        },
    )
    OutlinedButton(onClick = onSaveInterval, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.autocheck_save_interval))
    }
}

@Composable
private fun ActiveActions(
    state: ActiveMonitoringState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRunCheckNow: () -> Unit,
) {
    when (state) {
        ActiveMonitoringState.STOPPED -> {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.autocheck_start))
            }
        }
        ActiveMonitoringState.STOPPED_BY_SYSTEM,
        ActiveMonitoringState.ERROR,
        -> {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.autocheck_start_again))
            }
        }
        ActiveMonitoringState.STARTING -> {
            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.autocheck_starting))
            }
        }
        ActiveMonitoringState.RUNNING -> {
            OutlinedButton(onClick = onRunCheckNow, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.autocheck_check_now))
            }
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.autocheck_stop))
            }
        }
        ActiveMonitoringState.CHECKING -> {
            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.autocheck_checking_now))
            }
            OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.autocheck_stop))
            }
        }
        ActiveMonitoringState.STOPPING -> {
            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.autocheck_stopping))
            }
        }
    }
}

@Composable
private fun ActiveStatusSummary(status: ActiveMonitoringStatus) {
    var expanded by remember { mutableStateOf(false) }

    AppCard(title = null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = stringResource(R.string.autocheck_last_check_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            status.lastCheckAtMillis?.let {
                Text(
                    text = it.toDisplayDateTime(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (status.lastCheckAtMillis == null) {
            Text(
                text = stringResource(R.string.autocheck_no_checks_yet),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = { expanded = !expanded }) {
            Text(
                stringResource(
                    if (expanded) {
                        R.string.autocheck_hide_details
                    } else {
                        R.string.autocheck_show_details
                    },
                ),
            )
        }
        if (expanded) {
            status.lastStopReason?.takeIf { it.isNotBlank() }?.let {
                CompactDetailRow(stringResource(R.string.autocheck_stop_reason), it)
            }
            status.lastError?.takeIf { it.isNotBlank() }?.let {
                ErrorCard(it)
            }
            if (status.backgroundWasEnabledBeforeStart) {
                CompactDetailRow(
                    label = stringResource(R.string.autocheck_background_restore_after_stop),
                    value = stringResource(R.string.autocheck_yes),
                )
            }
        }
    }
}

@Composable
private fun ActiveMonitoringExplanation() {
    var expanded by remember { mutableStateOf(false) }

    AppCard(title = null) {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(
                    if (expanded) {
                        R.string.autocheck_how_monitoring_works_hide
                    } else {
                        R.string.autocheck_how_monitoring_works_show
                    },
                ),
            )
        }
        if (expanded) {
            Text(
                text = stringResource(R.string.autocheck_active_foreground_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.autocheck_active_android_15_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompactWarning(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
