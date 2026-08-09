package com.whitelistchecker.domain.checker

import android.net.Network
import com.whitelistchecker.domain.model.EditableDnsServer
import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

class CellularDnsResolver internal constructor(
    private val network: Network,
    servers: List<EditableDnsServer>,
    private val queryExecutor: DnsQueryExecutor = DnsQueryClient(),
) : Dns {

    private val resolvers = servers.filter { it.enabled }.toList()
    private val cache = ConcurrentHashMap<String, List<InetAddress>>()

    override fun lookup(hostname: String): List<InetAddress> {
        val normalized = hostname.trim().lowercase()
        if (normalized.isBlank()) throw UnknownHostException("Hostname is empty")
        cache[normalized]?.let { return it }

        val resolved = resolveUncached(normalized)
        val cached = cache.putIfAbsent(normalized, resolved)
        return cached ?: resolved
    }

    fun cachedHostCount(): Int = cache.size

    private fun resolveUncached(hostname: String): List<InetAddress> {
        if (resolvers.isEmpty()) {
            throw UnknownHostException("No enabled custom DNS resolvers are available")
        }
        val failures = mutableListOf<String>()
        resolvers.forEach { server ->
            val aResult = queryExecutor.query(network, server, hostname, DnsRecordType.A)
            if (aResult.successful && aResult.addresses.isNotEmpty()) {
                return aResult.addresses.distinctBy { it.hostAddress }
            }
            if (!aResult.successful) {
                failures += "${server.name}/A=${aResult.errorType.name}"
            }

            val aaaaResult = queryExecutor.query(network, server, hostname, DnsRecordType.AAAA)
            if (aaaaResult.successful && aaaaResult.addresses.isNotEmpty()) {
                return aaaaResult.addresses.distinctBy { it.hostAddress }
            }
            if (!aaaaResult.successful) {
                failures += "${server.name}/AAAA=${aaaaResult.errorType.name}"
            }
        }
        throw UnknownHostException(
            buildString {
                append("Custom DNS could not resolve ")
                append(hostname)
                if (failures.isNotEmpty()) {
                    append(": ")
                    append(failures.joinToString())
                }
            },
        )
    }
}

class CellularDnsResolverFactory internal constructor(
    private val queryExecutor: DnsQueryExecutor = DnsQueryClient(),
) {
    fun create(
        network: Network,
        servers: List<EditableDnsServer>,
    ): CellularDnsResolver {
        return CellularDnsResolver(
            network = network,
            servers = servers,
            queryExecutor = queryExecutor,
        )
    }
}
