package com.whitelistchecker.domain.publicservice

import com.whitelistchecker.data.publicservice.PendingPublicReportRepository
import com.whitelistchecker.data.publicservice.PublicServiceSettingsRepository
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.history.CheckTriggerType

class PublicReportUploadUseCase(
    private val settingsRepository: PublicServiceSettingsRepository,
    private val pendingReportRepository: PendingPublicReportRepository,
    private val registrationUseCase: PublicServiceRegistrationUseCase,
    private val client: PublicServiceClient,
    private val payloadBuilder: PublicReportPayloadBuilder,
) {

    suspend fun onCheckCompleted(
        result: NetworkCheckResult,
        triggerType: CheckTriggerType,
    ) {
        val settings = settingsRepository.getSettings()
        val payload = payloadBuilder.build(result, triggerType, settings) ?: return
        pendingReportRepository.enqueue(
            reportId = payload.reportId,
            payloadJson = payload.json,
            checkedAtMillis = payload.checkedAtMillis,
        )
        flushQueue()
    }

    suspend fun flushQueue(): PublicReportFlushResult {
        val settings = settingsRepository.getSettings()
        if (!settings.shareReports) {
            pendingReportRepository.clear()
            settingsRepository.savePendingReportCount(0)
            return PublicReportFlushResult(skippedCount = 0)
        }
        if (!settings.hasPublicReportContext) {
            val count = pendingReportRepository.count()
            settingsRepository.savePendingReportCount(count)
            return PublicReportFlushResult(skippedCount = count)
        }

        val registered = registrationUseCase.ensureRegistered()
        val token = registrationUseCase.tokenOrThrow()
        val pending = pendingReportRepository.getAll()
            .filter { it.attemptCount < PendingPublicReportRepository.MAX_ATTEMPT_COUNT }

        var attempted = 0
        var sent = 0
        var failed = 0
        var lastError: String? = null

        for (report in pending) {
            val latestSettings = settingsRepository.getSettings()
            if (!latestSettings.shareReports) {
                pendingReportRepository.clear()
                settingsRepository.savePendingReportCount(0)
                return PublicReportFlushResult(
                    attemptedCount = attempted,
                    sentCount = sent,
                    failedCount = failed,
                    skippedCount = 0,
                    lastError = lastError,
                )
            }
            attempted += 1
            try {
                val response = client.uploadReport(registered, token, report.payloadJson)
                if (response.accepted) {
                    sent += 1
                    pendingReportRepository.delete(report.reportId)
                    settingsRepository.recordUploadSuccess(System.currentTimeMillis())
                }
            } catch (exception: Exception) {
                failed += 1
                lastError = exception.message ?: exception.javaClass.simpleName
                pendingReportRepository.markAttempt(report, lastError)
                settingsRepository.recordUploadError(lastError)
                break
            }
        }
        val remaining = pendingReportRepository.count()
        settingsRepository.savePendingReportCount(remaining)
        return PublicReportFlushResult(
            attemptedCount = attempted,
            sentCount = sent,
            failedCount = failed,
            skippedCount = remaining,
            lastError = lastError,
        )
    }

    suspend fun clearPendingReports() {
        pendingReportRepository.clear()
        settingsRepository.savePendingReportCount(0)
    }
}

data class PublicReportFlushResult(
    val attemptedCount: Int = 0,
    val sentCount: Int = 0,
    val failedCount: Int = 0,
    val skippedCount: Int = 0,
    val lastError: String? = null,
)
