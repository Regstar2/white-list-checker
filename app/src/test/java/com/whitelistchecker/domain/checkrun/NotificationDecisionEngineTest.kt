package com.whitelistchecker.domain.checkrun

import com.whitelistchecker.domain.model.NotificationPolicy
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.history.CheckTriggerType
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationDecisionEngineTest {
    private val engine = NotificationDecisionEngine()

    @Test
    fun noneSuccessDoesNotNotify() {
        val decision = evaluate(
            policy = NotificationPolicy.NONE,
            previousValidState = WhitelistState.WHITELIST_OFF,
            currentAttempt = CheckOutcome.Success(WhitelistState.WHITELIST_ON),
        )

        assertEquals(NotificationDecision.None, decision)
    }

    @Test
    fun noneUnavailableDoesNotNotify() {
        val decision = evaluate(
            policy = NotificationPolicy.NONE,
            currentAttempt = CheckOutcome.Unavailable(WhitelistState.CELLULAR_NETWORK_UNAVAILABLE, "no cellular"),
        )

        assertEquals(NotificationDecision.None, decision)
    }

    @Test
    fun everyAttemptSuccessNotifiesResult() {
        val decision = evaluate(
            policy = NotificationPolicy.EVERY_ATTEMPT,
            currentAttempt = CheckOutcome.Success(WhitelistState.WHITELIST_OFF),
        )

        assertEquals(
            NotificationDecision.AttemptResult(WhitelistState.WHITELIST_OFF),
            decision,
        )
    }

    @Test
    fun everyAttemptUnavailableNotifiesUnavailable() {
        val decision = evaluate(
            policy = NotificationPolicy.EVERY_ATTEMPT,
            currentAttempt = CheckOutcome.Unavailable(WhitelistState.NO_MOBILE_INTERNET, "no internet"),
        )

        assertEquals(
            NotificationDecision.AttemptUnavailable(WhitelistState.NO_MOBILE_INTERNET),
            decision,
        )
    }

    @Test
    fun everyAttemptFailureNotifiesFailure() {
        val decision = evaluate(
            policy = NotificationPolicy.EVERY_ATTEMPT,
            currentAttempt = CheckOutcome.Failure("boom"),
        )

        assertEquals(NotificationDecision.AttemptFailed("boom"), decision)
    }

    @Test
    fun stateChangeOnlyFirstSuccessDoesNotNotify() {
        val decision = evaluate(
            policy = NotificationPolicy.STATE_CHANGE_ONLY,
            previousValidState = null,
            currentAttempt = CheckOutcome.Success(WhitelistState.WHITELIST_ON),
        )

        assertEquals(NotificationDecision.None, decision)
    }

    @Test
    fun stateChangeOnlySameStateDoesNotNotify() {
        val decision = evaluate(
            policy = NotificationPolicy.STATE_CHANGE_ONLY,
            previousValidState = WhitelistState.WHITELIST_ON,
            currentAttempt = CheckOutcome.Success(WhitelistState.WHITELIST_ON),
        )

        assertEquals(NotificationDecision.None, decision)
    }

    @Test
    fun stateChangeOnlyOffToOnNotifies() {
        val decision = evaluate(
            policy = NotificationPolicy.STATE_CHANGE_ONLY,
            previousValidState = WhitelistState.WHITELIST_OFF,
            currentAttempt = CheckOutcome.Success(WhitelistState.WHITELIST_ON),
        )

        assertEquals(
            NotificationDecision.StateChanged(
                oldState = WhitelistState.WHITELIST_OFF,
                newState = WhitelistState.WHITELIST_ON,
            ),
            decision,
        )
    }

    @Test
    fun stateChangeOnlyOnToOffNotifies() {
        val decision = evaluate(
            policy = NotificationPolicy.STATE_CHANGE_ONLY,
            previousValidState = WhitelistState.WHITELIST_ON,
            currentAttempt = CheckOutcome.Success(WhitelistState.WHITELIST_OFF),
        )

        assertEquals(
            NotificationDecision.StateChanged(
                oldState = WhitelistState.WHITELIST_ON,
                newState = WhitelistState.WHITELIST_OFF,
            ),
            decision,
        )
    }

    @Test
    fun stateChangeOnlyUnavailableDoesNotNotify() {
        val decision = evaluate(
            policy = NotificationPolicy.STATE_CHANGE_ONLY,
            previousValidState = WhitelistState.WHITELIST_OFF,
            currentAttempt = CheckOutcome.Unavailable(WhitelistState.MOBILE_DNS_FAILURE, "dns"),
        )

        assertEquals(NotificationDecision.None, decision)
    }

    @Test
    fun stateChangeOnlyFailureDoesNotNotify() {
        val decision = evaluate(
            policy = NotificationPolicy.STATE_CHANGE_ONLY,
            previousValidState = WhitelistState.WHITELIST_OFF,
            currentAttempt = CheckOutcome.Failure("boom"),
        )

        assertEquals(NotificationDecision.None, decision)
    }

    @Test
    fun unavailableToSameStateRestoredOffDoesNotNotifyWithoutRestoreSetting() {
        val decision = evaluate(
            policy = NotificationPolicy.STATE_CHANGE_ONLY,
            previousAttempt = CheckOutcome.Unavailable(WhitelistState.NO_MOBILE_INTERNET, "no internet"),
            previousValidState = WhitelistState.WHITELIST_OFF,
            currentAttempt = CheckOutcome.Success(WhitelistState.WHITELIST_OFF),
            notifyOnAccessRestored = false,
        )

        assertEquals(NotificationDecision.None, decision)
    }

    @Test
    fun unavailableToChangedStateNotifiesStateChangeWithoutRestoreSetting() {
        val decision = evaluate(
            policy = NotificationPolicy.STATE_CHANGE_ONLY,
            previousAttempt = CheckOutcome.Unavailable(WhitelistState.NO_MOBILE_INTERNET, "no internet"),
            previousValidState = WhitelistState.WHITELIST_OFF,
            currentAttempt = CheckOutcome.Success(WhitelistState.WHITELIST_ON),
            notifyOnAccessRestored = false,
        )

        assertEquals(
            NotificationDecision.StateChanged(WhitelistState.WHITELIST_OFF, WhitelistState.WHITELIST_ON),
            decision,
        )
    }

    @Test
    fun unavailableToSameStateNotifiesRestoreWhenEnabled() {
        val decision = evaluate(
            policy = NotificationPolicy.STATE_CHANGE_ONLY,
            previousAttempt = CheckOutcome.Unavailable(WhitelistState.NO_MOBILE_INTERNET, "no internet"),
            previousValidState = WhitelistState.WHITELIST_ON,
            currentAttempt = CheckOutcome.Success(WhitelistState.WHITELIST_ON),
            notifyOnAccessRestored = true,
        )

        assertEquals(
            NotificationDecision.AccessRestored(WhitelistState.WHITELIST_ON),
            decision,
        )
    }

    @Test
    fun unavailableToChangedStateCombinesRestoreAndChangeWhenEnabled() {
        val decision = evaluate(
            policy = NotificationPolicy.STATE_CHANGE_ONLY,
            previousAttempt = CheckOutcome.Unavailable(WhitelistState.NO_MOBILE_INTERNET, "no internet"),
            previousValidState = WhitelistState.WHITELIST_OFF,
            currentAttempt = CheckOutcome.Success(WhitelistState.WHITELIST_ON),
            notifyOnAccessRestored = true,
        )

        assertEquals(
            NotificationDecision.AccessRestoredAndStateChanged(
                oldState = WhitelistState.WHITELIST_OFF,
                newState = WhitelistState.WHITELIST_ON,
            ),
            decision,
        )
    }

    @Test
    fun partialProblemDoesNotBecomeValidStateChange() {
        val decision = evaluate(
            policy = NotificationPolicy.STATE_CHANGE_ONLY,
            previousValidState = WhitelistState.WHITELIST_OFF,
            currentAttempt = CheckOutcome.Success(WhitelistState.PARTIAL_PROBLEM),
        )

        assertEquals(NotificationDecision.None, decision)
    }

    private fun evaluate(
        policy: NotificationPolicy,
        previousAttempt: CheckOutcome = CheckOutcome.Unknown,
        previousValidState: WhitelistState? = null,
        currentAttempt: CheckOutcome,
        notifyOnAccessRestored: Boolean = false,
    ): NotificationDecision {
        return engine.evaluate(
            policy = policy,
            previousAttempt = previousAttempt,
            previousValidState = previousValidState,
            currentAttempt = currentAttempt,
            trigger = CheckTriggerType.WORK_MANAGER,
            notifyOnAccessRestored = notifyOnAccessRestored,
        )
    }
}
