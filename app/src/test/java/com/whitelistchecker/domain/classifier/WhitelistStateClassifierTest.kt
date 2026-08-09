package com.whitelistchecker.domain.classifier

import com.whitelistchecker.domain.model.DnsWhitelistSignal
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistState
import org.junit.Assert.assertEquals
import org.junit.Test

class WhitelistStateClassifierTest {

    private val classifier = WhitelistStateClassifier()

    @Test
    fun classify_allSitesUnavailableWithUnknownDns_returnsNoMobileInternet() {
        val state = classifier.classify(
            foreignSummary = summary(TargetGroup.FOREIGN, available = 0, total = 2),
            localSummary = summary(TargetGroup.LOCAL, available = 0, total = 2),
        )

        assertEquals(WhitelistState.NO_MOBILE_INTERNET, state)
    }

    @Test
    fun classify_whitelistOnPattern_stillWorks() {
        val state = classifyWhitelistSitePattern(DnsWhitelistSignal.UNKNOWN)
        assertEquals(WhitelistState.WHITELIST_ON, state)
    }

    @Test
    fun classify_siteOnAndDnsOn_returnsWhitelistOn() {
        val state = classifyWhitelistSitePattern(DnsWhitelistSignal.WHITELIST_LIKE)
        assertEquals(WhitelistState.WHITELIST_ON, state)
    }

    @Test
    fun classify_siteOnAndDnsNormal_returnsPartialProblem() {
        val state = classifyWhitelistSitePattern(DnsWhitelistSignal.NORMAL)
        assertEquals(WhitelistState.PARTIAL_PROBLEM, state)
    }

    @Test
    fun classify_siteOffAndDnsNormal_returnsWhitelistOff() {
        val state = classifyNormalSitePattern(DnsWhitelistSignal.NORMAL)
        assertEquals(WhitelistState.WHITELIST_OFF, state)
    }

    @Test
    fun classify_siteOffAndDnsWhitelistLike_returnsPartialProblem() {
        val state = classifyNormalSitePattern(DnsWhitelistSignal.WHITELIST_LIKE)
        assertEquals(WhitelistState.PARTIAL_PROBLEM, state)
    }

    @Test
    fun classify_dnsUnavailable_returnsMobileDnsFailure() {
        val state = classifyNormalSitePattern(DnsWhitelistSignal.NO_DNS_ACCESS)
        assertEquals(WhitelistState.MOBILE_DNS_FAILURE, state)
    }

    private fun classifyWhitelistSitePattern(dnsSignal: DnsWhitelistSignal): WhitelistState {
        return classifier.classify(
            foreignSummary = summary(TargetGroup.FOREIGN, available = 0, total = 2),
            localSummary = summary(TargetGroup.LOCAL, available = 2, total = 2),
            dnsSignal = dnsSignal,
        )
    }

    private fun classifyNormalSitePattern(dnsSignal: DnsWhitelistSignal): WhitelistState {
        return classifier.classify(
            foreignSummary = summary(TargetGroup.FOREIGN, available = 2, total = 2),
            localSummary = summary(TargetGroup.LOCAL, available = 2, total = 2),
            dnsSignal = dnsSignal,
        )
    }

    private fun summary(group: TargetGroup, available: Int, total: Int): TargetGroupSummary {
        return TargetGroupSummary(
            group = group,
            availableCount = available,
            totalCount = total,
        )
    }
}
