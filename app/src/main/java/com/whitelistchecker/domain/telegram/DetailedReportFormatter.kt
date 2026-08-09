package com.whitelistchecker.domain.telegram

import com.whitelistchecker.domain.model.DnsCheckErrorType
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.SiteCheckErrorType
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.ui.toDisplayDateTime

class DetailedReportFormatter {

    fun formatCheckResult(result: NetworkCheckResult): String {
        val foreign = result.foreignSummary
        val local = result.localSummary
        return buildString {
            appendLine("Whitelist Checker — подробный отчёт")
            appendLine()
            appendLine("Итоговое состояние: ${result.state.name}")
            appendLine("Site signal: ${result.siteState.name}")
            appendLine("DNS signal: ${result.dnsSignal.name}")
            appendLine("Проверяемая сеть: ${result.checkedNetworkLabel}")
            appendLine("Активная сеть: ${result.activeNetworkLabel}")
            appendLine("Private DNS: ${if (result.privateDnsActive) "active" else "inactive"}")
            result.privateDnsServerName?.let { serverName ->
                appendLine("Private DNS server: $serverName")
            }
            appendLine("Custom DNS: ${if (result.customDnsUsed) "used" else "not used"}")
            appendLine("Внешние сайты: ${foreign.availableCount}/${foreign.totalCount}")
            appendLine("Локальные сайты: ${local.availableCount}/${local.totalCount}")
            result.foreignDnsSummary?.let { summary ->
                appendLine("Внешние DNS: ${summary.availableCount}/${summary.totalCount}")
            }
            result.localDnsSummary?.let { summary ->
                appendLine("Локальные DNS: ${summary.availableCount}/${summary.totalCount}")
            }
            appendLine("Время: ${result.checkedAtMillis.toDisplayDateTime()}")
            result.diagnosticsMessage?.let { diagnostics ->
                appendLine("Диагностика TCP: $diagnostics")
            }
            result.error?.let { error ->
                appendLine("Ошибка: $error")
            }
            appendLine()
            appendDnsResults(result)
            appendLine()
            appendSiteResults(result)
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

    private fun StringBuilder.appendDnsResults(result: NetworkCheckResult) {
        if (result.dnsResults.isEmpty()) {
            appendLine("DNS-проверка не выполнена.")
            return
        }
        appendLine("DNS:")
        result.dnsResults.groupBy { it.server.group }.forEach { (group, dnsResults) ->
            appendLine(dnsGroupLabel(group))
            dnsResults.forEach { dns ->
                appendLine("- ${dns.server.name} (${dns.server.address}:${dns.server.port})")
                appendLine("  Протокол: ${dns.server.protocol.name}")
                appendLine("  Статус: ${if (dns.available) "доступен" else "недоступен"}")
                appendLine("  Время: ${dns.responseTimeMs} мс")
                appendLine("  Адресов в ответе: ${dns.resolvedAddressesCount}")
                if (dns.errorType != DnsCheckErrorType.NONE) {
                    appendLine("  Тип ошибки: ${dns.errorType.name}")
                    appendLine("  Ошибка: ${dns.error ?: "—"}")
                }
            }
        }
    }

    private fun StringBuilder.appendSiteResults(result: NetworkCheckResult) {
        if (result.siteResults.isEmpty()) {
            appendLine("Проверка сайтов не выполнена.")
            return
        }
        appendLine("Сайты:")
        result.siteResults.groupBy { it.target.group }.forEach { (group, sites) ->
            appendLine(siteGroupLabel(group))
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

    private fun siteGroupLabel(group: TargetGroup): String = when (group) {
        TargetGroup.FOREIGN -> "Внешние сайты"
        TargetGroup.LOCAL -> "Локальные сайты"
    }

    private fun dnsGroupLabel(group: TargetGroup): String = when (group) {
        TargetGroup.FOREIGN -> "Внешние DNS"
        TargetGroup.LOCAL -> "Локальные DNS"
    }
}
