package com.whitelistchecker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.WhitelistStateChangeType

@Composable
fun WhitelistState.toDisplayLabel(): String = when (this) {
    WhitelistState.UNKNOWN -> stringResource(R.string.whitelist_state_display_unknown)
    WhitelistState.WHITELIST_OFF -> stringResource(R.string.whitelist_state_display_whitelist_off)
    WhitelistState.WHITELIST_ON -> stringResource(R.string.whitelist_state_display_whitelist_on)
    WhitelistState.NO_MOBILE_INTERNET -> stringResource(R.string.whitelist_state_display_no_mobile_internet)
    WhitelistState.MOBILE_DNS_FAILURE -> stringResource(R.string.dns_issue_display_label)
    WhitelistState.PARTIAL_PROBLEM -> stringResource(R.string.whitelist_state_display_partial_problem)
    WhitelistState.CELLULAR_NETWORK_UNAVAILABLE -> stringResource(R.string.whitelist_state_display_cellular_unavailable)
}

@Composable
fun WhitelistState.toPlainLabel(): String = when (this) {
    WhitelistState.UNKNOWN -> stringResource(R.string.diagnostics_state_unknown)
    WhitelistState.WHITELIST_OFF -> stringResource(R.string.diagnostics_state_whitelist_off)
    WhitelistState.WHITELIST_ON -> stringResource(R.string.diagnostics_state_whitelist_on)
    WhitelistState.NO_MOBILE_INTERNET -> stringResource(R.string.diagnostics_state_no_mobile_internet)
    WhitelistState.MOBILE_DNS_FAILURE -> stringResource(R.string.dns_issue_plain_label)
    WhitelistState.PARTIAL_PROBLEM -> stringResource(R.string.diagnostics_state_partial_problem)
    WhitelistState.CELLULAR_NETWORK_UNAVAILABLE -> stringResource(R.string.diagnostics_state_cellular_unavailable)
}

@Composable
fun WhitelistState.toDescription(): String? = when (this) {
    WhitelistState.MOBILE_DNS_FAILURE -> stringResource(R.string.dns_issue_description)
    WhitelistState.CELLULAR_NETWORK_UNAVAILABLE -> stringResource(R.string.whitelist_state_description_cellular_unavailable)
    else -> null
}

@Composable
fun WhitelistStateChangeType.toEventTitle(): String = when (this) {
    WhitelistStateChangeType.WHITELIST_TURNED_ON -> stringResource(R.string.whitelist_event_display_turned_on)
    WhitelistStateChangeType.WHITELIST_TURNED_OFF -> stringResource(R.string.whitelist_event_display_turned_off)
    WhitelistStateChangeType.MANUAL_CHECK -> stringResource(R.string.whitelist_event_display_manual_check)
    WhitelistStateChangeType.TEST_MESSAGE -> stringResource(R.string.whitelist_event_display_test_message)
    WhitelistStateChangeType.OTHER_CONFIRMED_CHANGE -> stringResource(R.string.whitelist_event_display_other_confirmed_change)
    WhitelistStateChangeType.NO_CONFIRMED_CHANGE -> ""
}
