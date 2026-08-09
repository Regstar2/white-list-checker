package com.whitelistchecker.ui.notifications

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import com.whitelistchecker.ui.components.AppCard
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.main.MainUiState

@Composable
fun LocalNotificationsScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onLocalEnabledChange: (Boolean) -> Unit,
    onSendLocalTest: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val showPermissionButton = uiState.notificationPermissionRequired &&
        !uiState.notificationsAllowed &&
        uiState.localNotificationSettings.enabled

    ScreenScaffold(title = stringResource(R.string.notifications_local_title), onBack = onBack) {
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
                        text = stringResource(R.string.notifications_local_switch_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (uiState.localNotificationSettings.enabled) {
                            stringResource(R.string.notifications_enabled)
                        } else {
                            stringResource(R.string.notifications_disabled)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = uiState.localNotificationSettings.enabled,
                    onCheckedChange = onLocalEnabledChange,
                )
            }
        }

        AppCard(title = stringResource(R.string.notifications_status_title)) {
            StatusLine(
                text = localPermissionStatusText(uiState),
                tone = if (!uiState.notificationPermissionRequired || uiState.notificationsAllowed) {
                    StatusTone.Success
                } else {
                    StatusTone.Warning
                },
            )
            uiState.lastLocalNotificationResult?.let { result ->
                StatusLine(
                    text = localLastResultText(result),
                    tone = when (result) {
                        LocalNotificationResult.Success -> StatusTone.Success
                        LocalNotificationResult.Disabled -> StatusTone.Neutral
                        LocalNotificationResult.PermissionNotGranted -> StatusTone.Warning
                        is LocalNotificationResult.Failure -> StatusTone.Error
                    },
                    detail = (result as? LocalNotificationResult.Failure)?.reason,
                )
            }
        }

        AppCard(title = stringResource(R.string.notifications_when_send_title)) {
            Text(
                text = stringResource(R.string.notifications_when_send_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (showPermissionButton) {
            Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.notifications_allow_permission))
            }
        } else {
            Button(
                onClick = onSendLocalTest,
                enabled = !uiState.isSendingLocalTest && uiState.result != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.notifications_send_local_test),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (uiState.isSendingLocalTest) {
            CircularProgressIndicator()
        }

        AppCard(title = stringResource(R.string.notifications_system_settings)) {
            SettingsNavigationRow(
                title = stringResource(R.string.notifications_app_permissions),
                onClick = onOpenAppSettings,
            )
            HorizontalDivider()
            SettingsNavigationRow(
                title = stringResource(R.string.notifications_battery_settings),
                onClick = onOpenBatterySettings,
            )
        }
    }
}

@Composable
private fun localPermissionStatusText(uiState: MainUiState): String {
    return when {
        !uiState.notificationPermissionRequired -> stringResource(R.string.notifications_permission_not_required)
        uiState.notificationsAllowed -> stringResource(R.string.notifications_permission_granted)
        else -> stringResource(R.string.notifications_permission_denied)
    }
}

@Composable
private fun localLastResultText(result: LocalNotificationResult): String {
    return when (result) {
        LocalNotificationResult.Success -> stringResource(R.string.notifications_local_result_success)
        LocalNotificationResult.Disabled -> stringResource(R.string.notifications_local_result_disabled)
        LocalNotificationResult.PermissionNotGranted -> stringResource(R.string.notifications_local_result_no_permission)
        is LocalNotificationResult.Failure -> stringResource(R.string.notifications_local_result_error)
    }
}
