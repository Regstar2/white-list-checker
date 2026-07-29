package com.whitelistchecker.domain.checkrun

import com.whitelistchecker.domain.model.WhitelistState

sealed interface NotificationDecision {
    data object None : NotificationDecision

    data class AttemptResult(
        val currentState: WhitelistState?,
    ) : NotificationDecision

    data class StateChanged(
        val oldState: WhitelistState,
        val newState: WhitelistState,
    ) : NotificationDecision

    data class AccessRestored(
        val currentState: WhitelistState?,
    ) : NotificationDecision

    data class AccessRestoredAndStateChanged(
        val oldState: WhitelistState,
        val newState: WhitelistState,
    ) : NotificationDecision

    data class AttemptUnavailable(
        val state: WhitelistState,
    ) : NotificationDecision

    data class AttemptFailed(
        val error: String,
    ) : NotificationDecision
}
