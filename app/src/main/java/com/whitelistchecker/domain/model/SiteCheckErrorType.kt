package com.whitelistchecker.domain.model

enum class SiteCheckErrorType {
    NONE,
    DNS,
    TIMEOUT,
    CONNECTION,
    TLS,
    HTTP,
    UNKNOWN,
}
