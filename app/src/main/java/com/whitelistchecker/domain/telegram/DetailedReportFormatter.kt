package com.whitelistchecker.domain.telegram

import com.whitelistchecker.domain.model.DnsCheckErrorType
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.SiteCheckErrorType
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.ui.toDisplayDateTime

class DetailedReportFormatter(
    private val textProvider: DetailedReportTextProvider,
) {

    fun formatCheckResult(result: NetworkCheckResult): String {
        val foreign = result.foreignSummary
        val local = result.localSummary
        return buildString {
            appendLine(text(DetailedReportTextKey.TITLE))
            appendLine()
            appendLine(text(DetailedReportTextKey.FINAL_STATE, result.state.name))
            appendLine(text(DetailedReportTextKey.SITE_SIGNAL, result.siteState.name))
            appendLine(text(DetailedReportTextKey.DNS_SIGNAL, result.dnsSignal.name))
            appendLine(text(DetailedReportTextKey.CHECKED_NETWORK, result.checkedNetworkLabel))
            appendLine(text(DetailedReportTextKey.ACTIVE_NETWORK, result.activeNetworkLabel))
            appendLine(
                text(
                    DetailedReportTextKey.PRIVATE_DNS,
                    text(
                        if (result.privateDnsActive) {
                            DetailedReportTextKey.ACTIVE
                        } else {
                            DetailedReportTextKey.INACTIVE
                        },
                    ),
                ),
            )
            result.privateDnsServerName?.let { serverName ->
                appendLine(text(DetailedReportTextKey.PRIVATE_DNS_SERVER, serverName))
            }
            appendLine(
                text(
                    DetailedReportTextKey.CUSTOM_DNS,
                    text(
                        if (result.customDnsUsed) {
                            DetailedReportTextKey.USED
                        } else {
                            DetailedReportTextKey.NOT_USED
                        },
                    ),
                ),
            )
            appendLine(
                text(
                    DetailedReportTextKey.FOREIGN_SITES_SUMMARY,
                    foreign.availableCount,
                    foreign.totalCount,
                ),
            )
            appendLine(
                text(
                    DetailedReportTextKey.LOCAL_SITES_SUMMARY,
                    local.availableCount,
                    local.totalCount,
                ),
            )
            result.foreignDnsSummary?.let { summary ->
                appendLine(
                    text(
                        DetailedReportTextKey.FOREIGN_DNS_SUMMARY,
                        summary.availableCount,
                        summary.totalCount,
                    ),
                )
            }
            result.localDnsSummary?.let { summary ->
                appendLine(
                    text(
                        DetailedReportTextKey.LOCAL_DNS_SUMMARY,
                        summary.availableCount,
                        summary.totalCount,
                    ),
                )
            }
            appendLine(text(DetailedReportTextKey.CHECK_TIME, result.checkedAtMillis.toDisplayDateTime()))
            result.diagnosticsMessage?.let { diagnostics ->
                appendLine(text(DetailedReportTextKey.TCP_DIAGNOSTICS, diagnostics))
            }
            result.error?.let { error ->
                appendLine(text(DetailedReportTextKey.ERROR, error))
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
            appendLine(text(DetailedReportTextKey.EVENT, event.type.name))
            appendLine(text(DetailedReportTextKey.OLD_STATE, event.oldState.name))
            appendLine(text(DetailedReportTextKey.NEW_STATE, event.newState.name))
            appendLine(text(DetailedReportTextKey.EVENT_TIME, event.changedAtMillis.toDisplayDateTime()))
        }.trim()
    }

    private fun StringBuilder.appendDnsResults(result: NetworkCheckResult) {
        if (result.dnsResults.isEmpty()) {
            appendLine(text(DetailedReportTextKey.DNS_NOT_RUN))
            return
        }
        appendLine(text(DetailedReportTextKey.DNS_HEADER))
        result.dnsResults.groupBy { it.server.group }.forEach { (group, dnsResults) ->
            appendLine(dnsGroupLabel(group))
            dnsResults.forEach { dns ->
                appendLine(
                    text(
                        DetailedReportTextKey.DNS_SERVER,
                        dns.server.name,
                        dns.server.address,
                        dns.server.port,
                    ),
                )
                appendLine(text(DetailedReportTextKey.PROTOCOL, dns.server.protocol.name))
                appendLine(
                    text(
                        DetailedReportTextKey.STATUS,
                        text(
                            if (dns.available) {
                                DetailedReportTextKey.AVAILABLE
                            } else {
                                DetailedReportTextKey.UNAVAILABLE
                            },
                        ),
                    ),
                )
                appendLine(text(DetailedReportTextKey.RESPONSE_TIME, dns.responseTimeMs))
                appendLine(
                    text(
                        DetailedReportTextKey.RESOLVED_ADDRESS_COUNT,
                        dns.resolvedAddressesCount,
                    ),
                )
                if (dns.errorType != DnsCheckErrorType.NONE) {
                    appendLine(text(DetailedReportTextKey.ERROR_TYPE, dns.errorType.name))
                    appendLine(
                        text(
                            DetailedReportTextKey.ERROR,
                            dns.error ?: text(DetailedReportTextKey.NOT_AVAILABLE),
                        ),
                    )
                }
            }
        }
    }

    private fun StringBuilder.appendSiteResults(result: NetworkCheckResult) {
        if (result.siteResults.isEmpty()) {
            appendLine(text(DetailedReportTextKey.SITES_NOT_RUN))
            return
        }
        appendLine(text(DetailedReportTextKey.SITES_HEADER))
        result.siteResults.groupBy { it.target.group }.forEach { (group, sites) ->
            appendLine(siteGroupLabel(group))
            sites.forEach { site ->
                appendLine(text(DetailedReportTextKey.SITE, site.target.name, site.target.url))
                appendLine(
                    text(
                        DetailedReportTextKey.STATUS,
                        text(
                            if (site.available) {
                                DetailedReportTextKey.AVAILABLE
                            } else {
                                DetailedReportTextKey.UNAVAILABLE
                            },
                        ),
                    ),
                )
                appendLine(
                    text(
                        DetailedReportTextKey.HTTP,
                        site.httpCode ?: text(DetailedReportTextKey.NOT_AVAILABLE),
                    ),
                )
                if (site.errorType != SiteCheckErrorType.NONE) {
                    appendLine(text(DetailedReportTextKey.ERROR_TYPE, site.errorType.name))
                }
                appendLine(
                    text(
                        DetailedReportTextKey.ERROR,
                        site.error ?: text(DetailedReportTextKey.NOT_AVAILABLE),
                    ),
                )
                appendLine(text(DetailedReportTextKey.RESPONSE_TIME, site.durationMs))
            }
        }
        val unavailable = result.siteResults.filter { !it.available }
        if (unavailable.isNotEmpty()) {
            appendLine()
            appendLine(text(DetailedReportTextKey.UNAVAILABLE_HEADER))
            unavailable.forEach { site ->
                val detail = site.error ?: text(
                    DetailedReportTextKey.HTTP,
                    site.httpCode ?: text(DetailedReportTextKey.NOT_AVAILABLE),
                ).trim()
                appendLine(text(DetailedReportTextKey.UNAVAILABLE_SITE, site.target.name, detail))
            }
        }
    }

    private fun siteGroupLabel(group: TargetGroup): String = text(
        when (group) {
            TargetGroup.FOREIGN -> DetailedReportTextKey.FOREIGN_SITES_GROUP
            TargetGroup.LOCAL -> DetailedReportTextKey.LOCAL_SITES_GROUP
        },
    )

    private fun dnsGroupLabel(group: TargetGroup): String = text(
        when (group) {
            TargetGroup.FOREIGN -> DetailedReportTextKey.FOREIGN_DNS_GROUP
            TargetGroup.LOCAL -> DetailedReportTextKey.LOCAL_DNS_GROUP
        },
    )

    private fun text(
        key: DetailedReportTextKey,
        vararg args: Any,
    ): String = textProvider.text(key, *args)
}
