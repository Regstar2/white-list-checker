package com.whitelistchecker.domain.monitor

import com.whitelistchecker.domain.model.StateChangeDetectionResult
import com.whitelistchecker.domain.model.WhitelistMonitorState
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.domain.model.WhitelistStateChangeType

class StateChangeDetector {

    fun detect(
        currentState: WhitelistState,
        savedState: WhitelistMonitorState,
        nowMillis: Long,
    ): StateChangeDetectionResult {
        if (currentState == WhitelistState.UNKNOWN) {
            return StateChangeDetectionResult(
                updatedMonitorState = savedState.copy(
                    pendingState = WhitelistState.UNKNOWN,
                    pendingStateCount = 0,
                ),
                event = null,
            )
        }

        if (savedState.lastConfirmedState == WhitelistState.UNKNOWN) {
            return StateChangeDetectionResult(
                updatedMonitorState = savedState.copy(
                    lastConfirmedState = currentState,
                    lastConfirmedAtMillis = nowMillis,
                    pendingState = WhitelistState.UNKNOWN,
                    pendingStateCount = 0,
                ),
                event = null,
            )
        }

        if (currentState == savedState.lastConfirmedState) {
            return StateChangeDetectionResult(
                updatedMonitorState = savedState.copy(
                    pendingState = WhitelistState.UNKNOWN,
                    pendingStateCount = 0,
                ),
                event = null,
            )
        }

        val pendingState = currentState
        val pendingStateCount = if (currentState == savedState.pendingState) {
            savedState.pendingStateCount + 1
        } else {
            1
        }

        if (pendingStateCount < REQUIRED_CONFIRMATION_COUNT) {
            return StateChangeDetectionResult(
                updatedMonitorState = savedState.copy(
                    pendingState = pendingState,
                    pendingStateCount = pendingStateCount,
                ),
                event = null,
            )
        }

        val oldState = savedState.lastConfirmedState
        val newState = currentState
        val event = WhitelistStateChangeEvent(
            oldState = oldState,
            newState = newState,
            type = resolveChangeType(oldState, newState),
            changedAtMillis = nowMillis,
        )

        return StateChangeDetectionResult(
            updatedMonitorState = savedState.copy(
                lastConfirmedState = newState,
                lastConfirmedAtMillis = nowMillis,
                lastStateChangeAtMillis = nowMillis,
                pendingState = WhitelistState.UNKNOWN,
                pendingStateCount = 0,
            ),
            event = event,
        )
    }

    private fun resolveChangeType(
        oldState: WhitelistState,
        newState: WhitelistState,
    ): WhitelistStateChangeType {
        if (oldState == WhitelistState.WHITELIST_OFF && newState == WhitelistState.WHITELIST_ON) {
            return WhitelistStateChangeType.WHITELIST_TURNED_ON
        }
        if (oldState == WhitelistState.WHITELIST_ON && newState == WhitelistState.WHITELIST_OFF) {
            return WhitelistStateChangeType.WHITELIST_TURNED_OFF
        }
        return WhitelistStateChangeType.OTHER_CONFIRMED_CHANGE
    }

    companion object {
        const val REQUIRED_CONFIRMATION_COUNT = 2
    }
}
