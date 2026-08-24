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
    private val fallbackDns: Dns = CellularNetworkDns(network),
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

        return resolveThroughCellularNetwork(hostname, failures)
    }

    private fun resolveThroughCellularNetwork(
        hostname: String,
        customDnsFailures: List<String>,
    ): List<InetAddress> {
        return try {
            val addresses = fallbackDns.lookup(hostname).distinctBy { it.hostAddress }
            if (addresses.isEmpty()) {
                throw UnknownHostException("Cellular network DNS returned no addresses for $hostname")
            }
            addresses
        } catch (exception: UnknownHostException) {
            throw UnknownHostException(
                buildString {
                    append("Could not resolve ")
                    append(hostname)
                    append(" through the cellular Network")
                    if (customDnsFailures.isNotEmpty()) {
                        append(" after custom DNS failures: ")
                        append(customDnsFailures.joinToString())
                    }
                    exception.message?.takeIf { it.isNotBlank() }?.let { message ->
                        append("; fallback=")
                        append(message)
                    }
                },
            )
        }
    }
}

internal class CellularNetworkDns(
    private val network: Network,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val normalized = hostname.trim()
        if (normalized.isBlank()) throw UnknownHostException("Hostname is empty")
        val addresses = network.getAllByName(normalized).toList().distinctBy { it.hostAddress }
        if (addresses.isEmpty()) {
            throw UnknownHostException("Cellular network DNS returned no addresses for $normalized")
        }
        return addresses
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
