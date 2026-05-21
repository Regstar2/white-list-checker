package com.whitelistchecker.domain.checker

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.whitelistchecker.data.targets.CheckTargetsRepository
import com.whitelistchecker.domain.classifier.WhitelistStateClassifier
import com.whitelistchecker.domain.model.CheckTarget
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.SiteCheckResult
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class WhitelistCheckUseCase(
    private val connectivityManager: ConnectivityManager,
    private val targetsRepository: CheckTargetsRepository,
    private val cellularNetworkProvider: CellularNetworkProvider,
    private val mobileSiteChecker: MobileSiteChecker,
    private val classifier: WhitelistStateClassifier,
    private val networkDiagnosticsUseCase: NetworkDiagnosticsUseCase,
) {

    suspend fun execute(): NetworkCheckResult {
        val checkedAtMillis = System.currentTimeMillis()
        val activeNetworkLabel = resolveActiveNetworkLabel()
        val checkedNetworkLabel = "Mobile"
        val targets = targetsRepository.getEnabledTargets()
        val emptyForeignSummary = emptySummary(TargetGroup.FOREIGN, targets)
        val emptyLocalSummary = emptySummary(TargetGroup.LOCAL, targets)

        val cellularNetwork = cellularNetworkProvider.requestCellularNetwork()

        if (cellularNetwork == null) {
            cellularNetworkProvider.release()
            return NetworkCheckResult(
                siteResults = emptyList(),
                foreignSummary = emptyForeignSummary,
                localSummary = emptyLocalSummary,
                state = WhitelistState.CELLULAR_NETWORK_UNAVAILABLE,
                activeNetworkLabel = activeNetworkLabel,
                checkedNetworkLabel = checkedNetworkLabel,
                checkedAtMillis = checkedAtMillis,
                error = CELLULAR_UNAVAILABLE_MESSAGE,
            )
        }

        return try {
            val siteResults = coroutineScope {
                targets.map { target ->
                    async {
                        mobileSiteChecker.checkTarget(cellularNetwork, target)
                    }
                }.awaitAll()
            }
            val foreignSummary = buildSummary(TargetGroup.FOREIGN, siteResults)
            val localSummary = buildSummary(TargetGroup.LOCAL, siteResults)
            val state = classifier.classify(foreignSummary, localSummary, siteResults)
            val diagnosticsMessage = if (state == WhitelistState.MOBILE_DNS_FAILURE) {
                networkDiagnosticsUseCase.diagnoseDnsConnectivity(cellularNetwork)
            } else {
                null
            }
            NetworkCheckResult(
                siteResults = siteResults,
                foreignSummary = foreignSummary,
                localSummary = localSummary,
                state = state,
                activeNetworkLabel = activeNetworkLabel,
                checkedNetworkLabel = checkedNetworkLabel,
                checkedAtMillis = checkedAtMillis,
                diagnosticsMessage = diagnosticsMessage,
            )
        } finally {
            cellularNetworkProvider.release()
        }
    }

    private fun buildSummary(
        group: TargetGroup,
        results: List<SiteCheckResult>,
    ): TargetGroupSummary {
        val groupResults = results.filter { it.target.group == group }
        return TargetGroupSummary(
            group = group,
            availableCount = groupResults.count { it.available },
            totalCount = groupResults.size,
        )
    }

    private fun emptySummary(group: TargetGroup, targets: List<CheckTarget>): TargetGroupSummary {
        val totalCount = targets.count { it.group == group }
        return TargetGroupSummary(
            group = group,
            availableCount = 0,
            totalCount = totalCount,
        )
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
