package com.whitelistchecker.domain.model

enum class WhitelistState {
    UNKNOWN,
    WHITELIST_OFF,
    WHITELIST_ON,
    NO_MOBILE_INTERNET,
    MOBILE_DNS_FAILURE,
    PARTIAL_PROBLEM,
    CELLULAR_NETWORK_UNAVAILABLE,
}
