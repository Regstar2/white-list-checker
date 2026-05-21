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
        val unavailableBlock = buildUnavailableBlock(checkResult)
        return buildString {
            appendLine("📊 <b>Результат проверки мобильной сети</b>")
            appendLine()
            appendLine("<b>Проверяемая сеть:</b> ${checkResult.checkedNetworkLabel}")
            appendLine("<b>Активная сеть телефона:</b> ${checkResult.activeNetworkLabel}")
            appendLine()
            appendLine("<b>Внешние сайты:</b> ${foreign.availableCount}/${foreign.totalCount} доступно")
            appendLine("<b>Локальные сайты:</b> ${local.availableCount}/${local.totalCount} доступно")
            appendLine()
            appendLine("<b>Текущее состояние:</b> ${plainLabel(checkResult.state)}")
            appendDnsHintIfNeeded(checkResult)
            appendLine()
            appendLine("<b>Время:</b> ${checkResult.checkedAtMillis.toDisplayDateTime()}")
            checkResult.diagnosticsMessage?.let { diagnostics ->
                appendLine()
                appendLine("<b>Диагностика:</b> $diagnostics")
            }
            if (unavailableBlock.isNotBlank()) {
                appendLine()
                append(unavailableBlock)
            }
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
        val unavailableBlock = buildUnavailableBlock(checkResult)
        return buildString {
            appendLine(header)
            appendLine()
            appendLine("<b>Проверяемая сеть:</b> ${checkResult.checkedNetworkLabel}")
            appendLine("<b>Активная сеть телефона:</b> ${checkResult.activeNetworkLabel}")
            appendLine()
            appendLine("<b>Внешние сайты:</b> ${foreign.availableCount}/${foreign.totalCount} доступно")
            appendLine("<b>Локальные сайты:</b> ${local.availableCount}/${local.totalCount} доступно")
            appendLine()
            appendLine("<b>Было:</b> ${plainLabel(event.oldState)}")
            appendLine("<b>Стало:</b> ${plainLabel(event.newState)}")
            appendDnsHintIfNeeded(checkResult)
            appendLine()
            appendLine("<b>Время:</b> ${event.changedAtMillis.toDisplayDateTime()}")
            checkResult.diagnosticsMessage?.let { diagnostics ->
                appendLine()
                appendLine("<b>Диагностика:</b> $diagnostics")
            }
            if (unavailableBlock.isNotBlank()) {
                appendLine()
                append(unavailableBlock)
            }
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

    private fun StringBuilder.appendDnsHintIfNeeded(checkResult: NetworkCheckResult) {
        if (checkResult.state != WhitelistState.MOBILE_DNS_FAILURE) return
        appendLine()
        appendLine(
            "Похоже на проблему DNS в мобильной сети: домены не резолвятся через cellular Network. " +
                "Проверьте Private DNS, APN и работу сайтов при отключённом Wi-Fi.",
        )
    }

    private fun buildUnavailableBlock(checkResult: NetworkCheckResult): String {
        val unavailable = checkResult.siteResults.filter { !it.available }
        if (unavailable.isEmpty()) return ""
        return buildString {
            appendLine("<b>Недоступны:</b>")
            unavailable.forEach { site ->
                val error = site.error ?: "HTTP ${site.httpCode ?: "—"}"
                appendLine("- ${site.target.name}: $error")
            }
        }.trimEnd()
    }
}
