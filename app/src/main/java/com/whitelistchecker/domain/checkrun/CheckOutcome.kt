package com.whitelistchecker.domain.checkrun

import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.WhitelistState

sealed interface CheckOutcome {
    val availability: CheckAvailability

    data object Unknown : CheckOutcome {
        override val availability: CheckAvailability = CheckAvailability.UNKNOWN
    }

    data class Success(
        val state: WhitelistState,
    ) : CheckOutcome {
        override val availability: CheckAvailability = CheckAvailability.AVAILABLE
    }

    data class Unavailable(
        val state: WhitelistState,
        val reason: String?,
    ) : CheckOutcome {
        override val availability: CheckAvailability = CheckAvailability.UNAVAILABLE
    }

    data class Failure(
        val error: String,
    ) : CheckOutcome {
        override val availability: CheckAvailability = CheckAvailability.FAILED
    }

    fun validWhitelistStateOrNull(): WhitelistState? {
        val state = when (this) {
            is Success -> state
            else -> null
        }
        return state?.takeIf { it.isValidWhitelistStatus() }
    }

    companion object {
        fun fromResult(result: NetworkCheckResult): CheckOutcome {
            return when (result.state) {
                WhitelistState.NO_MOBILE_INTERNET,
                WhitelistState.MOBILE_DNS_FAILURE,
                WhitelistState.CELLULAR_NETWORK_UNAVAILABLE,
                -> Unavailable(
                    state = result.state,
                    reason = result.error ?: result.diagnosticsMessage,
                )
                else -> {
                    if (!result.error.isNullOrBlank() && result.siteResults.isEmpty()) {
                        Failure(result.error)
                    } else {
                        Success(result.state)
                    }
                }
            }
        }

        fun fromFailure(exception: Throwable): CheckOutcome {
            return Failure(exception.message ?: exception.javaClass.simpleName)
        }
    }
}

fun WhitelistState.isValidWhitelistStatus(): Boolean {
    return this == WhitelistState.WHITELIST_ON || this == WhitelistState.WHITELIST_OFF
}
