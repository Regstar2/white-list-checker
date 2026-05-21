package com.whitelistchecker.domain.monitor

import com.whitelistchecker.data.monitor.MonitorStateRepository
import com.whitelistchecker.domain.checker.WhitelistCheckUseCase
import com.whitelistchecker.domain.model.WhitelistMonitorResult

class WhitelistMonitorUseCase(
    private val whitelistCheckUseCase: WhitelistCheckUseCase,
    private val monitorStateRepository: MonitorStateRepository,
    private val stateChangeDetector: StateChangeDetector,
) {

    suspend fun checkAndUpdateState(): WhitelistMonitorResult {
        val checkResult = whitelistCheckUseCase.execute()
        val savedState = monitorStateRepository.getState()
        val detection = stateChangeDetector.detect(
            currentState = checkResult.state,
            savedState = savedState,
            nowMillis = System.currentTimeMillis(),
        )
        monitorStateRepository.saveState(detection.updatedMonitorState)
        return WhitelistMonitorResult(
            checkResult = checkResult,
            monitorState = detection.updatedMonitorState,
            stateChangeEvent = detection.event,
        )
    }

    suspend fun loadMonitorState() = monitorStateRepository.getState()
}
