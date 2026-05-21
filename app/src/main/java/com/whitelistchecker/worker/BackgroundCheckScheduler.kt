package com.whitelistchecker.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.whitelistchecker.domain.model.BackgroundCheckSettings
import java.util.concurrent.TimeUnit

class BackgroundCheckScheduler(
    context: Context,
) {

    private val appContext = context.applicationContext
    private val workManager by lazy { WorkManager.getInstance(appContext) }

    fun schedule(intervalMinutes: Long) {
        val normalizedInterval = BackgroundCheckSettings(intervalMinutes = intervalMinutes).normalizedIntervalMinutes
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<WhitelistCheckWorker>(
            normalizedInterval,
            TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    fun reschedule(settings: BackgroundCheckSettings) {
        if (settings.enabled) {
            schedule(settings.normalizedIntervalMinutes)
        } else {
            cancel()
        }
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "whitelist_periodic_check"
    }
}
