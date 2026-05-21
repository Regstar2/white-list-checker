package com.whitelistchecker.domain.classifier

import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistState

class WhitelistStateClassifier {

    fun classify(
        foreignSummary: TargetGroupSummary,
        localSummary: TargetGroupSummary,
    ): WhitelistState {
        val foreignRate = foreignSummary.availabilityRate
        val localRate = localSummary.availabilityRate

        return when {
            foreignRate == 0.0 && localRate == 0.0 -> WhitelistState.NO_MOBILE_INTERNET
            foreignRate <= 0.25 && localRate >= 0.5 -> WhitelistState.WHITELIST_ON
            foreignRate >= 0.5 && localRate >= 0.5 -> WhitelistState.WHITELIST_OFF
            else -> WhitelistState.PARTIAL_PROBLEM
        }
    }
}
