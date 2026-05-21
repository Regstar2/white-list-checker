package com.whitelistchecker.domain.notifications

import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.domain.model.WhitelistStateChangeType

class LocalNotificationFormatter {

    fun conclusionFor(state: WhitelistState): String = when (state) {
        WhitelistState.WHITELIST_OFF -> "Белые списки не обнаружены"
        WhitelistState.WHITELIST_ON -> "Похоже на включённые белые списки"
        WhitelistState.NO_MOBILE_INTERNET -> "Мобильного интернета нет"
        WhitelistState.PARTIAL_PROBLEM -> "Частичная проблема сети"
        WhitelistState.CELLULAR_NETWORK_UNAVAILABLE -> "Мобильная сеть недоступна"
        WhitelistState.UNKNOWN -> "Состояние не определено"
    }

    fun checkSummaryLine(checkResult: NetworkCheckResult): String {
        val foreign = checkResult.foreignSummary
        val local = checkResult.localSummary
        return "Внешние: ${foreign.availableCount}/${foreign.totalCount}, " +
            "локальные: ${local.availableCount}/${local.totalCount}"
    }

    fun textForCheckResult(checkResult: NetworkCheckResult): String {
        return "${conclusionFor(checkResult.state)}. ${checkSummaryLine(checkResult)}."
    }

    fun titleFor(event: WhitelistStateChangeEvent): String = when (event.type) {
        WhitelistStateChangeType.WHITELIST_TURNED_ON -> "Белые списки включились"
        WhitelistStateChangeType.WHITELIST_TURNED_OFF -> "Белые списки выключились"
        WhitelistStateChangeType.OTHER_CONFIRMED_CHANGE -> "Состояние сети изменилось"
        WhitelistStateChangeType.NO_CONFIRMED_CHANGE -> ""
    }

    fun textFor(
        event: WhitelistStateChangeEvent,
        checkResult: NetworkCheckResult,
    ): String {
        val conclusion = conclusionFor(checkResult.state)
        val summary = checkSummaryLine(checkResult)
        return when (event.type) {
            WhitelistStateChangeType.WHITELIST_TURNED_ON,
            WhitelistStateChangeType.WHITELIST_TURNED_OFF,
            -> "$conclusion. $summary."
            WhitelistStateChangeType.OTHER_CONFIRMED_CHANGE -> "$conclusion. $summary."
            WhitelistStateChangeType.NO_CONFIRMED_CHANGE -> ""
        }
    }
}
