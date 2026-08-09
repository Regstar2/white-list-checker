package com.whitelistchecker.domain.model

data class NetworkCheckResult(
    val siteResults: List<SiteCheckResult>,
    val foreignSummary: TargetGroupSummary,
    val localSummary: TargetGroupSummary,
    val state: WhitelistState,
    val activeNetworkLabel: String,
    val checkedNetworkLabel: String,
    val checkedAtMillis: Long,
    val error: String? = null,
    val diagnosticsMessage: String? = null,
    val dnsResults: List<DnsCheckResult> = emptyList(),
    val foreignDnsSummary: TargetGroupSummary? = null,
    val localDnsSummary: TargetGroupSummary? = null,
    val dnsSignal: DnsWhitelistSignal = DnsWhitelistSignal.UNKNOWN,
    val siteState: WhitelistState = state,
    val privateDnsActive: Boolean = false,
    val privateDnsServerName: String? = null,
    val customDnsUsed: Boolean = false,
)
