package com.whitelistchecker.domain.telegram

import com.whitelistchecker.domain.checkrun.NotificationDecision
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

    fun formatDecision(
        decision: NotificationDecision,
        checkResult: NetworkCheckResult?,
    ): String {
        return when (decision) {
            is NotificationDecision.AttemptResult -> checkResult?.let(::formatManualCheck)
                ?: "<b>Результат проверки</b>\n\n${decision.currentState?.let(::plainLabel) ?: "Неизвестно"}"
            is NotificationDecision.StateChanged -> formatStateChanged(decision.newState, checkResult)
            is NotificationDecision.AccessRestored -> buildString {
                appendLine("<b>Проверка снова доступна</b>")
                appendLine()
                if (checkResult != null) {
                    append(formatManualCheck(checkResult))
                } else {
                    append(decision.currentState?.let(::plainLabel) ?: "Результат неизвестен")
                }
            }.trim()
            is NotificationDecision.AccessRestoredAndStateChanged -> buildString {
                appendLine("<b>Проверка снова доступна</b>")
                appendLine()
                appendLine("Состояние изменилось: ${plainLabel(decision.newState)}")
                if (checkResult != null) {
                    appendLine()
                    append(formatManualCheck(checkResult))
                }
            }.trim()
            is NotificationDecision.AttemptUnavailable -> buildString {
                appendLine("<b>Проверка недоступна</b>")
                appendLine()
                appendLine(plainLabel(decision.state))
                appendLine("Последний валидный статус белых списков не изменён.")
            }.trim()
            is NotificationDecision.AttemptFailed -> buildString {
                appendLine("<b>Ошибка проверки</b>")
                appendLine()
                appendLine(decision.error)
            }.trim()
            NotificationDecision.None -> ""
        }
    }

    private fun formatStateChanged(
        newState: WhitelistState,
        checkResult: NetworkCheckResult?,
    ): String {
        if (checkResult != null) {
            return buildString {
                appendLine("<b>Состояние белых списков изменилось</b>")
                appendLine()
                append(formatManualCheck(checkResult))
            }.trim()
        }
        return "<b>Состояние белых списков изменилось</b>\n\n${plainLabel(newState)}"
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
