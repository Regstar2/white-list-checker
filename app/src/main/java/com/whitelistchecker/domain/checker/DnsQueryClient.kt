package com.whitelistchecker.domain.checker

import android.net.Network
import com.whitelistchecker.domain.model.DnsCheckErrorType
import com.whitelistchecker.domain.model.EditableDnsServer
import java.net.ConnectException
import java.net.InetAddress
import java.net.SocketException
import java.net.SocketTimeoutException

internal data class DnsQueryResult(
    val addresses: List<InetAddress>,
    val errorType: DnsCheckErrorType = DnsCheckErrorType.NONE,
    val error: String? = null,
) {
    val successful: Boolean
        get() = errorType == DnsCheckErrorType.NONE
}

internal interface DnsQueryExecutor {
    fun query(
        network: Network,
        server: EditableDnsServer,
        hostname: String,
        type: DnsRecordType,
        timeoutMs: Int = DnsQueryClient.DEFAULT_TIMEOUT_MS,
    ): DnsQueryResult
}

internal class DnsQueryClient(
    private val transport: DnsTransport = CellularDnsTransport(),
) : DnsQueryExecutor {

    override fun query(
        network: Network,
        server: EditableDnsServer,
        hostname: String,
        type: DnsRecordType,
        timeoutMs: Int,
    ): DnsQueryResult {
        val query = try {
            DnsPacketCodec.buildQuery(hostname, type)
        } catch (exception: Exception) {
            return DnsQueryResult(
                addresses = emptyList(),
                errorType = DnsCheckErrorType.INVALID_RESPONSE,
                error = describe(exception),
            )
        }
        return try {
            val udpParsed = try {
                val udpResponse = transport.queryUdp(network, server, query.bytes, timeoutMs)
                DnsPacketCodec.parseResponse(query, udpResponse)
            } catch (exception: Exception) {
                if (shouldFallbackToTcp(exception)) {
                    null
                } else {
                    throw exception
                }
            }
            val parsed = if (udpParsed == null || udpParsed.truncated) {
                val tcpResponse = transport.queryTcp(network, server, query.bytes, timeoutMs)
                DnsPacketCodec.parseResponse(query, tcpResponse)
            } else {
                udpParsed
            }
            when (parsed.responseCode) {
                DNS_RCODE_NO_ERROR -> DnsQueryResult(addresses = parsed.addresses)
                DNS_RCODE_SERVER_FAILURE -> DnsQueryResult(
                    addresses = emptyList(),
                    errorType = DnsCheckErrorType.SERVFAIL,
                    error = "DNS server returned SERVFAIL",
                )
                DNS_RCODE_NAME_ERROR -> DnsQueryResult(
                    addresses = emptyList(),
                    errorType = DnsCheckErrorType.NXDOMAIN,
                    error = "DNS server returned NXDOMAIN",
                )
                else -> DnsQueryResult(
                    addresses = emptyList(),
                    errorType = DnsCheckErrorType.INVALID_RESPONSE,
                    error = "DNS server returned response code ${parsed.responseCode}",
                )
            }
        } catch (exception: Exception) {
            val errorType = when (exception) {
                is SocketTimeoutException -> DnsCheckErrorType.TIMEOUT
                is ConnectException -> DnsCheckErrorType.CONNECTION
                is SocketException -> DnsCheckErrorType.NETWORK
                is DnsPacketException -> DnsCheckErrorType.INVALID_RESPONSE
                else -> DnsCheckErrorType.UNKNOWN
            }
            DnsQueryResult(
                addresses = emptyList(),
                errorType = errorType,
                error = describe(exception),
            )
        }
    }

    private fun shouldFallbackToTcp(exception: Exception): Boolean {
        return exception is SocketTimeoutException ||
            exception is ConnectException ||
            exception is SocketException
    }

    private fun describe(exception: Exception): String {
        return exception.javaClass.simpleName +
            (exception.message?.let { message -> ": $message" } ?: "")
    }

    companion object {
        const val DEFAULT_TIMEOUT_MS = 2_500
        private const val DNS_RCODE_NO_ERROR = 0
        private const val DNS_RCODE_SERVER_FAILURE = 2
        private const val DNS_RCODE_NAME_ERROR = 3
    }
}
