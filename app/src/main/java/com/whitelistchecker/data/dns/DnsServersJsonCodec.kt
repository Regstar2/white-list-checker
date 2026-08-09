package com.whitelistchecker.data.dns

import com.whitelistchecker.domain.model.DnsServerProtocol
import com.whitelistchecker.domain.model.EditableDnsServer
import com.whitelistchecker.domain.model.TargetGroup
import org.json.JSONArray
import org.json.JSONObject

object DnsServersJsonCodec {

    fun encode(servers: List<EditableDnsServer>): String {
        val array = JSONArray()
        servers.forEach { server ->
            array.put(
                JSONObject()
                    .put("id", server.id)
                    .put("name", server.name)
                    .put("address", server.address)
                    .put("group", server.group.name)
                    .put("enabled", server.enabled)
                    .put("builtIn", server.builtIn)
                    .put("protocol", server.protocol.name)
                    .put("port", server.port),
            )
        }
        return array.toString()
    }

    fun decode(raw: String?): List<EditableDnsServer> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        EditableDnsServer(
                            id = item.optString("id"),
                            name = item.optString("name"),
                            address = item.optString("address"),
                            group = runCatching {
                                TargetGroup.valueOf(item.optString("group"))
                            }.getOrDefault(TargetGroup.FOREIGN),
                            enabled = item.optBoolean("enabled", true),
                            builtIn = item.optBoolean("builtIn", false),
                            protocol = runCatching {
                                DnsServerProtocol.valueOf(item.optString("protocol"))
                            }.getOrDefault(DnsServerProtocol.DNS_UDP_TCP),
                            port = item.optInt("port", EditableDnsServer.DEFAULT_DNS_PORT)
                                .takeIf { it in 1..65535 }
                                ?: EditableDnsServer.DEFAULT_DNS_PORT,
                        ),
                    )
                }
            }.filter { server ->
                server.id.isNotBlank() &&
                    server.name.isNotBlank() &&
                    server.address.isNotBlank()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
