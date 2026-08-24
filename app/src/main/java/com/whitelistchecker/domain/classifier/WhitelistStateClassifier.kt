package com.whitelistchecker.domain.classifier

import com.whitelistchecker.domain.model.DnsWhitelistSignal
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistState

class WhitelistStateClassifier {

    fun classify(
        foreignSummary: TargetGroupSummary,
        localSummary: TargetGroupSummary,
        dnsSignal: DnsWhitelistSignal = DnsWhitelistSignal.UNKNOWN,
        dnsFailureConfirmed: Boolean = false,
    ): WhitelistState {
        val siteState = classifySites(foreignSummary, localSummary)
        if (dnsFailureConfirmed) {
            return WhitelistState.MOBILE_DNS_FAILURE
        }
        return combine(siteState, dnsSignal)
    }

    fun classifySites(
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

    private fun combine(
        siteState: WhitelistState,
        dnsSignal: DnsWhitelistSignal,
    ): WhitelistState {
        return when {
            siteState == WhitelistState.WHITELIST_ON && dnsSignal == DnsWhitelistSignal.NORMAL -> {
                WhitelistState.PARTIAL_PROBLEM
            }
            siteState == WhitelistState.WHITELIST_OFF && dnsSignal == DnsWhitelistSignal.WHITELIST_LIKE -> {
                WhitelistState.PARTIAL_PROBLEM
            }
            else -> siteState
        }
    }
}
