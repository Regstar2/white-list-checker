package com.whitelistchecker.domain.model.history

enum class CheckTargetResultStatus {
    SUCCESS,
    FAILURE,
    TIMEOUT,
    DNS_ERROR,
    CONNECTION_ERROR,
    TLS_ERROR,
    CANCELLED,
    SKIPPED,
    UNKNOWN,
}
