package com.whitelistchecker.data.check

import com.whitelistchecker.domain.model.CheckTarget
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.SiteCheckErrorType
import com.whitelistchecker.domain.model.SiteCheckResult
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistState
import org.json.JSONArray
import org.json.JSONObject

internal object LastCheckJsonMapper {

    fun encode(result: NetworkCheckResult): String {
        return JSONObject().apply {
            put("state", result.state.name)
            put("activeNetworkLabel", result.activeNetworkLabel)
            put("checkedNetworkLabel", result.checkedNetworkLabel)
            put("checkedAtMillis", result.checkedAtMillis)
            putOpt("error", result.error)
            putOpt("diagnosticsMessage", result.diagnosticsMessage)
            put("foreignSummary", encodeSummary(result.foreignSummary))
            put("localSummary", encodeSummary(result.localSummary))
            put("siteResults", encodeSiteResults(result.siteResults))
        }.toString()
    }

    fun decode(json: String): NetworkCheckResult {
        val root = JSONObject(json)
        return NetworkCheckResult(
            siteResults = decodeSiteResults(root.getJSONArray("siteResults")),
            foreignSummary = decodeSummary(root.getJSONObject("foreignSummary")),
            localSummary = decodeSummary(root.getJSONObject("localSummary")),
            state = parseWhitelistState(root.getString("state")),
            activeNetworkLabel = root.getString("activeNetworkLabel"),
            checkedNetworkLabel = root.getString("checkedNetworkLabel"),
            checkedAtMillis = root.getLong("checkedAtMillis"),
            error = root.optString("error").takeIf { it.isNotBlank() },
            diagnosticsMessage = root.optString("diagnosticsMessage").takeIf { it.isNotBlank() },
        )
    }

    private fun encodeSummary(summary: TargetGroupSummary): JSONObject {
        return JSONObject().apply {
            put("group", summary.group.name)
            put("availableCount", summary.availableCount)
            put("totalCount", summary.totalCount)
        }
    }

    private fun decodeSummary(json: JSONObject): TargetGroupSummary {
        return TargetGroupSummary(
            group = TargetGroup.valueOf(json.getString("group")),
            availableCount = json.getInt("availableCount"),
            totalCount = json.getInt("totalCount"),
        )
    }

    private fun encodeSiteResults(results: List<SiteCheckResult>): JSONArray {
        return JSONArray().apply {
            results.forEach { site ->
                put(
                    JSONObject().apply {
                        put("targetName", site.target.name)
                        put("targetUrl", site.target.url)
                        put("targetGroup", site.target.group.name)
                        put("available", site.available)
                        putOpt("httpCode", site.httpCode)
                        putOpt("error", site.error)
                        put("errorType", site.errorType.name)
                        put("durationMs", site.durationMs)
                    },
                )
            }
        }
    }

    private fun decodeSiteResults(array: JSONArray): List<SiteCheckResult> {
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    SiteCheckResult(
                        target = CheckTarget(
                            name = item.getString("targetName"),
                            url = item.getString("targetUrl"),
                            group = TargetGroup.valueOf(item.getString("targetGroup")),
                        ),
                        available = item.getBoolean("available"),
                        httpCode = if (item.has("httpCode") && !item.isNull("httpCode")) {
                            item.getInt("httpCode")
                        } else {
                            null
                        },
                        error = item.optString("error").takeIf { it.isNotBlank() },
                        errorType = runCatching {
                            SiteCheckErrorType.valueOf(item.getString("errorType"))
                        }.getOrDefault(SiteCheckErrorType.NONE),
                        durationMs = item.getLong("durationMs"),
                    ),
                )
            }
        }
    }

    private fun parseWhitelistState(value: String): WhitelistState {
        return runCatching { WhitelistState.valueOf(value) }.getOrDefault(WhitelistState.UNKNOWN)
    }
}
