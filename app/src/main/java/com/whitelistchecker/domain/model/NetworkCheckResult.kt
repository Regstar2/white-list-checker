package com.whitelistchecker.domain.model

data class NetworkCheckResult(
    val google: SiteCheckResult?,
    val yandex: SiteCheckResult?,
    val state: WhitelistState,
    val activeNetworkLabel: String,
    val checkedNetworkLabel: String,
    val checkedAtMillis: Long,
    val error: String?,
)
