package com.whitelistchecker.domain.publicservice

import com.whitelistchecker.data.publicservice.PublicServiceSettingsRepository
import com.whitelistchecker.domain.checkrun.CheckExecutionResult
import com.whitelistchecker.domain.model.ActiveMonitoringState
import com.whitelistchecker.domain.model.PublicServiceCommandOutcome
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.history.CheckTriggerType
import com.whitelistchecker.domain.system.AppVersionProvider
import com.whitelistchecker.domain.telegram.CheckAndNotifyUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.min
import kotlin.random.Random

class PublicServiceRemoteCommandLoop(
    private val settingsRepository: PublicServiceSettingsRepository,
    private val registrationUseCase: PublicServiceRegistrationUseCase,
    private val publicServiceClient: PublicServiceClient,
    private val checkAndNotifyUseCase: CheckAndNotifyUseCase,
    private val appVersionProvider: AppVersionProvider,
) {

    suspend fun runUntilCancelled(
        serviceSessionId: String,
        serviceStartedAtMillis: Long,
        currentStateProvider: suspend () -> ActiveMonitoringState,
        checkInProgressProvider: suspend () -> Boolean,
    ) {
        var failures = 0
        var nextDelayMs = DEFAULT_POLL_DELAY_MS

        while (currentCoroutineContext().isActive) {
            val settings = settingsRepository.getSettings()
            if (!settings.allowRemoteChecks) {
                settingsRepository.recordServiceSyncError(null)
                delay(IDLE_DELAY_MS)
                continue
            }
            try {
                val registered = registrationUseCase.ensureRegistered()
                val token = registrationUseCase.tokenOrThrow()
                publicServiceClient.saveSettings(registered, token)
                val response = publicServiceClient.serviceSync(
                    settings = registered,
                    token = token,
                    serviceSessionId = serviceSessionId,
                    serviceStartedAtMillis = serviceStartedAtMillis,
                    serviceState = currentStateProvider().name,
                    checkInProgress = checkInProgressProvider(),
                    appVersion = appVersionProvider.versionName(),
                )
                failures = 0
                settingsRepository.recordServiceSyncSuccess(System.currentTimeMillis())
                nextDelayMs = response.nextPollAfterSeconds.coerceAtLeast(5L) * 1000L
                response.command?.let { command ->
                    if (command.expiresAtMillis <= System.currentTimeMillis()) {
                        sendExpiredResult(registered, token, serviceSessionId, command.commandId)
                    } else {
                        executeRemoteCheck(registered, token, serviceSessionId, command.commandId)
                    }
                }
            } catch (exception: Exception) {
                failures += 1
                if (failures >= VISIBLE_ERROR_FAILURES) {
                    settingsRepository.recordServiceSyncError(exception.message ?: exception.javaClass.simpleName)
                }
                nextDelayMs = backoffDelayMs(failures)
            }
            delay(nextDelayMs)
        }
    }

    private suspend fun executeRemoteCheck(
        settings: com.whitelistchecker.domain.model.PublicServiceSettings,
        token: String,
        serviceSessionId: String,
        commandId: String,
    ) {
        val now = System.currentTimeMillis()
        val payload = try {
            when (
                val result = checkAndNotifyUseCase.tryExecute(
                    triggerType = CheckTriggerType.REMOTE_TELEGRAM,
                )
            ) {
                is CheckExecutionResult.AlreadyRunning -> PublicServiceClient.CommandResultPayload(
                    serviceSessionId = serviceSessionId,
                    outcome = PublicServiceCommandOutcome.BUSY,
                    checkedAtMillis = now,
                    errorCode = "CHECK_ALREADY_RUNNING",
                )
                is CheckExecutionResult.Completed -> {
                    val checkResult = result.value.monitorResult.checkResult
                    val mappedState = checkResult.state.toPublicState()
                    if (mappedState == null) {
                        PublicServiceClient.CommandResultPayload(
                            serviceSessionId = serviceSessionId,
                            outcome = PublicServiceCommandOutcome.UNAVAILABLE,
                            checkedAtMillis = checkResult.checkedAtMillis,
                            errorCode = checkResult.state.name,
                        )
                    } else {
                        PublicServiceClient.CommandResultPayload(
                            serviceSessionId = serviceSessionId,
                            outcome = PublicServiceCommandOutcome.SUCCESS,
                            checkedAtMillis = checkResult.checkedAtMillis,
                            whitelistState = mappedState,
                            foreign = PublicServiceClient.CountPayload(
                                available = checkResult.foreignSummary.availableCount,
                                total = checkResult.foreignSummary.totalCount,
                            ),
                            local = PublicServiceClient.CountPayload(
                                available = checkResult.localSummary.availableCount,
                                total = checkResult.localSummary.totalCount,
                            ),
                        )
                    }
                }
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            PublicServiceClient.CommandResultPayload(
                serviceSessionId = serviceSessionId,
                outcome = PublicServiceCommandOutcome.UNAVAILABLE,
                checkedAtMillis = System.currentTimeMillis(),
                errorCode = "CHECK_FAILED",
            )
        }
        publicServiceClient.sendCommandResult(settings, token, commandId, payload)
        settingsRepository.recordRemoteCommand(payload.outcome.name, System.currentTimeMillis())
    }

    private suspend fun sendExpiredResult(
        settings: com.whitelistchecker.domain.model.PublicServiceSettings,
        token: String,
        serviceSessionId: String,
        commandId: String,
    ) {
        publicServiceClient.sendCommandResult(
            settings = settings,
            token = token,
            commandId = commandId,
            payload = PublicServiceClient.CommandResultPayload(
                serviceSessionId = serviceSessionId,
                outcome = PublicServiceCommandOutcome.EXPIRED,
                checkedAtMillis = System.currentTimeMillis(),
                errorCode = "COMMAND_EXPIRED",
            ),
        )
        settingsRepository.recordRemoteCommand(PublicServiceCommandOutcome.EXPIRED.name, System.currentTimeMillis())
    }

    private fun WhitelistState.toPublicState(): String? {
        return when (this) {
            WhitelistState.WHITELIST_ON -> "LIKELY_ENABLED"
            WhitelistState.WHITELIST_OFF -> "LIKELY_DISABLED"
            WhitelistState.PARTIAL_PROBLEM -> "PARTIAL_PROBLEM"
            WhitelistState.MOBILE_DNS_FAILURE -> "MOBILE_DNS_FAILURE"
            WhitelistState.NO_MOBILE_INTERNET,
            WhitelistState.CELLULAR_NETWORK_UNAVAILABLE,
            WhitelistState.UNKNOWN,
            -> null
        }
    }

    private fun backoffDelayMs(failures: Int): Long {
        val base = min(MAX_BACKOFF_MS, INITIAL_BACKOFF_MS * (1L shl failures.coerceAtMost(5)))
        return base + Random.nextLong(0, JITTER_MS)
    }

    private companion object {
        const val DEFAULT_POLL_DELAY_MS = 15_000L
        const val IDLE_DELAY_MS = 10_000L
        const val INITIAL_BACKOFF_MS = 2_000L
        const val MAX_BACKOFF_MS = 60_000L
        const val JITTER_MS = 1_000L
        const val VISIBLE_ERROR_FAILURES = 3
    }
}
