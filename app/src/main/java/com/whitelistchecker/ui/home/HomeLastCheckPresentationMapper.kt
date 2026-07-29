package com.whitelistchecker.ui.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.whitelistchecker.R
import com.whitelistchecker.domain.model.LastCheckDisplayState
import com.whitelistchecker.domain.model.LastCheckFreshness
import com.whitelistchecker.domain.model.LastCheckOutcome
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistState

object HomeLastCheckPresentationMapper {

    fun map(displayState: LastCheckDisplayState): HomeLastCheckUiModel {
        return when (displayState) {
            LastCheckDisplayState.NoCheck -> HomeLastCheckUiModel(
                headlineRes = R.string.home_result_no_check_title,
                bodyRes = R.string.home_result_no_check_body,
                iconRes = R.drawable.ic_home_info,
                tone = HomeResultTone.NEUTRAL,
            )
            LastCheckDisplayState.Running -> HomeLastCheckUiModel(
                headlineRes = R.string.home_result_running_title,
                bodyRes = R.string.home_result_running_body,
                iconRes = R.drawable.ic_home_sync,
                tone = HomeResultTone.NEUTRAL,
            )
            LastCheckDisplayState.LoadError -> HomeLastCheckUiModel(
                headlineRes = R.string.home_result_load_error_title,
                bodyRes = R.string.home_result_load_error_body,
                iconRes = R.drawable.ic_home_error,
                tone = HomeResultTone.ERROR,
            )
            is LastCheckDisplayState.Available -> {
                val result = displayState.result
                HomeLastCheckUiModel(
                    headlineRes = result.state.toHeadlineRes(),
                    iconRes = result.state.toIconRes(displayState.outcome),
                    tone = result.state.toTone(displayState.outcome),
                    checkedAtMillis = result.checkedAtMillis,
                    localCount = result.localSummary.toGroupCount(),
                    foreignCount = result.foreignSummary.toGroupCount(),
                    route = HomeNetworkRouteUiModel.from(
                        checkedNetworkLabel = result.checkedNetworkLabel,
                        activeNetworkLabel = result.activeNetworkLabel,
                    ),
                    error = result.error,
                    stale = displayState.freshness == LastCheckFreshness.STALE,
                    showDetails = true,
                )
            }
        }
    }

    private fun TargetGroupSummary.toGroupCount(): HomeGroupCountUiModel {
        return HomeGroupCountUiModel(
            availableCount = availableCount,
            totalCount = totalCount,
        )
    }

    @StringRes
    private fun WhitelistState.toHeadlineRes(): Int {
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

    @DrawableRes
    private fun WhitelistState.toIconRes(outcome: LastCheckOutcome): Int {
        if (outcome == LastCheckOutcome.FAILURE) return R.drawable.ic_home_error
        return when (toTone(outcome)) {
            HomeResultTone.SUCCESS -> R.drawable.ic_home_check_circle
            HomeResultTone.WARNING -> R.drawable.ic_home_warning
            HomeResultTone.ERROR -> R.drawable.ic_home_error
            HomeResultTone.NEUTRAL -> R.drawable.ic_home_info
        }
    }

    private fun WhitelistState.toTone(outcome: LastCheckOutcome): HomeResultTone {
        if (outcome == LastCheckOutcome.FAILURE) return HomeResultTone.ERROR
        return when (this) {
            WhitelistState.WHITELIST_OFF -> HomeResultTone.SUCCESS
            WhitelistState.WHITELIST_ON,
            WhitelistState.MOBILE_DNS_FAILURE,
            WhitelistState.PARTIAL_PROBLEM,
            -> HomeResultTone.WARNING
            WhitelistState.NO_MOBILE_INTERNET,
            WhitelistState.CELLULAR_NETWORK_UNAVAILABLE,
            -> HomeResultTone.ERROR
            WhitelistState.UNKNOWN -> HomeResultTone.NEUTRAL
        }
    }
}

data class HomeLastCheckUiModel(
    @StringRes val headlineRes: Int,
    @StringRes val bodyRes: Int? = null,
    @DrawableRes val iconRes: Int,
    val tone: HomeResultTone,
    val checkedAtMillis: Long? = null,
    val localCount: HomeGroupCountUiModel? = null,
    val foreignCount: HomeGroupCountUiModel? = null,
    val route: HomeNetworkRouteUiModel? = null,
    val error: String? = null,
    val stale: Boolean = false,
    val showDetails: Boolean = false,
)

data class HomeGroupCountUiModel(
    val availableCount: Int,
    val totalCount: Int,
)

enum class HomeResultTone {
    SUCCESS,
    WARNING,
    ERROR,
    NEUTRAL,
}

data class HomeNetworkRouteUiModel(
    @StringRes val textRes: Int,
    val checkedNetwork: HomeNetworkLabelUiModel? = null,
    val activeNetwork: HomeNetworkLabelUiModel? = null,
) {
    companion object {
        fun from(
            checkedNetworkLabel: String,
            activeNetworkLabel: String,
        ): HomeNetworkRouteUiModel {
            val checked = HomeNetworkLabelUiModel.from(checkedNetworkLabel)
            val active = HomeNetworkLabelUiModel.from(activeNetworkLabel)
            return when {
                checked == HomeNetworkLabelUiModel.MOBILE && active == HomeNetworkLabelUiModel.WIFI ->
                    HomeNetworkRouteUiModel(R.string.home_result_route_mobile_wifi)
                checked == HomeNetworkLabelUiModel.MOBILE ->
                    HomeNetworkRouteUiModel(R.string.home_result_route_mobile)
                else -> HomeNetworkRouteUiModel(
                    textRes = R.string.home_result_route_generic,
                    checkedNetwork = checked,
                    activeNetwork = active,
                )
            }
        }
    }
}

sealed class HomeNetworkLabelUiModel {
    data object MOBILE : HomeNetworkLabelUiModel()
    data object WIFI : HomeNetworkLabelUiModel()
    data object ETHERNET : HomeNetworkLabelUiModel()
    data object UNKNOWN : HomeNetworkLabelUiModel()
    data class Raw(val value: String) : HomeNetworkLabelUiModel()

    companion object {
        fun from(label: String): HomeNetworkLabelUiModel {
            return when (label.trim().lowercase()) {
                "mobile", "cellular", "мобильная сеть", "мобильная" -> MOBILE
                "wi-fi", "wifi", "вай-фай" -> WIFI
                "ethernet" -> ETHERNET
                "unknown", "неизвестно", "" -> UNKNOWN
                else -> Raw(label)
            }
        }
    }
}
