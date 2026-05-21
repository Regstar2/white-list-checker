package com.whitelistchecker.domain.classifier

import com.whitelistchecker.domain.model.CheckTarget
import com.whitelistchecker.domain.model.SiteCheckErrorType
import com.whitelistchecker.domain.model.SiteCheckResult
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistState
import org.junit.Assert.assertEquals
import org.junit.Test

class WhitelistStateClassifierTest {

    private val classifier = WhitelistStateClassifier()

    @Test
    fun classify_allDnsFailures_returnsMobileDnsFailure() {
        val siteResults = List(4) { index ->
            siteResult(
                name = "site-$index",
                group = if (index % 2 == 0) TargetGroup.FOREIGN else TargetGroup.LOCAL,
                errorType = SiteCheckErrorType.DNS,
            )
        }

        val state = classifier.classify(
            foreignSummary = summary(TargetGroup.FOREIGN, available = 0, total = 2),
            localSummary = summary(TargetGroup.LOCAL, available = 0, total = 2),
            siteResults = siteResults,
        )

        assertEquals(WhitelistState.MOBILE_DNS_FAILURE, state)
    }

    @Test
    fun classify_mixedTimeoutErrors_returnsNoMobileInternet() {
        val siteResults = listOf(
            siteResult("foreign-1", TargetGroup.FOREIGN, SiteCheckErrorType.TIMEOUT),
            siteResult("foreign-2", TargetGroup.FOREIGN, SiteCheckErrorType.CONNECTION),
            siteResult("local-1", TargetGroup.LOCAL, SiteCheckErrorType.TIMEOUT),
            siteResult("local-2", TargetGroup.LOCAL, SiteCheckErrorType.CONNECTION),
        )

        val state = classifier.classify(
            foreignSummary = summary(TargetGroup.FOREIGN, available = 0, total = 2),
            localSummary = summary(TargetGroup.LOCAL, available = 0, total = 2),
            siteResults = siteResults,
        )

        assertEquals(WhitelistState.NO_MOBILE_INTERNET, state)
    }

    @Test
    fun classify_whitelistOnPattern_stillWorks() {
        val siteResults = listOf(
            siteResult("foreign-1", TargetGroup.FOREIGN, SiteCheckErrorType.DNS, available = false),
            siteResult("foreign-2", TargetGroup.FOREIGN, SiteCheckErrorType.NONE, available = false),
            siteResult("local-1", TargetGroup.LOCAL, SiteCheckErrorType.NONE, available = true),
            siteResult("local-2", TargetGroup.LOCAL, SiteCheckErrorType.NONE, available = true),
        )

        val state = classifier.classify(
            foreignSummary = summary(TargetGroup.FOREIGN, available = 0, total = 2),
            localSummary = summary(TargetGroup.LOCAL, available = 2, total = 2),
            siteResults = siteResults,
        )

        assertEquals(WhitelistState.WHITELIST_ON, state)
    }

    private fun summary(group: TargetGroup, available: Int, total: Int): TargetGroupSummary {
        return TargetGroupSummary(
            group = group,
            availableCount = available,
            totalCount = total,
        )
    }

    private fun siteResult(
        name: String,
        group: TargetGroup,
        errorType: SiteCheckErrorType,
        available: Boolean = false,
    ): SiteCheckResult {
        return SiteCheckResult(
            target = CheckTarget(name = name, url = "https://$name.example", group = group),
            available = available,
            httpCode = if (available) 200 else null,
            error = if (available) null else "UnknownHostException: Unable to resolve host",
            errorType = if (available) SiteCheckErrorType.NONE else errorType,
            durationMs = 100,
        )
    }
}
