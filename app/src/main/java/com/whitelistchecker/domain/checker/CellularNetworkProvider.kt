package com.whitelistchecker.domain.checker

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

class CellularNetworkProvider(
    private val connectivityManager: ConnectivityManager,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeCallback: ConnectivityManager.NetworkCallback? = null
    private val released = AtomicBoolean(false)

    suspend fun requestCellularNetwork(): CellularNetworkRequestResult =
        suspendCancellableCoroutine { continuation ->
            released.set(false)
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    activeCallback = this
                    if (continuation.isActive) {
                        continuation.resume(CellularNetworkRequestResult(network = network))
                    }
                }

                override fun onUnavailable() {
                    activeCallback = this
                    if (continuation.isActive) {
                        continuation.resume(CellularNetworkRequestResult())
                    }
                }
            }

            activeCallback = callback
            try {
                connectivityManager.requestNetwork(request, callback, NETWORK_TIMEOUT_MS)
            } catch (_: SecurityException) {
                activeCallback = null
                if (continuation.isActive) {
                    continuation.resume(CellularNetworkRequestResult(permissionDenied = true))
                }
            }

            continuation.invokeOnCancellation {
                release()
            }
        }

    fun release() {
        if (!released.compareAndSet(false, true)) return
        val callback = activeCallback ?: return
        activeCallback = null
        mainHandler.post {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: IllegalArgumentException) {
                // already unregistered
            }
        }
    }

    companion object {
        const val NETWORK_TIMEOUT_MS = 8_000
    }
}
