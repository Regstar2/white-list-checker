package com.whitelistchecker.service

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.whitelistchecker.MainActivity
import com.whitelistchecker.R
import com.whitelistchecker.WhitelistCheckerApplication
import com.whitelistchecker.domain.checkrun.CheckExecutionResult
import com.whitelistchecker.domain.model.ActiveMonitoringSettings
import com.whitelistchecker.domain.model.ActiveMonitoringState
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.history.CheckTriggerType
import com.whitelistchecker.domain.notifications.LocalNotificationChannelManager
import com.whitelistchecker.ui.toDisplayDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class ActiveMonitoringService : Service() {

    private val appContainer by lazy {
        (application as WhitelistCheckerApplication).appContainer
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitoringJob: Job? = null
    private var telegramCommandsJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        running.set(true)
        appContainer.channelManager.ensureChannelsCreated()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopFromUser()
            ACTION_CHECK_NOW -> runCheckNowFromNotification()
            ACTION_START, null -> startMonitoring()
            else -> startMonitoring()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTimeout(startId: Int, fgsType: Int) {
        serviceScope.launch {
            stopMonitoring(
                finalState = ActiveMonitoringState.STOPPED_BY_SYSTEM,
                reason = "Android остановил dataSync foreground service по системному лимиту",
            )
        }
    }

    override fun onDestroy() {
        running.set(false)
        monitoringJob?.cancel()
        telegramCommandsJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        startForegroundWithState(
            state = ActiveMonitoringState.STARTING,
            settings = null,
            lastState = WhitelistState.UNKNOWN,
        )
        if (monitoringJob?.isActive == true) return
        monitoringJob = serviceScope.launch {
            val repository = appContainer.activeMonitoringRepository
            try {
                repository.saveState(ActiveMonitoringState.STARTING)
                val backgroundSettings = appContainer.backgroundCheckSettingsRepository.getSettings()
                repository.saveBackgroundWasEnabledBeforeStart(backgroundSettings.enabled)
                if (backgroundSettings.enabled) {
                    appContainer.backgroundCheckScheduler.cancel()
                }
                repository.saveState(ActiveMonitoringState.RUNNING)
                var settings = repository.getSettings()
                startTelegramCommandsIfNeeded()
                startForegroundWithState(
                    state = ActiveMonitoringState.RUNNING,
                    settings = settings,
                    lastState = WhitelistState.UNKNOWN,
                )
                while (true) {
                    settings = repository.getSettings()
                    runOneCheck(
                        triggerType = CheckTriggerType.FOREGROUND_INTERVAL,
                        settings = settings,
                    )
                    delay(settings.normalizedIntervalMinutes * ONE_MINUTE_MS)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                repository.saveState(
                    state = ActiveMonitoringState.ERROR,
                    error = exception.message ?: exception.javaClass.simpleName,
                )
                startForegroundWithState(
                    state = ActiveMonitoringState.ERROR,
                    settings = repository.getSettings(),
                    lastState = WhitelistState.UNKNOWN,
                    extra = exception.message ?: exception.javaClass.simpleName,
                )
            }
        }
    }

    private fun runCheckNowFromNotification() {
        serviceScope.launch {
            val settings = appContainer.activeMonitoringRepository.getSettings()
            runOneCheck(
                triggerType = CheckTriggerType.FOREGROUND_NOTIFICATION_ACTION,
                settings = settings,
            )
        }
    }

    private suspend fun runOneCheck(
        triggerType: CheckTriggerType,
        settings: ActiveMonitoringSettings,
    ) {
        val repository = appContainer.activeMonitoringRepository
        repository.saveState(ActiveMonitoringState.CHECKING)
        startForegroundWithState(
            state = ActiveMonitoringState.CHECKING,
            settings = settings,
            lastState = WhitelistState.UNKNOWN,
        )
        when (
            val result = appContainer.checkAndNotifyUseCase.tryExecute(
                triggerType = triggerType,
                notificationPolicy = settings.notificationPolicy,
                notifyOnAccessRestored = settings.notifyOnAccessRestored,
            )
        ) {
            is CheckExecutionResult.Completed -> {
                val checkResult = result.value.monitorResult.checkResult
                repository.saveLastCheckAt(checkResult.checkedAtMillis)
                repository.saveState(ActiveMonitoringState.RUNNING)
                startForegroundWithState(
                    state = ActiveMonitoringState.RUNNING,
                    settings = settings,
                    lastState = checkResult.state,
                    checkedAtMillis = checkResult.checkedAtMillis,
                )
            }
            is CheckExecutionResult.AlreadyRunning -> {
                repository.saveState(ActiveMonitoringState.RUNNING)
                startForegroundWithState(
                    state = ActiveMonitoringState.RUNNING,
                    settings = settings,
                    lastState = WhitelistState.UNKNOWN,
                    extra = "Проверка уже выполняется",
                )
            }
        }
    }

    private fun stopFromUser() {
        serviceScope.launch {
            stopMonitoring(
                finalState = ActiveMonitoringState.STOPPED,
                reason = "Остановлено пользователем",
            )
        }
    }

    private suspend fun stopMonitoring(
        finalState: ActiveMonitoringState,
        reason: String,
    ) {
        val repository = appContainer.activeMonitoringRepository
        repository.saveState(ActiveMonitoringState.STOPPING)
        monitoringJob?.cancel()
        monitoringJob = null
        telegramCommandsJob?.cancel()
        telegramCommandsJob = null
        val status = repository.getStatus()
        val backgroundSettings = appContainer.backgroundCheckSettingsRepository.getSettings()
        if (status.backgroundWasEnabledBeforeStart && backgroundSettings.enabled) {
            appContainer.backgroundCheckScheduler.reschedule(backgroundSettings)
        }
        repository.saveState(finalState, stopReason = reason)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startTelegramCommandsIfNeeded() {
        if (telegramCommandsJob?.isActive == true) return
        telegramCommandsJob = serviceScope.launch {
            appContainer.telegramCommandListener.runUntilCancelled()
        }
    }

    private fun startForegroundWithState(
        state: ActiveMonitoringState,
        settings: ActiveMonitoringSettings?,
        lastState: WhitelistState,
        checkedAtMillis: Long? = null,
        extra: String? = null,
    ) {
        val notification = buildNotification(
            state = state,
            settings = settings,
            lastState = lastState,
            checkedAtMillis = checkedAtMillis,
            extra = extra,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                ONGOING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(ONGOING_NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(
        state: ActiveMonitoringState,
        settings: ActiveMonitoringSettings?,
        lastState: WhitelistState,
        checkedAtMillis: Long?,
        extra: String?,
    ) = NotificationCompat.Builder(
        this,
        LocalNotificationChannelManager.ACTIVE_MONITORING_CHANNEL_ID,
    )
        .setSmallIcon(R.drawable.ic_stat_whitelist)
        .setContentTitle(getString(R.string.active_monitoring_notification_title))
        .setContentText(notificationText(state, settings, lastState, checkedAtMillis, extra))
        .setStyle(
            NotificationCompat.BigTextStyle().bigText(
                notificationText(state, settings, lastState, checkedAtMillis, extra),
            ),
        )
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setContentIntent(openAppPendingIntent())
        .addAction(
            R.drawable.ic_home_sync,
            getString(R.string.active_monitoring_action_check_now),
            servicePendingIntent(ACTION_CHECK_NOW, REQUEST_CHECK_NOW),
        )
        .addAction(
            R.drawable.ic_home_error,
            getString(R.string.active_monitoring_action_stop),
            servicePendingIntent(ACTION_STOP, REQUEST_STOP),
        )
        .build()

    private fun notificationText(
        state: ActiveMonitoringState,
        settings: ActiveMonitoringSettings?,
        lastState: WhitelistState,
        checkedAtMillis: Long?,
        extra: String?,
    ): String {
        val interval = settings?.normalizedIntervalMinutes ?: ActiveMonitoringSettings.DEFAULT_INTERVAL_MINUTES
        val lines = mutableListOf<String>()
        lines += when (state) {
            ActiveMonitoringState.STARTING -> getString(R.string.active_monitoring_state_starting)
            ActiveMonitoringState.RUNNING -> getString(R.string.active_monitoring_state_running)
            ActiveMonitoringState.CHECKING -> getString(R.string.active_monitoring_state_checking)
            ActiveMonitoringState.STOPPING -> getString(R.string.active_monitoring_state_stopping)
            ActiveMonitoringState.STOPPED_BY_SYSTEM -> getString(R.string.active_monitoring_state_stopped_by_system)
            ActiveMonitoringState.ERROR -> getString(R.string.active_monitoring_state_error)
            ActiveMonitoringState.STOPPED -> getString(R.string.active_monitoring_state_stopped)
        }
        lines += getString(R.string.active_monitoring_notification_interval, interval)
        if (checkedAtMillis != null) {
            lines += getString(R.string.active_monitoring_notification_last_check, checkedAtMillis.toDisplayDateTime())
            lines += getString(
                R.string.active_monitoring_notification_result,
                whitelistStateLabel(lastState),
            )
        }
        if (!extra.isNullOrBlank()) {
            lines += extra
        }
        return lines.joinToString(separator = "\n")
    }

    private fun whitelistStateLabel(state: WhitelistState): String {
        return when (state) {
            WhitelistState.WHITELIST_OFF -> getString(R.string.home_result_state_whitelist_off)
            WhitelistState.WHITELIST_ON -> getString(R.string.home_result_state_whitelist_on)
            WhitelistState.NO_MOBILE_INTERNET -> getString(R.string.home_result_state_no_mobile_internet)
            WhitelistState.MOBILE_DNS_FAILURE -> getString(R.string.home_result_state_mobile_dns_failure)
            WhitelistState.PARTIAL_PROBLEM -> getString(R.string.home_result_state_partial_problem)
            WhitelistState.CELLULAR_NETWORK_UNAVAILABLE -> getString(R.string.home_result_state_cellular_unavailable)
            WhitelistState.UNKNOWN -> getString(R.string.home_result_state_unknown)
        }
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this,
            REQUEST_OPEN_APP,
            intent,
            pendingIntentFlags(),
        )
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, ActiveMonitoringService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            pendingIntentFlags(),
        )
    }

    private fun pendingIntentFlags(): Int {
        return PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    }

    companion object {
        const val ACTION_START = "com.whitelistchecker.active.START"
        const val ACTION_STOP = "com.whitelistchecker.active.STOP"
        const val ACTION_CHECK_NOW = "com.whitelistchecker.active.CHECK_NOW"

        private const val ONGOING_NOTIFICATION_ID = 2001
        private const val REQUEST_OPEN_APP = 2101
        private const val REQUEST_CHECK_NOW = 2102
        private const val REQUEST_STOP = 2103
        private const val ONE_MINUTE_MS = 60_000L
        private val running = AtomicBoolean(false)

        fun isServiceRunning(): Boolean = running.get()

        fun startIntent(context: Context): Intent {
            return Intent(context, ActiveMonitoringService::class.java).setAction(ACTION_START)
        }
    }
}
