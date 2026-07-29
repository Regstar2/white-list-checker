package com.whitelistchecker.domain.active

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.whitelistchecker.data.active.ActiveMonitoringRepository
import com.whitelistchecker.domain.model.ActiveMonitoringState
import com.whitelistchecker.service.ActiveMonitoringService

class ActiveMonitoringController(
    context: Context,
    private val repository: ActiveMonitoringRepository,
) {
    private val appContext = context.applicationContext

    fun start() {
        val intent = Intent(appContext, ActiveMonitoringService::class.java)
            .setAction(ActiveMonitoringService.ACTION_START)
        ContextCompat.startForegroundService(appContext, intent)
    }

    fun stop() {
        val intent = Intent(appContext, ActiveMonitoringService::class.java)
            .setAction(ActiveMonitoringService.ACTION_STOP)
        appContext.startService(intent)
    }

    fun checkNow() {
        val intent = Intent(appContext, ActiveMonitoringService::class.java)
            .setAction(ActiveMonitoringService.ACTION_CHECK_NOW)
        appContext.startService(intent)
    }

    suspend fun reconcileStateWithProcess() {
        val status = repository.getStatus()
        if (status.state in RUNNING_STATES && !ActiveMonitoringService.isServiceRunning()) {
            repository.saveState(
                state = ActiveMonitoringState.STOPPED_BY_SYSTEM,
                stopReason = "Сервис активного мониторинга больше не работает",
            )
        }
    }

    private companion object {
        val RUNNING_STATES = setOf(
            ActiveMonitoringState.STARTING,
            ActiveMonitoringState.RUNNING,
            ActiveMonitoringState.CHECKING,
            ActiveMonitoringState.STOPPING,
        )
    }
}
