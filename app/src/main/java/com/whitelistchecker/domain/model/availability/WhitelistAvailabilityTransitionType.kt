package com.whitelistchecker.domain.model.availability

enum class WhitelistAvailabilityTransitionType {
    BECAME_AVAILABLE,
    BECAME_UNAVAILABLE,
    STAYED_AVAILABLE,
    STAYED_UNAVAILABLE,
    UNKNOWN_TO_AVAILABLE,
    UNKNOWN_TO_UNAVAILABLE,
    ERROR_STATE,
}
