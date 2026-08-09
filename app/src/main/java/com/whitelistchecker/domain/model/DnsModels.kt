package com.whitelistchecker.domain.model

import java.util.UUID

enum class DnsServerProtocol {
    DNS_UDP_TCP,
}

enum class DnsCheckErrorType {
    NONE,
    TIMEOUT,
    CONNECTION,
    INVALID_RESPONSE,
    SERVFAIL,
    NXDOMAIN,
    NETWORK,
    UNKNOWN,
}

enum class DnsWhitelistSignal {
    UNKNOWN,
    WHITELIST_LIKE,
    NORMAL,
    NO_DNS_ACCESS,
    PARTIAL,
}

data class EditableDnsServer(
    val id: String,
    val name: String,
    val address: String,
    val group: TargetGroup,
    val enabled: Boolean = true,
    val builtIn: Boolean = false,
    val protocol: DnsServerProtocol = DnsServerProtocol.DNS_UDP_TCP,
    val port: Int = DEFAULT_DNS_PORT,
) {
    companion object {
        const val DEFAULT_DNS_PORT = 53

        fun create(
            name: String,
            address: String,
            group: TargetGroup,
            protocol: DnsServerProtocol = DnsServerProtocol.DNS_UDP_TCP,
            port: Int = DEFAULT_DNS_PORT,
            builtIn: Boolean = false,
            id: String = UUID.randomUUID().toString(),
        ): EditableDnsServer {
            return EditableDnsServer(
                id = id,
                name = name,
                address = address,
                group = group,
                protocol = protocol,
                port = port,
                builtIn = builtIn,
            )
        }
    }
}

data class DnsCheckResult(
    val server: EditableDnsServer,
    val available: Boolean,
    val responseTimeMs: Long,
    val errorType: DnsCheckErrorType,
    val error: String? = null,
    val resolvedAddressesCount: Int = 0,
)
