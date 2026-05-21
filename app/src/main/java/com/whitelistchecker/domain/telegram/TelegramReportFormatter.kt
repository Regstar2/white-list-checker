package com.whitelistchecker.domain.telegram

import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.domain.model.WhitelistStateChangeType
import com.whitelistchecker.ui.toDisplayDateTime

class TelegramReportFormatter {

    fun formatManualCheck(checkResult: NetworkCheckResult): String {
        val foreign = checkResult.foreignSummary
        val local = checkResult.localSummary
        return buildString {
            appendLine("📊 <b>Проверка мобильной сети</b>")
            appendLine()
            appendLine("<b>Состояние:</b> ${plainLabel(checkResult.state)}")
            appendLine("<b>Внешние:</b> ${foreign.availableCount}/${foreign.totalCount}")
            appendLine("<b>Локальные:</b> ${local.availableCount}/${local.totalCount}")
            appendLine("<b>Проверка:</b> ${checkResult.checkedNetworkLabel}")
            appendLine("<b>Время:</b> ${checkResult.checkedAtMillis.toDisplayDateTime()}")
        }.trim()
    }

    fun format(event: WhitelistStateChangeEvent, checkResult: NetworkCheckResult): String {
        val foreign = checkResult.foreignSummary
        val local = checkResult.localSummary
        val header = when (event.type) {
            WhitelistStateChangeType.WHITELIST_TURNED_ON ->
                "🟠 <b>Белые списки включились</b>"
            WhitelistStateChangeType.WHITELIST_TURNED_OFF ->
                "🟢 <b>Белые списки выключились</b>"
            else -> "⚪ <b>Состояние сети изменилось</b>"
        }
        if (checkResult.state == WhitelistState.MOBILE_DNS_FAILURE) {
            return buildString {
                appendLine("🟡 <b>Проблема DNS в мобильной сети</b>")
                appendLine()
                appendLine("Домены не резолвятся через Mobile.")
                appendLine("Проверь Private DNS/APN.")
                appendLine("<b>Время:</b> ${event.changedAtMillis.toDisplayDateTime()}")
            }.trim()
        }
        return buildString {
            appendLine(header)
            appendLine()
            appendLine("<b>Внешние:</b> ${foreign.availableCount}/${foreign.totalCount}")
            appendLine("<b>Локальные:</b> ${local.availableCount}/${local.totalCount}")
            appendLine("<b>Проверка:</b> ${checkResult.checkedNetworkLabel}")
            appendLine("<b>Время:</b> ${event.changedAtMillis.toDisplayDateTime()}")
        }.trim()
    }

    private fun plainLabel(state: WhitelistState): String = when (state) {
        WhitelistState.UNKNOWN -> "Неизвестное состояние"
        WhitelistState.WHITELIST_OFF -> "Белые списки не обнаружены"
        WhitelistState.WHITELIST_ON -> "Похоже на включённые белые списки"
        WhitelistState.NO_MOBILE_INTERNET -> "Мобильного интернета нет"
        WhitelistState.MOBILE_DNS_FAILURE -> "Проблема DNS в мобильной сети"
        WhitelistState.PARTIAL_PROBLEM -> "Частичная проблема сети"
        WhitelistState.CELLULAR_NETWORK_UNAVAILABLE -> "Мобильная сеть недоступна"
    }
}
