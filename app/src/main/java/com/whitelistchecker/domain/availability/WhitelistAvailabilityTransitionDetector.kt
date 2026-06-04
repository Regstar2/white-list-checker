package com.whitelistchecker.domain.availability

import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityState
import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityTransitionType

class WhitelistAvailabilityTransitionDetector {

    fun detect(
        previousState: WhitelistAvailabilityState,
        newState: WhitelistAvailabilityState,
    ): WhitelistAvailabilityTransitionType {
        if (newState == WhitelistAvailabilityState.ERROR) {
            return WhitelistAvailabilityTransitionType.ERROR_STATE
        }
        return when (previousState) {
            WhitelistAvailabilityState.UNAVAILABLE -> when (newState) {
                WhitelistAvailabilityState.AVAILABLE -> WhitelistAvailabilityTransitionType.BECAME_AVAILABLE
                WhitelistAvailabilityState.UNAVAILABLE -> WhitelistAvailabilityTransitionType.STAYED_UNAVAILABLE
                WhitelistAvailabilityState.UNKNOWN -> WhitelistAvailabilityTransitionType.ERROR_STATE
                WhitelistAvailabilityState.ERROR -> WhitelistAvailabilityTransitionType.ERROR_STATE
            }
            WhitelistAvailabilityState.AVAILABLE -> when (newState) {
                WhitelistAvailabilityState.UNAVAILABLE -> WhitelistAvailabilityTransitionType.BECAME_UNAVAILABLE
                WhitelistAvailabilityState.AVAILABLE -> WhitelistAvailabilityTransitionType.STAYED_AVAILABLE
                WhitelistAvailabilityState.UNKNOWN -> WhitelistAvailabilityTransitionType.ERROR_STATE
                WhitelistAvailabilityState.ERROR -> WhitelistAvailabilityTransitionType.ERROR_STATE
            }
            WhitelistAvailabilityState.UNKNOWN -> when (newState) {
                WhitelistAvailabilityState.AVAILABLE -> WhitelistAvailabilityTransitionType.UNKNOWN_TO_AVAILABLE
                WhitelistAvailabilityState.UNAVAILABLE -> WhitelistAvailabilityTransitionType.UNKNOWN_TO_UNAVAILABLE
                WhitelistAvailabilityState.UNKNOWN -> WhitelistAvailabilityTransitionType.ERROR_STATE
                WhitelistAvailabilityState.ERROR -> WhitelistAvailabilityTransitionType.ERROR_STATE
            }
            WhitelistAvailabilityState.ERROR -> when (newState) {
                WhitelistAvailabilityState.AVAILABLE -> WhitelistAvailabilityTransitionType.UNKNOWN_TO_AVAILABLE
                WhitelistAvailabilityState.UNAVAILABLE -> WhitelistAvailabilityTransitionType.UNKNOWN_TO_UNAVAILABLE
                WhitelistAvailabilityState.UNKNOWN -> WhitelistAvailabilityTransitionType.ERROR_STATE
                WhitelistAvailabilityState.ERROR -> WhitelistAvailabilityTransitionType.ERROR_STATE
            }
        }
    }

    fun isSignificantTransition(transition: WhitelistAvailabilityTransitionType): Boolean {
        return when (transition) {
            WhitelistAvailabilityTransitionType.BECAME_AVAILABLE,
            WhitelistAvailabilityTransitionType.BECAME_UNAVAILABLE,
            WhitelistAvailabilityTransitionType.UNKNOWN_TO_AVAILABLE,
            WhitelistAvailabilityTransitionType.UNKNOWN_TO_UNAVAILABLE,
            -> true
            else -> false
        }
    }

    fun isBecameAvailable(transition: WhitelistAvailabilityTransitionType): Boolean {
        return transition == WhitelistAvailabilityTransitionType.BECAME_AVAILABLE ||
            transition == WhitelistAvailabilityTransitionType.UNKNOWN_TO_AVAILABLE
    }

    fun isBecameUnavailable(transition: WhitelistAvailabilityTransitionType): Boolean {
        return transition == WhitelistAvailabilityTransitionType.BECAME_UNAVAILABLE ||
            transition == WhitelistAvailabilityTransitionType.UNKNOWN_TO_UNAVAILABLE
    }
}
