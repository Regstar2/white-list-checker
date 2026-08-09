package com.whitelistchecker.ui.autocheck

import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.res.stringResource
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.NotificationPolicy
import com.whitelistchecker.ui.components.ScreenScaffold
import com.whitelistchecker.ui.main.MainUiState

private enum class AutoCheckTab {
    BACKGROUND,
    ACTIVE,
}

@Composable
fun AutoCheckScreen(
    uiState: MainUiState,
    onBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onPresetIntervalChange: (Long) -> Unit,
    onUseCustomIntervalChange: (Boolean) -> Unit,
    onCustomIntervalInputChange: (String) -> Unit,
    onBackgroundNotificationPolicyChange: (NotificationPolicy) -> Unit,
    onSaveAndReschedule: () -> Unit,
    onRunNow: () -> Unit,
    onActiveIntervalInputChange: (String) -> Unit,
    onSaveActiveInterval: () -> Unit,
    onActiveNotificationPolicyChange: (NotificationPolicy) -> Unit,
    onNotifyOnAccessRestoredChange: (Boolean) -> Unit,
    onTelegramCommandsEnabledChange: (Boolean) -> Unit,
    onStartActiveMonitoring: () -> Unit,
    onStopActiveMonitoring: () -> Unit,
    onRunActiveCheckNow: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(AutoCheckTab.BACKGROUND) }

    ScreenScaffold(title = stringResource(R.string.autocheck_title), onBack = onBack) {
        TabRow(selectedTabIndex = AutoCheckTab.entries.indexOf(selectedTab)) {
            AutoCheckTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { selectedTab = tab },
                    text = { Text(stringResource(tab.titleRes())) },
                )
            }
        }

        when (selectedTab) {
            AutoCheckTab.BACKGROUND -> BackgroundCheckTab(
                uiState = uiState,
                onEnabledChange = onEnabledChange,
                onPresetIntervalChange = onPresetIntervalChange,
                onUseCustomIntervalChange = onUseCustomIntervalChange,
                onCustomIntervalInputChange = onCustomIntervalInputChange,
                onNotificationPolicyChange = onBackgroundNotificationPolicyChange,
                onSave = onSaveAndReschedule,
                onRunNow = onRunNow,
            )
            AutoCheckTab.ACTIVE -> ActiveMonitoringTab(
                uiState = uiState,
                onIntervalInputChange = onActiveIntervalInputChange,
                onSaveInterval = onSaveActiveInterval,
                onNotificationPolicyChange = onActiveNotificationPolicyChange,
                onNotifyOnAccessRestoredChange = onNotifyOnAccessRestoredChange,
                onTelegramCommandsEnabledChange = onTelegramCommandsEnabledChange,
                onStart = onStartActiveMonitoring,
                onStop = onStopActiveMonitoring,
                onRunCheckNow = onRunActiveCheckNow,
            )
        }
    }
}

private fun AutoCheckTab.titleRes(): Int {
    return when (this) {
        AutoCheckTab.BACKGROUND -> R.string.autocheck_tab_background
        AutoCheckTab.ACTIVE -> R.string.autocheck_tab_active
    }
}
