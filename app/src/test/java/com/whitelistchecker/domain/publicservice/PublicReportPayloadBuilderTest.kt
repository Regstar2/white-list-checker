package com.whitelistchecker.domain.publicservice

import com.whitelistchecker.domain.model.CheckTarget
import com.whitelistchecker.domain.model.AreaSource
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.OperatorDetectionSource
import com.whitelistchecker.domain.model.PublicServiceSettings
import com.whitelistchecker.domain.model.SiteCheckErrorType
import com.whitelistchecker.domain.model.SiteCheckResult
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.history.CheckTriggerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PublicReportPayloadBuilderTest {

    private val builder = PublicReportPayloadBuilder { "0.8.15" }
    private val settings = PublicServiceSettings(
        shareReports = true,
        regionCode = "RU-RYA",
        regionName = "Рязанская область",
        cityCode = "RU-RYA-RYAZAN",
        cityName = "Рязань",
        areaSource = AreaSource.MANUAL_SELECTION,
        areaConfirmedByUser = true,
        operatorCode = "MEGAFON",
        operatorSource = OperatorDetectionSource.MANUAL,
    )

    @Test
    fun `sharing disabled skips report`() {
        val payload = builder.build(
            result = result(WhitelistState.WHITELIST_ON),
            triggerType = CheckTriggerType.MANUAL_UI,
            settings = settings.copy(shareReports = false),
        )

        assertNull(payload)
    }

    @Test
    fun `cellular unavailable skips report`() {
        val payload = builder.build(
            result = result(WhitelistState.CELLULAR_NETWORK_UNAVAILABLE),
            triggerType = CheckTriggerType.MANUAL_UI,
            settings = settings,
        )

        assertNull(payload)
    }

    @Test
    fun `whitelist on maps to likely enabled`() {
        val payload = builder.buildModel(
            result = result(WhitelistState.WHITELIST_ON),
            triggerType = CheckTriggerType.MANUAL_UI,
            settings = settings,
            reportId = "report-1",
        )

        assertNotNull(payload)
        assertEquals("LIKELY_ENABLED", payload!!.whitelistState)
        assertEquals("CONCLUSIVE", payload.resultQuality)
        assertEquals("MANUAL", payload.triggerType)
        assertEquals("RU-RYA-RYAZAN", payload.cityCode)
        assertEquals("MANUAL_SELECTION", payload.areaSource)
        assertEquals("MANUAL", payload.operatorSource)
    }

    @Test
    fun `whitelist off maps to likely disabled`() {
        val payload = builder.buildModel(
            result = result(WhitelistState.WHITELIST_OFF),
            triggerType = CheckTriggerType.WORK_MANAGER,
            settings = settings,
            reportId = "report-2",
        )

        assertEquals("LIKELY_DISABLED", payload!!.whitelistState)
        assertEquals("WORK_MANAGER", payload.triggerType)
    }

    @Test
    fun `remote telegram trigger is explicit`() {
        val payload = builder.buildModel(
            result = result(WhitelistState.WHITELIST_OFF),
            triggerType = CheckTriggerType.REMOTE_TELEGRAM,
            settings = settings,
            reportId = "report-3",
        )

        assertEquals("REMOTE_TELEGRAM", payload!!.triggerType)
    }

    @Test
    fun `report works without city`() {
        val payload = builder.buildModel(
            result = result(WhitelistState.WHITELIST_OFF),
            triggerType = CheckTriggerType.MANUAL_UI,
            settings = settings.copy(cityCode = null, cityName = null),
            reportId = "report-4",
        )

        assertNotNull(payload)
        assertNull(payload!!.cityCode)
    }

    private fun result(state: WhitelistState): NetworkCheckResult {
        val foreignTarget = CheckTarget("Google", "https://www.google.com", TargetGroup.FOREIGN)
        val localTarget = CheckTarget("Yandex", "https://yandex.ru", TargetGroup.LOCAL)
        val foreignAvailable = state == WhitelistState.WHITELIST_OFF
        return NetworkCheckResult(
            siteResults = listOf(
                SiteCheckResult(
                    target = foreignTarget,
                    available = foreignAvailable,
                    httpCode = if (foreignAvailable) 204 else null,
                    error = if (foreignAvailable) null else "timeout",
                    errorType = if (foreignAvailable) SiteCheckErrorType.NONE else SiteCheckErrorType.TIMEOUT,
                    durationMs = 100,
                ),
                SiteCheckResult(
                    target = localTarget,
                    available = true,
                    httpCode = 204,
                    error = null,
                    errorType = SiteCheckErrorType.NONE,
                    durationMs = 120,
                ),
            ),
            foreignSummary = TargetGroupSummary(TargetGroup.FOREIGN, if (foreignAvailable) 1 else 0, 1),
            localSummary = TargetGroupSummary(TargetGroup.LOCAL, 1, 1),
            state = state,
            activeNetworkLabel = "Wi-Fi",
            checkedNetworkLabel = "Mobile",
            checkedAtMillis = 1234L,
        )
    }
}
