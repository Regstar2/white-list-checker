package com.whitelistchecker.domain.checker

import android.net.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress

class NetworkDiagnosticsUseCase {

    suspend fun diagnoseDnsConnectivity(network: Network): String = withContext(Dispatchers.IO) {
        val tcpWorks = checkTcpConnect(network, CLOUDFLARE_IP, HTTPS_PORT) ||
            checkTcpConnect(network, GOOGLE_DNS_IP, HTTPS_PORT)
        if (tcpWorks) {
            "Мобильный интернет есть, но DNS не работает."
        } else {
            "Мобильный интернет, вероятно, недоступен или сильно ограничен."
        }
    }

    fun checkTcpConnect(network: Network, host: String, port: Int): Boolean {
        return try {
            network.socketFactory.createSocket().use { socket ->
                socket.connect(InetSocketAddress(host, port), CONNECT_TIMEOUT_MS)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val CLOUDFLARE_IP = "1.1.1.1"
        private const val GOOGLE_DNS_IP = "8.8.8.8"
        private const val HTTPS_PORT = 443
        private const val CONNECT_TIMEOUT_MS = 4_000
    }
}
