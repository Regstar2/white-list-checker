package com.whitelistchecker.domain.checker

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.whitelistchecker.data.dns.DnsServersRepository
import com.whitelistchecker.data.targets.CheckTargetsRepository
import com.whitelistchecker.domain.classifier.DnsWhitelistSignalClassifier
import com.whitelistchecker.domain.classifier.WhitelistStateClassifier
import com.whitelistchecker.domain.model.CheckTarget
import com.whitelistchecker.domain.model.DnsCheckResult
import com.whitelistchecker.domain.model.EditableDnsServer
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.SiteCheckErrorType
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
    private val dnsServersRepository: DnsServersRepository,
    private val cellularNetworkProvider: CellularNetworkProvider,
    private val dnsProbe: CellularDnsProbe,
    private val dnsResolverFactory: CellularDnsResolverFactory,
    private val mobileSiteChecker: MobileSiteChecker,
    private val dnsSignalClassifier: DnsWhitelistSignalClassifier,
    private val classifier: WhitelistStateClassifier,
    @Suppress("UNUSED_PARAMETER") networkDiagnosticsUseCase: NetworkDiagnosticsUseCase,
    private val privateDnsDiagnosticsProvider: PrivateDnsDiagnosticsProvider,
) {

    suspend fun execute(): NetworkCheckResult {
        val checkedAtMillis = System.currentTimeMillis()
        val activeNetworkLabel = resolveActiveNetworkLabel()
        val checkedNetworkLabel = LABEL_MOBILE
        val targets = targetsRepository.getEnabledTargets()
        val dnsServers = dnsServersRepository.getEnabledServers()
        val emptyForeignSummary = emptySiteSummary(TargetGroup.FOREIGN, targets)
        val emptyLocalSummary = emptySiteSummary(TargetGroup.LOCAL, targets)
        val emptyForeignDnsSummary = emptyDnsSummary(TargetGroup.FOREIGN, dnsServers)
        val emptyLocalDnsSummary = emptyDnsSummary(TargetGroup.LOCAL, dnsServers)

        val cellularRequest = cellularNetworkProvider.requestCellularNetwork()
        val cellularNetwork = cellularRequest.network

        if (cellularNetwork == null) {
            cellularNetworkProvider.release()
            val errorMessage = if (cellularRequest.permissionDenied) {
                CHANGE_NETWORK_STATE_DENIED_MESSAGE
            } else {
                CELLULAR_UNAVAILABLE_MESSAGE
            }
            return NetworkCheckResult(
                siteResults = emptyList(),
                foreignSummary = emptyForeignSummary,
                localSummary = emptyLocalSummary,
                state = WhitelistState.CELLULAR_NETWORK_UNAVAILABLE,
                activeNetworkLabel = activeNetworkLabel,
                checkedNetworkLabel = checkedNetworkLabel,
                checkedAtMillis = checkedAtMillis,
                error = errorMessage,
                foreignDnsSummary = emptyForeignDnsSummary,
                localDnsSummary = emptyLocalDnsSummary,
            )
        }

        return try {
            val privateDns = privateDnsDiagnosticsProvider.read(cellularNetwork)
            val dnsResults = dnsProbe.probe(cellularNetwork, dnsServers)
            val foreignDnsSummary = buildDnsSummary(TargetGroup.FOREIGN, dnsResults)
            val localDnsSummary = buildDnsSummary(TargetGroup.LOCAL, dnsResults)
            val dnsSignal = dnsSignalClassifier.classify(foreignDnsSummary, localDnsSummary)
            val availableResolvers = dnsResults
                .filter { it.available }
                .sortedWith(compareBy<DnsCheckResult> { it.responseTimeMs }.thenBy { it.server.id })
                .map { it.server }

            val customDnsUsed = availableResolvers.isNotEmpty()
            val resolver = dnsResolverFactory.create(cellularNetwork, availableResolvers)
            val session = mobileSiteChecker.createSession(cellularNetwork, resolver)
            val siteResults = coroutineScope {
                targets.map { target ->
                    async {
                        session.checkTarget(target)
                    }
                }.awaitAll()
            }

            val foreignSummary = buildSiteSummary(TargetGroup.FOREIGN, siteResults)
            val localSummary = buildSiteSummary(TargetGroup.LOCAL, siteResults)
            val siteState = classifier.classifySites(foreignSummary, localSummary)
            val dnsFailureConfirmed = siteResults.isNotEmpty() && siteResults.all { result ->
                !result.available && result.errorType == SiteCheckErrorType.DNS
            }
            val state = classifier.classify(
                foreignSummary = foreignSummary,
                localSummary = localSummary,
                dnsSignal = dnsSignal,
                dnsFailureConfirmed = dnsFailureConfirmed,
            )
            NetworkCheckResult(
                siteResults = siteResults,
                foreignSummary = foreignSummary,
                localSummary = localSummary,
                state = state,
                activeNetworkLabel = activeNetworkLabel,
                checkedNetworkLabel = checkedNetworkLabel,
                checkedAtMillis = checkedAtMillis,
                diagnosticsMessage = null,
                dnsResults = dnsResults,
                foreignDnsSummary = foreignDnsSummary,
                localDnsSummary = localDnsSummary,
                dnsSignal = dnsSignal,
                siteState = siteState,
                privateDnsActive = privateDns.active,
                privateDnsServerName = privateDns.serverName,
                customDnsUsed = customDnsUsed,
            )
        } finally {
            cellularNetworkProvider.release()
        }
    }

    private fun buildSiteSummary(
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

    private fun buildDnsSummary(
        group: TargetGroup,
        results: List<DnsCheckResult>,
    ): TargetGroupSummary {
        val groupResults = results.filter { it.server.group == group }
        return TargetGroupSummary(
            group = group,
            availableCount = groupResults.count { it.available },
            totalCount = groupResults.size,
        )
    }

    private fun emptySiteSummary(group: TargetGroup, targets: List<CheckTarget>): TargetGroupSummary {
        return TargetGroupSummary(
            group = group,
            availableCount = 0,
            totalCount = targets.count { it.group == group },
        )
    }

    private fun emptyDnsSummary(
        group: TargetGroup,
        servers: List<EditableDnsServer>,
    ): TargetGroupSummary {
        return TargetGroupSummary(
            group = group,
            availableCount = 0,
            totalCount = servers.count { it.group == group },
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

        const val CHANGE_NETWORK_STATE_DENIED_MESSAGE =
            "Не хватает разрешения CHANGE_NETWORK_STATE для запроса мобильной сети. Проверьте AndroidManifest."
    }
}
