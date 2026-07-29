package com.whitelistchecker.domain.model

data class PublicServiceRemoteCommand(
    val commandId: String,
    val type: PublicServiceRemoteCommandType,
    val expiresAtMillis: Long,
)

enum class PublicServiceRemoteCommandType {
    CHECK_NOW,
}

enum class PublicServiceCommandOutcome {
    SUCCESS,
    UNAVAILABLE,
    FAILED,
    BUSY,
    EXPIRED,
    UNAUTHORIZED,
}
