package com.whitelistchecker.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.whitelistchecker.AppContainer

class WhitelistCheckWorkerFactory(
    private val appContainer: AppContainer,
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? {
        return when (workerClassName) {
            WhitelistCheckWorker::class.java.name -> WhitelistCheckWorker(
                appContext = appContext,
                workerParams = workerParameters,
                checkAndNotifyUseCase = appContainer.checkAndNotifyUseCase,
                backgroundCheckStatusRepository = appContainer.backgroundCheckStatusRepository,
            )
            else -> null
        }
    }
}
