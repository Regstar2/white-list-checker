package com.whitelistchecker.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
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
        val constraints = connectedConstraints()
        val request = PeriodicWorkRequestBuilder<WhitelistCheckWorker>(
            normalizedInterval,
            TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun runNow() {
        val request = OneTimeWorkRequestBuilder<WhitelistCheckWorker>()
            .setConstraints(connectedConstraints())
            .build()
        workManager.enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel() {
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    fun reschedule(settings: BackgroundCheckSettings) {
        if (settings.enabled) {
            schedule(settings.normalizedIntervalMinutes)
        } else {
            cancel()
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "whitelist_auto_check"
        private const val ONE_TIME_WORK_NAME = "whitelist_auto_check_now"

        private fun connectedConstraints(): Constraints {
            return Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        }
    }
}
