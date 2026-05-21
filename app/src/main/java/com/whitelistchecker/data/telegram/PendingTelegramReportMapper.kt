package com.whitelistchecker.data.telegram

import com.whitelistchecker.domain.model.PendingTelegramReport
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.WhitelistStateChangeType

fun PendingTelegramReportEntity.toDomain(): PendingTelegramReport {
    return PendingTelegramReport(
        id = id,
        text = text,
        eventType = parseChangeType(eventType),
        oldState = parseWhitelistState(oldState),
        newState = parseWhitelistState(newState),
        createdAtMillis = createdAtMillis,
        attemptCount = attemptCount,
        lastAttemptAtMillis = lastAttemptAtMillis,
        lastError = lastError,
    )
}

private fun parseWhitelistState(value: String): WhitelistState {
    return runCatching {
        WhitelistState.valueOf(value)
    }.getOrDefault(WhitelistState.UNKNOWN)
}

private fun parseChangeType(value: String): WhitelistStateChangeType {
    return runCatching {
        WhitelistStateChangeType.valueOf(value)
    }.getOrDefault(WhitelistStateChangeType.OTHER_CONFIRMED_CHANGE)
}
