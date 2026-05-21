package com.whitelistchecker.domain.classifier

import com.whitelistchecker.domain.model.SiteCheckResult
import com.whitelistchecker.domain.model.WhitelistState

class WhitelistStateClassifier {

    fun classify(google: SiteCheckResult, yandex: SiteCheckResult): WhitelistState {
        val googleAvailable = google.available
        val yandexAvailable = yandex.available
        return when {
            googleAvailable && yandexAvailable -> WhitelistState.WHITELIST_OFF
            !googleAvailable && yandexAvailable -> WhitelistState.WHITELIST_ON
            !googleAvailable && !yandexAvailable -> WhitelistState.NO_MOBILE_INTERNET
            googleAvailable && !yandexAvailable -> WhitelistState.PARTIAL_PROBLEM
            else -> WhitelistState.UNKNOWN
        }
    }
}
