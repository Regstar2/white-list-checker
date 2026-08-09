package com.whitelistchecker.ui.autocheck

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.ActiveMonitoringState
import com.whitelistchecker.domain.model.BackgroundCheckInterval
import com.whitelistchecker.domain.model.NotificationPolicy
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.ui.components.StatusTone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun NotificationPolicySelector(
    selectedPolicy: NotificationPolicy,
    onPolicyChange: (NotificationPolicy) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        NotificationPolicy.entries.forEach { policy ->
            FilterChip(
                selected = selectedPolicy == policy,
                onClick = { onPolicyChange(policy) },
                label = { Text(stringResource(policy.labelRes())) },
            )
        }
    }
}

@Composable
internal fun SwitchSettingRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@StringRes
internal fun BackgroundCheckInterval.labelRes(): Int {
    return when (this) {
        BackgroundCheckInterval.FIFTEEN_MINUTES -> R.string.autocheck_interval_15
        BackgroundCheckInterval.THIRTY_MINUTES -> R.string.autocheck_interval_30
        BackgroundCheckInterval.SIXTY_MINUTES -> R.string.autocheck_interval_60
    }
}

@StringRes
internal fun NotificationPolicy.labelRes(): Int {
    return when (this) {
        NotificationPolicy.NONE -> R.string.autocheck_policy_none
        NotificationPolicy.EVERY_ATTEMPT -> R.string.autocheck_policy_every_attempt
        NotificationPolicy.STATE_CHANGE_ONLY -> R.string.autocheck_policy_state_change_only
    }
}

@StringRes
internal fun ActiveMonitoringState.labelRes(): Int {
    return when (this) {
        ActiveMonitoringState.STOPPED -> R.string.autocheck_active_state_stopped
        ActiveMonitoringState.STARTING -> R.string.autocheck_active_state_starting
        ActiveMonitoringState.RUNNING -> R.string.autocheck_active_state_running
        ActiveMonitoringState.CHECKING -> R.string.autocheck_active_state_checking
        ActiveMonitoringState.STOPPING -> R.string.autocheck_active_state_stopping
        ActiveMonitoringState.STOPPED_BY_SYSTEM -> R.string.autocheck_active_state_stopped_by_system
        ActiveMonitoringState.ERROR -> R.string.autocheck_active_state_error
    }
}

internal fun ActiveMonitoringState.toTone(): StatusTone {
    return when (this) {
        ActiveMonitoringState.RUNNING,
        ActiveMonitoringState.CHECKING,
        -> StatusTone.SUCCESS
        ActiveMonitoringState.STARTING,
        ActiveMonitoringState.STOPPING,
        ActiveMonitoringState.STOPPED_BY_SYSTEM,
        -> StatusTone.WARNING
        ActiveMonitoringState.ERROR -> StatusTone.ERROR
        ActiveMonitoringState.STOPPED -> StatusTone.NEUTRAL
    }
}

@StringRes
internal fun WhitelistState.labelRes(): Int {
    return when (this) {
        WhitelistState.UNKNOWN -> R.string.home_result_state_unknown
        WhitelistState.WHITELIST_OFF -> R.string.home_result_state_whitelist_off
        WhitelistState.WHITELIST_ON -> R.string.home_result_state_whitelist_on
        WhitelistState.NO_MOBILE_INTERNET -> R.string.home_result_state_no_mobile_internet
        WhitelistState.MOBILE_DNS_FAILURE -> R.string.home_result_state_mobile_dns_failure
        WhitelistState.PARTIAL_PROBLEM -> R.string.home_result_state_partial_problem
        WhitelistState.CELLULAR_NETWORK_UNAVAILABLE -> R.string.home_result_state_cellular_unavailable
    }
}

internal fun WhitelistState.toTone(): StatusTone {
    return when (this) {
        WhitelistState.WHITELIST_OFF -> StatusTone.SUCCESS
        WhitelistState.WHITELIST_ON,
        WhitelistState.NO_MOBILE_INTERNET,
        WhitelistState.MOBILE_DNS_FAILURE,
        WhitelistState.PARTIAL_PROBLEM,
        WhitelistState.CELLULAR_NETWORK_UNAVAILABLE,
        -> StatusTone.WARNING
        WhitelistState.UNKNOWN -> StatusTone.NEUTRAL
    }
}
