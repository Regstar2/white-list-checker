package com.whitelistchecker.ui.autocheck

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.whitelistchecker.domain.model.BackgroundCheckInterval
import com.whitelistchecker.domain.model.BackgroundCheckSettings
import com.whitelistchecker.domain.model.BackgroundCheckStatus
import com.whitelistchecker.domain.model.NotificationPolicy
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.CompactDetailRow
import com.whitelistchecker.ui.components.ErrorCard
import com.whitelistchecker.ui.components.StatusChip
import com.whitelistchecker.ui.main.MainUiState
import com.whitelistchecker.ui.toDisplayDateTime

@Composable
internal fun BackgroundCheckTab(
    uiState: MainUiState,
    onEnabledChange: (Boolean) -> Unit,
    onPresetIntervalChange: (Long) -> Unit,
    onUseCustomIntervalChange: (Boolean) -> Unit,
    onCustomIntervalInputChange: (String) -> Unit,
    onNotificationPolicyChange: (NotificationPolicy) -> Unit,
    onSave: () -> Unit,
    onRunNow: () -> Unit,
) {
    AppCard(title = null) {
        BackgroundHeader(
            enabled = uiState.backgroundCheckSettings.enabled,
            onEnabledChange = onEnabledChange,
        )
        BackgroundIntervalSelector(
            settings = uiState.backgroundCheckSettings,
            useCustomInterval = uiState.useCustomInterval,
            customIntervalInput = uiState.customIntervalInput,
            intervalError = uiState.intervalError,
            onPresetIntervalChange = onPresetIntervalChange,
            onUseCustomIntervalChange = onUseCustomIntervalChange,
            onCustomIntervalInputChange = onCustomIntervalInputChange,
        )
        Text(
            text = stringResource(R.string.autocheck_notifications_title),
            style = MaterialTheme.typography.titleSmall,
        )
        NotificationPolicySelector(
            selectedPolicy = uiState.backgroundCheckSettings.notificationPolicy,
            onPolicyChange = onNotificationPolicyChange,
        )
        Button(
            onClick = onSave,
            enabled = uiState.backgroundCheckSettings.enabled && !uiState.isSavingBackgroundSettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.autocheck_save))
        }
        OutlinedButton(onClick = onRunNow, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.autocheck_check_now))
        }
    }

    BackgroundStatusSummary(
        status = uiState.backgroundCheckStatus,
        pendingReportsCount = uiState.pendingReportsCount,
    )
}

@Composable
private fun BackgroundHeader(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.autocheck_background_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = enabled,
            onCheckedChange = onEnabledChange,
        )
    }
    Text(
        text = stringResource(
            if (enabled) {
                R.string.autocheck_background_enabled_hint
            } else {
                R.string.autocheck_background_disabled_hint
            },
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackgroundIntervalSelector(
    settings: BackgroundCheckSettings,
    useCustomInterval: Boolean,
    customIntervalInput: String,
    intervalError: String?,
    onPresetIntervalChange: (Long) -> Unit,
    onUseCustomIntervalChange: (Boolean) -> Unit,
    onCustomIntervalInputChange: (String) -> Unit,
) {
    Text(
        text = stringResource(R.string.autocheck_interval_title),
        style = MaterialTheme.typography.titleSmall,
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BackgroundCheckInterval.entries.take(2).forEach { interval ->
                IntervalChip(
                    selected = !useCustomInterval && settings.intervalMinutes == interval.minutes,
                    label = stringResource(interval.labelRes()),
                    onClick = { onPresetIntervalChange(interval.minutes) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IntervalChip(
                selected = !useCustomInterval &&
                    settings.intervalMinutes == BackgroundCheckInterval.SIXTY_MINUTES.minutes,
                label = stringResource(BackgroundCheckInterval.SIXTY_MINUTES.labelRes()),
                onClick = { onPresetIntervalChange(BackgroundCheckInterval.SIXTY_MINUTES.minutes) },
                modifier = Modifier.weight(1f),
            )
            IntervalChip(
                selected = useCustomInterval,
                label = stringResource(R.string.autocheck_interval_custom),
                onClick = { onUseCustomIntervalChange(true) },
                modifier = Modifier.weight(1f),
            )
        }
    }
    if (useCustomInterval) {
        OutlinedTextField(
            value = customIntervalInput,
            onValueChange = onCustomIntervalInputChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.autocheck_interval_minutes_label)) },
            singleLine = true,
            isError = intervalError != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            supportingText = {
                Text(
                    text = intervalError ?: stringResource(
                        R.string.autocheck_background_interval_supporting,
                        BackgroundCheckSettings.MIN_INTERVAL_MINUTES,
                    ),
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntervalChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier,
    )
}

@Composable
private fun BackgroundStatusSummary(
    status: BackgroundCheckStatus,
    pendingReportsCount: Int,
) {
    var expanded by remember { mutableStateOf(false) }
    val summaryTime = status.lastFinishedAtMillis ?: status.lastRunAtMillis

    AppCard(title = null) {
        Text(
            text = stringResource(R.string.autocheck_last_check_title),
            style = MaterialTheme.typography.titleSmall,
        )
        if (summaryTime == null) {
            Text(
                text = stringResource(R.string.autocheck_no_checks_yet),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = summaryTime.toDisplayDateTime(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(
                    text = stringResource(status.lastState.labelRes()),
                    tone = status.lastState.toTone(),
                )
            }
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
            CompactDetailRow(
                label = stringResource(R.string.autocheck_last_run),
                value = status.lastRunAtMillis?.toDisplayDateTime() ?: stringResource(R.string.autocheck_no_value),
            )
            CompactDetailRow(
                label = stringResource(R.string.autocheck_finished_at),
                value = status.lastFinishedAtMillis?.toDisplayDateTime() ?: stringResource(R.string.autocheck_no_value),
            )
            CompactDetailRow(
                label = stringResource(R.string.autocheck_last_state),
                value = stringResource(status.lastState.labelRes()),
            )
            status.lastError?.takeIf { it.isNotBlank() }?.let { ErrorCard(it) }
            CompactDetailRow(
                label = stringResource(R.string.autocheck_telegram_result),
                value = status.lastTelegramSendResult ?: stringResource(R.string.autocheck_no_value),
            )
            status.lastQueueFlushSummary?.let {
                CompactDetailRow(stringResource(R.string.autocheck_queue_flush), it)
            }
            CompactDetailRow(
                label = stringResource(R.string.autocheck_queue_size),
                value = pendingReportsCount.toString(),
            )
        }
    }
}
