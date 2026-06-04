package com.whitelistchecker.domain.availability

import com.whitelistchecker.domain.model.availability.WhitelistAvailabilityState
import com.whitelistchecker.domain.model.history.CheckTargetResultStatus

object WhitelistAvailabilityStateMapper {

    fun fromCheckTargetStatus(status: CheckTargetResultStatus): WhitelistAvailabilityState {
        return when (status) {
            CheckTargetResultStatus.SUCCESS -> WhitelistAvailabilityState.AVAILABLE
            CheckTargetResultStatus.FAILURE -> WhitelistAvailabilityState.UNAVAILABLE
            CheckTargetResultStatus.DNS_ERROR,
            CheckTargetResultStatus.TIMEOUT,
            CheckTargetResultStatus.CONNECTION_ERROR,
            CheckTargetResultStatus.TLS_ERROR,
            -> WhitelistAvailabilityState.ERROR
            CheckTargetResultStatus.CANCELLED,
            CheckTargetResultStatus.SKIPPED,
            CheckTargetResultStatus.UNKNOWN,
            -> WhitelistAvailabilityState.UNKNOWN
        }
    }
}
