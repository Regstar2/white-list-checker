package com.whitelistchecker.domain.checkrun

import com.whitelistchecker.domain.model.NotificationPolicy
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.history.CheckTriggerType

class NotificationDecisionEngine {

    fun evaluate(
        policy: NotificationPolicy,
        previousAttempt: CheckOutcome,
        previousValidState: WhitelistState?,
        currentAttempt: CheckOutcome,
        trigger: CheckTriggerType,
        notifyOnAccessRestored: Boolean,
    ): NotificationDecision {
        val currentValidState = currentAttempt.validWhitelistStateOrNull()
        val accessRestored = notifyOnAccessRestored &&
            previousAttempt is CheckOutcome.Unavailable &&
            currentAttempt is CheckOutcome.Success
        val stateChanged = previousValidState != null &&
            currentValidState != null &&
            previousValidState != currentValidState

        if (accessRestored && stateChanged) {
            return NotificationDecision.AccessRestoredAndStateChanged(
                oldState = previousValidState,
                newState = currentValidState,
            )
        }

        return when (policy) {
            NotificationPolicy.NONE -> {
                if (accessRestored) {
                    NotificationDecision.AccessRestored(currentValidState)
                } else {
                    NotificationDecision.None
                }
            }
            NotificationPolicy.EVERY_ATTEMPT -> everyAttemptDecision(
                currentAttempt = currentAttempt,
                currentValidState = currentValidState,
                accessRestored = accessRestored,
            )
            NotificationPolicy.STATE_CHANGE_ONLY -> {
                when {
                    accessRestored -> NotificationDecision.AccessRestored(currentValidState)
                    stateChanged -> NotificationDecision.StateChanged(
                        oldState = previousValidState,
                        newState = currentValidState,
                    )
                    else -> NotificationDecision.None
                }
            }
        }
    }

    private fun everyAttemptDecision(
        currentAttempt: CheckOutcome,
        currentValidState: WhitelistState?,
        accessRestored: Boolean,
    ): NotificationDecision {
        return when (currentAttempt) {
            is CheckOutcome.Success -> {
                if (accessRestored) {
                    NotificationDecision.AccessRestored(currentValidState)
                } else {
                    NotificationDecision.AttemptResult(currentAttempt.state)
                }
            }
            is CheckOutcome.Unavailable -> NotificationDecision.AttemptUnavailable(currentAttempt.state)
            is CheckOutcome.Failure -> NotificationDecision.AttemptFailed(currentAttempt.error)
            CheckOutcome.Unknown -> NotificationDecision.None
        }
    }
}
