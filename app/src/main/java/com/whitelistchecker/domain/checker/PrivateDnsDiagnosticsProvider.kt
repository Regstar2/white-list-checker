package com.whitelistchecker.domain.checker

import android.net.ConnectivityManager
import android.net.Network
import android.os.Build

data class PrivateDnsDiagnostics(
    val active: Boolean,
    val serverName: String?,
)

class PrivateDnsDiagnosticsProvider(
    private val connectivityManager: ConnectivityManager,
) {
    fun read(network: Network): PrivateDnsDiagnostics {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return PrivateDnsDiagnostics(active = false, serverName = null)
        }
        val linkProperties = connectivityManager.getLinkProperties(network)
            ?: return PrivateDnsDiagnostics(active = false, serverName = null)
        return PrivateDnsDiagnostics(
            active = linkProperties.isPrivateDnsActive,
            serverName = linkProperties.privateDnsServerName,
        )
    }
}
