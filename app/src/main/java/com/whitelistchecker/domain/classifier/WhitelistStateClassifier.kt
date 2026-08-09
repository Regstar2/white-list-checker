package com.whitelistchecker.domain.classifier

import com.whitelistchecker.domain.model.DnsWhitelistSignal
import com.whitelistchecker.domain.model.SiteCheckErrorType
import com.whitelistchecker.domain.model.SiteCheckResult
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistState

class WhitelistStateClassifier {

    fun classify(
        foreignSummary: TargetGroupSummary,
        localSummary: TargetGroupSummary,
        siteResults: List<SiteCheckResult> = emptyList(),
        dnsSignal: DnsWhitelistSignal = DnsWhitelistSignal.UNKNOWN,
    ): WhitelistState {
        val siteState = classifySites(foreignSummary, localSummary, siteResults)
        return combine(siteState, dnsSignal)
    }

    fun classifySites(
        foreignSummary: TargetGroupSummary,
        localSummary: TargetGroupSummary,
        siteResults: List<SiteCheckResult> = emptyList(),
    ): WhitelistState {
        if (isMobileDnsFailure(siteResults)) {
            return WhitelistState.MOBILE_DNS_FAILURE
        }

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

    private fun isMobileDnsFailure(siteResults: List<SiteCheckResult>): Boolean {
        if (siteResults.isEmpty() || siteResults.any { it.available }) {
            return false
        }
        val dnsFailures = siteResults.count { it.errorType == SiteCheckErrorType.DNS }
        return dnsFailures * 4 >= siteResults.size * 3
    }
}
