package com.whitelistchecker.domain.classifier

import com.whitelistchecker.domain.model.DnsWhitelistSignal
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import org.junit.Assert.assertEquals
import org.junit.Test

class DnsWhitelistSignalClassifierTest {

    private val classifier = DnsWhitelistSignalClassifier()

    @Test
    fun foreignZeroOfTwo_localTwoOfTwo_isWhitelistLike() {
        assertEquals(
            DnsWhitelistSignal.WHITELIST_LIKE,
            classifier.classify(summary(TargetGroup.FOREIGN, 0, 2), summary(TargetGroup.LOCAL, 2, 2)),
        )
    }

    @Test
    fun bothTwoOfTwo_isNormal() {
        assertEquals(
            DnsWhitelistSignal.NORMAL,
            classifier.classify(summary(TargetGroup.FOREIGN, 2, 2), summary(TargetGroup.LOCAL, 2, 2)),
        )
    }

    @Test
    fun bothZeroOfTwo_isNoDnsAccess() {
        assertEquals(
            DnsWhitelistSignal.NO_DNS_ACCESS,
            classifier.classify(summary(TargetGroup.FOREIGN, 0, 2), summary(TargetGroup.LOCAL, 0, 2)),
        )
    }

    @Test
    fun mixedRates_arePartial() {
        assertEquals(
            DnsWhitelistSignal.PARTIAL,
            classifier.classify(summary(TargetGroup.FOREIGN, 1, 2), summary(TargetGroup.LOCAL, 0, 2)),
        )
    }

    private fun summary(group: TargetGroup, available: Int, total: Int) = TargetGroupSummary(
        group = group,
        availableCount = available,
        totalCount = total,
    )
}
