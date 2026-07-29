package com.whitelistchecker.domain.publicservice

import android.content.Context
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import com.whitelistchecker.domain.model.DetectedOperator
import com.whitelistchecker.domain.model.OperatorDetectionConfidence
import com.whitelistchecker.domain.model.OperatorDetectionSource
import com.whitelistchecker.domain.model.PublicServiceCatalog

class MobileOperatorDetector(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val telephonyManager = appContext.getSystemService(TelephonyManager::class.java)

    fun detect(): DetectedOperator {
        val manager = telephonyForDataSubscription()
        val networkOperator = runCatching { manager.networkOperator }.getOrNull()
        val networkName = runCatching { manager.networkOperatorName }.getOrNull()
        val byNetworkMcc = PublicServiceCatalog.detectOperatorByMccMnc(networkOperator)
        if (byNetworkMcc != null) {
            return DetectedOperator(
                operatorCode = byNetworkMcc.code,
                displayName = networkName?.ifBlank { byNetworkMcc.label } ?: byNetworkMcc.label,
                mccMnc = networkOperator,
                source = OperatorDetectionSource.NETWORK_OPERATOR,
                confidence = OperatorDetectionConfidence.EXACT_MCC_MNC,
            )
        }
        val byNetworkName = PublicServiceCatalog.detectOperatorByName(networkName)
        if (byNetworkName != null) {
            return DetectedOperator(
                operatorCode = byNetworkName.code,
                displayName = networkName?.ifBlank { byNetworkName.label } ?: byNetworkName.label,
                mccMnc = networkOperator?.ifBlank { null },
                source = OperatorDetectionSource.NETWORK_OPERATOR,
                confidence = OperatorDetectionConfidence.NORMALIZED_NAME,
            )
        }

        val simOperator = runCatching { manager.simOperator }.getOrNull()
        val simName = runCatching { manager.simOperatorName }.getOrNull()
        val bySimMcc = PublicServiceCatalog.detectOperatorByMccMnc(simOperator)
        if (bySimMcc != null) {
            return DetectedOperator(
                operatorCode = bySimMcc.code,
                displayName = simName?.ifBlank { bySimMcc.label } ?: bySimMcc.label,
                mccMnc = simOperator,
                source = OperatorDetectionSource.SIM_OPERATOR,
                confidence = OperatorDetectionConfidence.EXACT_MCC_MNC,
            )
        }
        val bySimName = PublicServiceCatalog.detectOperatorByName(simName)
        if (bySimName != null) {
            return DetectedOperator(
                operatorCode = bySimName.code,
                displayName = simName?.ifBlank { bySimName.label } ?: bySimName.label,
                mccMnc = simOperator?.ifBlank { null },
                source = OperatorDetectionSource.SIM_OPERATOR,
                confidence = OperatorDetectionConfidence.NORMALIZED_NAME,
            )
        }
        return DetectedOperator(
            displayName = networkName?.ifBlank { null } ?: simName?.ifBlank { null },
            mccMnc = networkOperator?.ifBlank { null } ?: simOperator?.ifBlank { null },
            source = OperatorDetectionSource.UNKNOWN,
            confidence = OperatorDetectionConfidence.UNKNOWN,
        )
    }

    private fun telephonyForDataSubscription(): TelephonyManager {
        val subscriptionId = dataSubscriptionId()
        if (subscriptionId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) return telephonyManager
        return runCatching { telephonyManager.createForSubscriptionId(subscriptionId) }.getOrDefault(telephonyManager)
    }

    private fun dataSubscriptionId(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return SubscriptionManager.INVALID_SUBSCRIPTION_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val activeData = runCatching { SubscriptionManager.getActiveDataSubscriptionId() }.getOrDefault(
                SubscriptionManager.INVALID_SUBSCRIPTION_ID,
            )
            if (activeData != SubscriptionManager.INVALID_SUBSCRIPTION_ID) return activeData
        }
        return runCatching { SubscriptionManager.getDefaultDataSubscriptionId() }.getOrDefault(
            SubscriptionManager.INVALID_SUBSCRIPTION_ID,
        )
    }
}
