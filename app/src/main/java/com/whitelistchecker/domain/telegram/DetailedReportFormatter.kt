package com.whitelistchecker.domain.telegram

import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.SiteCheckErrorType
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.ui.toDisplayDateTime

class DetailedReportFormatter {

    fun formatCheckResult(result: NetworkCheckResult): String {
        val foreign = result.foreignSummary
        val local = result.localSummary
        return buildString {
            appendLine("Whitelist Checker — подробный отчёт")
            appendLine()
            appendLine("Состояние: ${result.state.name}")
            appendLine("Проверяемая сеть: ${result.checkedNetworkLabel}")
            appendLine("Активная сеть: ${result.activeNetworkLabel}")
            appendLine("Внешние: ${foreign.availableCount}/${foreign.totalCount}")
            appendLine("Локальные: ${local.availableCount}/${local.totalCount}")
            appendLine("Время: ${result.checkedAtMillis.toDisplayDateTime()}")
            result.diagnosticsMessage?.let { diagnostics ->
                appendLine("Диагностика TCP: $diagnostics")
            }
            result.error?.let { error ->
                appendLine("Ошибка: $error")
            }
            appendLine()
            appendUnavailableSites(result)
        }.trim()
    }

    fun formatStateChange(event: WhitelistStateChangeEvent, result: NetworkCheckResult): String {
        return buildString {
            appendLine(formatCheckResult(result))
            appendLine()
            appendLine("Событие: ${event.type.name}")
            appendLine("Было: ${event.oldState.name}")
            appendLine("Стало: ${event.newState.name}")
            appendLine("Время события: ${event.changedAtMillis.toDisplayDateTime()}")
        }.trim()
    }

    private fun StringBuilder.appendUnavailableSites(result: NetworkCheckResult) {
        if (result.siteResults.isEmpty()) {
            appendLine("Проверка сайтов не выполнена.")
            return
        }
        appendLine("Сайты:")
        result.siteResults.groupBy { it.target.group }.forEach { (group, sites) ->
            appendLine(groupLabel(group))
            sites.forEach { site ->
                appendLine("- ${site.target.name} (${site.target.url})")
                appendLine("  Статус: ${if (site.available) "доступен" else "недоступен"}")
                appendLine("  HTTP: ${site.httpCode ?: "—"}")
                if (site.errorType != SiteCheckErrorType.NONE) {
                    appendLine("  Тип ошибки: ${site.errorType.name}")
                }
                appendLine("  Ошибка: ${site.error ?: "—"}")
                appendLine("  Время: ${site.durationMs} мс")
            }
        }
        val unavailable = result.siteResults.filter { !it.available }
        if (unavailable.isNotEmpty()) {
            appendLine()
            appendLine("Недоступны:")
            unavailable.forEach { site ->
                appendLine("- ${site.target.name}: ${site.error ?: "HTTP ${site.httpCode ?: "—"}"}")
            }
        }
    }

    private fun groupLabel(group: TargetGroup): String = when (group) {
        TargetGroup.FOREIGN -> "Внешние сайты"
        TargetGroup.LOCAL -> "Локальные сайты"
    }
}
