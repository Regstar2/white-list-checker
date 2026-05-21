package com.whitelistchecker.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.whitelistchecker.data.background.BackgroundCheckStatusRepository
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.telegram.CheckAndNotifyUseCase

class WhitelistCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val checkAndNotifyUseCase: CheckAndNotifyUseCase,
    private val backgroundCheckStatusRepository: BackgroundCheckStatusRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val startedAt = System.currentTimeMillis()
        backgroundCheckStatusRepository.saveRunStarted(startedAt)

        return try {
            val result = checkAndNotifyUseCase.execute()
            backgroundCheckStatusRepository.saveRunFinished(
                finishedAtMillis = System.currentTimeMillis(),
                state = result.monitorResult.checkResult.state,
                error = null,
                telegramSendResult = result.telegramSendResult,
                queueFlushResult = result.queueFlushResult,
            )
            Result.success()
        } catch (exception: Exception) {
            backgroundCheckStatusRepository.saveRunFinished(
                finishedAtMillis = System.currentTimeMillis(),
                state = WhitelistState.UNKNOWN,
                error = exception.message ?: exception.javaClass.simpleName,
                telegramSendResult = null,
                queueFlushResult = null,
            )
            Result.retry()
        }
    }
}
