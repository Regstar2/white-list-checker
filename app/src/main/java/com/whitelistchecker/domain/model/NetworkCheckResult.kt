package com.whitelistchecker.domain.model

data class NetworkCheckResult(
    val siteResults: List<SiteCheckResult>,
    val foreignSummary: TargetGroupSummary,
    val localSummary: TargetGroupSummary,
    val state: WhitelistState,
    val activeNetworkLabel: String,
    val checkedNetworkLabel: String,
    val checkedAtMillis: Long,
    val error: String?,
)
