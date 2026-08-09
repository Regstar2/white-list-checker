package com.whitelistchecker.domain.checker

import android.net.Network
import com.whitelistchecker.domain.model.DnsCheckErrorType
import com.whitelistchecker.domain.model.DnsCheckResult
import com.whitelistchecker.domain.model.EditableDnsServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class CellularDnsProbe internal constructor(
    private val queryExecutor: DnsQueryExecutor = DnsQueryClient(),
) {

    suspend fun probe(
        network: Network,
        servers: List<EditableDnsServer>,
    ): List<DnsCheckResult> = coroutineScope {
        servers.filter { it.enabled }.map { server ->
            async(Dispatchers.IO) {
                probeOne(network, server)
            }
        }.awaitAll()
    }

    private fun probeOne(
        network: Network,
        server: EditableDnsServer,
    ): DnsCheckResult {
        val startedAt = System.currentTimeMillis()
        val result = queryExecutor.query(
            network = network,
            server = server,
            hostname = PROBE_HOSTNAME,
            type = DnsRecordType.A,
        )
        val responseTimeMs = System.currentTimeMillis() - startedAt
        val available = result.successful && result.addresses.isNotEmpty()
        return DnsCheckResult(
            server = server,
            available = available,
            responseTimeMs = responseTimeMs,
            errorType = if (available) DnsCheckErrorType.NONE else result.errorType,
            error = if (available) null else result.error ?: "DNS response contains no A records",
            resolvedAddressesCount = result.addresses.size,
        )
    }

    companion object {
        const val PROBE_HOSTNAME = "example.com"
    }
}
