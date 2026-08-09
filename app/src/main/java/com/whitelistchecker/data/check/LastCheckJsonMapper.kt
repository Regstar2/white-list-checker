package com.whitelistchecker.data.check

import com.whitelistchecker.domain.model.CheckTarget
import com.whitelistchecker.domain.model.DnsCheckErrorType
import com.whitelistchecker.domain.model.DnsCheckResult
import com.whitelistchecker.domain.model.DnsServerProtocol
import com.whitelistchecker.domain.model.DnsWhitelistSignal
import com.whitelistchecker.domain.model.EditableDnsServer
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
            put("dnsResults", encodeDnsResults(result.dnsResults))
            result.foreignDnsSummary?.let { put("foreignDnsSummary", encodeSummary(it)) }
            result.localDnsSummary?.let { put("localDnsSummary", encodeSummary(it)) }
            put("dnsSignal", result.dnsSignal.name)
            put("siteState", result.siteState.name)
            put("privateDnsActive", result.privateDnsActive)
            putOpt("privateDnsServerName", result.privateDnsServerName)
            put("customDnsUsed", result.customDnsUsed)
        }.toString()
    }

    fun decode(json: String): NetworkCheckResult {
        val root = JSONObject(json)
        val state = parseWhitelistState(root.getString("state"))
        return NetworkCheckResult(
            siteResults = decodeSiteResults(root.getJSONArray("siteResults")),
            foreignSummary = decodeSummary(root.getJSONObject("foreignSummary")),
            localSummary = decodeSummary(root.getJSONObject("localSummary")),
            state = state,
            activeNetworkLabel = root.getString("activeNetworkLabel"),
            checkedNetworkLabel = root.getString("checkedNetworkLabel"),
            checkedAtMillis = root.getLong("checkedAtMillis"),
            error = root.nullableString("error"),
            diagnosticsMessage = root.nullableString("diagnosticsMessage"),
            dnsResults = root.optJSONArray("dnsResults")?.let(::decodeDnsResults).orEmpty(),
            foreignDnsSummary = root.optJSONObject("foreignDnsSummary")?.let(::decodeSummary),
            localDnsSummary = root.optJSONObject("localDnsSummary")?.let(::decodeSummary),
            dnsSignal = parseDnsSignal(root.optString("dnsSignal")),
            siteState = root.optString("siteState")
                .takeIf { it.isNotBlank() }
                ?.let(::parseWhitelistState)
                ?: state,
            privateDnsActive = root.optBoolean("privateDnsActive", false),
            privateDnsServerName = root.nullableString("privateDnsServerName"),
            customDnsUsed = root.optBoolean("customDnsUsed", false),
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
                        httpCode = item.optIntOrNull("httpCode"),
                        error = item.nullableString("error"),
                        errorType = runCatching {
                            SiteCheckErrorType.valueOf(item.getString("errorType"))
                        }.getOrDefault(SiteCheckErrorType.NONE),
                        durationMs = item.getLong("durationMs"),
                    ),
                )
            }
        }
    }

    private fun encodeDnsResults(results: List<DnsCheckResult>): JSONArray {
        return JSONArray().apply {
            results.forEach { result ->
                put(
                    JSONObject().apply {
                        put("server", encodeDnsServer(result.server))
                        put("available", result.available)
                        put("responseTimeMs", result.responseTimeMs)
                        put("errorType", result.errorType.name)
                        putOpt("error", result.error)
                        put("resolvedAddressesCount", result.resolvedAddressesCount)
                    },
                )
            }
        }
    }

    private fun decodeDnsResults(array: JSONArray): List<DnsCheckResult> {
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    DnsCheckResult(
                        server = decodeDnsServer(item.getJSONObject("server")),
                        available = item.getBoolean("available"),
                        responseTimeMs = item.getLong("responseTimeMs"),
                        errorType = runCatching {
                            DnsCheckErrorType.valueOf(item.optString("errorType"))
                        }.getOrDefault(DnsCheckErrorType.UNKNOWN),
                        error = item.nullableString("error"),
                        resolvedAddressesCount = item.optInt("resolvedAddressesCount", 0),
                    ),
                )
            }
        }
    }

    private fun encodeDnsServer(server: EditableDnsServer): JSONObject {
        return JSONObject().apply {
            put("id", server.id)
            put("name", server.name)
            put("address", server.address)
            put("group", server.group.name)
            put("enabled", server.enabled)
            put("builtIn", server.builtIn)
            put("protocol", server.protocol.name)
            put("port", server.port)
        }
    }

    private fun decodeDnsServer(json: JSONObject): EditableDnsServer {
        return EditableDnsServer(
            id = json.getString("id"),
            name = json.getString("name"),
            address = json.getString("address"),
            group = TargetGroup.valueOf(json.getString("group")),
            enabled = json.optBoolean("enabled", true),
            builtIn = json.optBoolean("builtIn", false),
            protocol = runCatching {
                DnsServerProtocol.valueOf(json.optString("protocol"))
            }.getOrDefault(DnsServerProtocol.DNS_UDP_TCP),
            port = json.optInt("port", EditableDnsServer.DEFAULT_DNS_PORT),
        )
    }

    private fun parseWhitelistState(value: String): WhitelistState {
        return runCatching { WhitelistState.valueOf(value) }.getOrDefault(WhitelistState.UNKNOWN)
    }

    private fun parseDnsSignal(value: String): DnsWhitelistSignal {
        return runCatching { DnsWhitelistSignal.valueOf(value) }.getOrDefault(DnsWhitelistSignal.UNKNOWN)
    }

    private fun JSONObject.nullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return getString(key).takeIf { it.isNotBlank() }
    }

    private fun JSONObject.optIntOrNull(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return getInt(key)
    }
}
