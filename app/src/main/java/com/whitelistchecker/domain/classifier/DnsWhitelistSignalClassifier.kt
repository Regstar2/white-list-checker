package com.whitelistchecker.domain.classifier

import com.whitelistchecker.domain.model.DnsWhitelistSignal
import com.whitelistchecker.domain.model.TargetGroupSummary

class DnsWhitelistSignalClassifier {

    fun classify(
        foreignSummary: TargetGroupSummary,
        localSummary: TargetGroupSummary,
    ): DnsWhitelistSignal {
        if (foreignSummary.totalCount == 0 || localSummary.totalCount == 0) {
            return DnsWhitelistSignal.UNKNOWN
        }
        val foreignRate = foreignSummary.availabilityRate
        val localRate = localSummary.availabilityRate
        return when {
            foreignRate == 0.0 && localRate == 0.0 -> DnsWhitelistSignal.NO_DNS_ACCESS
            foreignRate <= 0.25 && localRate >= 0.5 -> DnsWhitelistSignal.WHITELIST_LIKE
            foreignRate >= 0.5 && localRate >= 0.5 -> DnsWhitelistSignal.NORMAL
            else -> DnsWhitelistSignal.PARTIAL
        }
    }
}
