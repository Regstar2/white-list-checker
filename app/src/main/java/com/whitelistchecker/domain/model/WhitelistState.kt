package com.whitelistchecker.domain.model

enum class WhitelistState {
    UNKNOWN,
    WHITELIST_OFF,
    WHITELIST_ON,
    NO_MOBILE_INTERNET,
    PARTIAL_PROBLEM,
    CELLULAR_NETWORK_UNAVAILABLE,
}
