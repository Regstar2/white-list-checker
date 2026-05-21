package com.whitelistchecker.domain.checker

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.whitelistchecker.domain.classifier.WhitelistStateClassifier
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.WhitelistState

class WhitelistCheckUseCase(
    private val connectivityManager: ConnectivityManager,
    private val cellularNetworkProvider: CellularNetworkProvider,
    private val mobileSiteChecker: MobileSiteChecker,
    private val classifier: WhitelistStateClassifier,
) {

    suspend fun execute(): NetworkCheckResult {
        val checkedAtMillis = System.currentTimeMillis()
        val activeNetworkLabel = resolveActiveNetworkLabel()
        val checkedNetworkLabel = "Mobile"

        val cellularNetwork = cellularNetworkProvider.requestCellularNetwork()

        if (cellularNetwork == null) {
            cellularNetworkProvider.release()
            return NetworkCheckResult(
                google = null,
                yandex = null,
                state = WhitelistState.CELLULAR_NETWORK_UNAVAILABLE,
                activeNetworkLabel = activeNetworkLabel,
                checkedNetworkLabel = checkedNetworkLabel,
                checkedAtMillis = checkedAtMillis,
                error = CELLULAR_UNAVAILABLE_MESSAGE,
            )
        }

        return try {
            val google = mobileSiteChecker.check(
                network = cellularNetwork,
                name = MobileSiteChecker.GOOGLE_NAME,
                url = MobileSiteChecker.GOOGLE_URL,
            )
            val yandex = mobileSiteChecker.check(
                network = cellularNetwork,
                name = MobileSiteChecker.YANDEX_NAME,
                url = MobileSiteChecker.YANDEX_URL,
            )
            val state = classifier.classify(google, yandex)
            NetworkCheckResult(
                google = google,
                yandex = yandex,
                state = state,
                activeNetworkLabel = activeNetworkLabel,
                checkedNetworkLabel = checkedNetworkLabel,
                checkedAtMillis = checkedAtMillis,
                error = null,
            )
        } finally {
            cellularNetworkProvider.release()
        }
    }

    private fun resolveActiveNetworkLabel(): String {
        val network = connectivityManager.activeNetwork ?: return LABEL_UNKNOWN
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return LABEL_UNKNOWN
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> LABEL_WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> LABEL_MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> LABEL_ETHERNET
            else -> LABEL_UNKNOWN
        }
    }

    companion object {
        private const val LABEL_WIFI = "Wi-Fi"
        private const val LABEL_MOBILE = "Mobile"
        private const val LABEL_ETHERNET = "Ethernet"
        private const val LABEL_UNKNOWN = "Unknown"

        const val CELLULAR_UNAVAILABLE_MESSAGE =
            "Мобильная сеть недоступна. Возможные причины: мобильные данные выключены, " +
                "нет SIM, нет сигнала, оператор или прошивка не дали поднять cellular-сеть параллельно Wi-Fi."
    }
}
