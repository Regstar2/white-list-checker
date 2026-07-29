package com.whitelistchecker.domain.publicservice

import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.PublicServiceSettings
import com.whitelistchecker.domain.model.SiteCheckResult
import com.whitelistchecker.domain.model.SiteCheckErrorType
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.history.CheckTriggerType
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.UUID
import kotlin.math.max

class PublicReportPayloadBuilder(
    private val appVersionProvider: () -> String,
) {

    fun build(
        result: NetworkCheckResult,
        triggerType: CheckTriggerType,
        settings: PublicServiceSettings,
        reportId: String = UUID.randomUUID().toString(),
    ): PublicReportPayload? {
        val report = buildModel(result, triggerType, settings, reportId) ?: return null
        return PublicReportPayload(
            reportId = report.reportId,
            checkedAtMillis = report.checkedAt,
            json = report.toJson().toString(),
        )
    }

    internal fun buildModel(
        result: NetworkCheckResult,
        triggerType: CheckTriggerType,
        settings: PublicServiceSettings,
        reportId: String = UUID.randomUUID().toString(),
    ): PublicReportModel? {
        if (!settings.hasPublicReportContext) return null
        if (!isUploadable(result)) return null
        val mappedState = mapWhitelistState(result.state) ?: return null
        return PublicReportModel(
            schemaVersion = 1,
            requestId = UUID.randomUUID().toString(),
            reportId = reportId,
            checkedAt = result.checkedAtMillis,
            triggerType = triggerType.toPublicTrigger(),
            appVersion = appVersionProvider(),
            regionCode = settings.regionCode,
            cityCode = settings.cityCode,
            customCityName = settings.customCityName,
            operatorCode = settings.operatorCode,
            areaSource = settings.areaSource.name,
            operatorSource = settings.operatorSource.name,
            whitelistState = mappedState,
            resultQuality = result.state.toResultQuality(),
            foreign = PublicCount(
                available = result.foreignSummary.availableCount,
                total = result.foreignSummary.totalCount,
            ),
            local = PublicCount(
                available = result.localSummary.availableCount,
                total = result.localSummary.totalCount,
            ),
            targets = result.siteResults.map { it.toPublicTarget() },
        )
    }

    private fun isUploadable(result: NetworkCheckResult): Boolean {
        if (result.error != null) return false
        return when (result.state) {
            WhitelistState.WHITELIST_ON,
            WhitelistState.WHITELIST_OFF,
            WhitelistState.PARTIAL_PROBLEM,
            WhitelistState.MOBILE_DNS_FAILURE,
            -> true
            WhitelistState.UNKNOWN,
            WhitelistState.NO_MOBILE_INTERNET,
            WhitelistState.CELLULAR_NETWORK_UNAVAILABLE,
            -> false
        }
    }

    private fun mapWhitelistState(state: WhitelistState): String? {
        return when (state) {
            WhitelistState.WHITELIST_ON -> "LIKELY_ENABLED"
            WhitelistState.WHITELIST_OFF -> "LIKELY_DISABLED"
            WhitelistState.PARTIAL_PROBLEM -> "PARTIAL_PROBLEM"
            WhitelistState.MOBILE_DNS_FAILURE -> "MOBILE_DNS_FAILURE"
            WhitelistState.UNKNOWN,
            WhitelistState.NO_MOBILE_INTERNET,
            WhitelistState.CELLULAR_NETWORK_UNAVAILABLE,
            -> null
        }
    }

    private fun WhitelistState.toResultQuality(): String {
        return when (this) {
            WhitelistState.WHITELIST_ON,
            WhitelistState.WHITELIST_OFF,
            -> "CONCLUSIVE"
            WhitelistState.PARTIAL_PROBLEM,
            WhitelistState.MOBILE_DNS_FAILURE,
            -> "INCONCLUSIVE"
            else -> "INCONCLUSIVE"
        }
    }

    private fun CheckTriggerType.toPublicTrigger(): String {
        return when (this) {
            CheckTriggerType.MANUAL,
            CheckTriggerType.MANUAL_UI,
            -> "MANUAL"
            CheckTriggerType.BACKGROUND,
            CheckTriggerType.WORK_MANAGER,
            -> "WORK_MANAGER"
            CheckTriggerType.FOREGROUND_INTERVAL -> "FOREGROUND_INTERVAL"
            CheckTriggerType.FOREGROUND_NOTIFICATION_ACTION -> "FOREGROUND_NOTIFICATION_ACTION"
            CheckTriggerType.TELEGRAM_COMMAND,
            CheckTriggerType.REMOTE_TELEGRAM,
            -> "REMOTE_TELEGRAM"
        }
    }

    private fun SiteCheckResult.toPublicTarget(): PublicTarget {
        return PublicTarget(
            targetCode = safeTargetCode(target.url, target.group),
            targetGroup = target.group.name,
            targetStatus = if (available) "AVAILABLE" else "UNAVAILABLE",
            latencyBucket = latencyBucket(durationMs, errorType),
        )
    }

    private fun safeTargetCode(url: String, group: TargetGroup): String {
        return runCatching { URI(url).host }
            .getOrNull()
            ?.lowercase()
            ?.removePrefix("www.")
            ?.take(MAX_TARGET_CODE_LENGTH)
            ?: group.groupFallback()
    }

    private fun TargetGroup.groupFallback(): String {
        return when (this) {
            TargetGroup.FOREIGN -> "foreign-target"
            TargetGroup.LOCAL -> "local-target"
        }
    }

    private fun latencyBucket(durationMs: Long, errorType: SiteCheckErrorType): String {
        if (errorType != SiteCheckErrorType.NONE) return errorType.name
        val normalized = max(0L, durationMs)
        return when {
            normalized < 250 -> "LT_250_MS"
            normalized < 1000 -> "LT_1S"
            normalized < 3000 -> "LT_3S"
            else -> "GE_3S"
        }
    }

    data class PublicReportPayload(
        val reportId: String,
        val checkedAtMillis: Long,
        val json: String,
    )

    internal data class PublicReportModel(
        val schemaVersion: Int,
        val requestId: String,
        val reportId: String,
        val checkedAt: Long,
        val triggerType: String,
        val appVersion: String,
        val regionCode: String,
        val cityCode: String?,
        val customCityName: String?,
        val operatorCode: String,
        val areaSource: String,
        val operatorSource: String,
        val whitelistState: String,
        val resultQuality: String,
        val foreign: PublicCount,
        val local: PublicCount,
        val targets: List<PublicTarget>,
    ) {
        fun toJson(): JSONObject {
            return JSONObject()
                .put("schemaVersion", schemaVersion)
                .put("requestId", requestId)
                .put("reportId", reportId)
                .put("checkedAt", checkedAt)
                .put("triggerType", triggerType)
                .put("appVersion", appVersion)
                .put("regionCode", regionCode)
                .putNullable("cityCode", cityCode)
                .putNullable("customCityName", customCityName)
                .put("operatorCode", operatorCode)
                .put("areaSource", areaSource)
                .put("operatorSource", operatorSource)
                .put("whitelistState", whitelistState)
                .put("resultQuality", resultQuality)
                .put(
                    "foreign",
                    JSONObject()
                        .put("available", foreign.available)
                        .put("total", foreign.total),
                )
                .put(
                    "local",
                    JSONObject()
                        .put("available", local.available)
                        .put("total", local.total),
                )
                .put("targets", JSONArray(targets.map { it.toJson() }))
        }
    }

    internal data class PublicCount(
        val available: Int,
        val total: Int,
    )

    internal data class PublicTarget(
        val targetCode: String,
        val targetGroup: String,
        val targetStatus: String,
        val latencyBucket: String,
    ) {
        fun toJson(): JSONObject {
            return JSONObject()
                .put("targetCode", targetCode)
                .put("targetGroup", targetGroup)
                .put("targetStatus", targetStatus)
                .put("latencyBucket", latencyBucket)
        }
    }

    companion object {
        private const val MAX_TARGET_CODE_LENGTH = 80
    }
}

private fun JSONObject.putNullable(name: String, value: String?): JSONObject {
    if (value.isNullOrBlank()) return put(name, JSONObject.NULL)
    return put(name, value)
}
