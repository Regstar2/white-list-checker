package com.whitelistchecker.domain.notifications

import com.whitelistchecker.domain.model.LocalNotificationResult
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.WhitelistMonitorResult
import com.whitelistchecker.domain.model.WhitelistMonitorState
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.domain.model.WhitelistStateChangeType
import com.whitelistchecker.domain.monitor.WhitelistMonitorUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class CheckAndLocalNotifyUseCaseTest {

    private val whitelistMonitorUseCase: WhitelistMonitorUseCase = mock()
    private val localNotificationEventUseCase: LocalNotificationEventUseCase = mock()
    private val useCase = CheckAndLocalNotifyUseCase(
        whitelistMonitorUseCase = whitelistMonitorUseCase,
        localNotificationEventUseCase = localNotificationEventUseCase,
    )

    @Test
    fun execute_noEvent_returnsNullLocalNotificationResult() = runTest {
        val monitorResult = monitorResult(stateChangeEvent = null)
        whenever(whitelistMonitorUseCase.checkAndUpdateState()).thenReturn(monitorResult)
        whenever(
            localNotificationEventUseCase.notifyIfNeeded(null, monitorResult.checkResult),
        ).thenReturn(null)

        val result = useCase.execute()

        assertNull(result.localNotificationResult)
        verify(localNotificationEventUseCase, never()).sendTestOnManualCheck(org.mockito.kotlin.any())
    }

    @Test
    fun execute_whitelistTurnedOn_callsNotificationSender() = runTest {
        val event = stateChangeEvent(WhitelistStateChangeType.WHITELIST_TURNED_ON)
        val monitorResult = monitorResult(stateChangeEvent = event)
        whenever(whitelistMonitorUseCase.checkAndUpdateState()).thenReturn(monitorResult)
        whenever(
            localNotificationEventUseCase.notifyIfNeeded(event, monitorResult.checkResult),
        ).thenReturn(LocalNotificationResult.Success)

        val result = useCase.execute()

        assertEquals(LocalNotificationResult.Success, result.localNotificationResult)
        verify(localNotificationEventUseCase).notifyIfNeeded(event, monitorResult.checkResult)
        verify(localNotificationEventUseCase, never()).sendTestOnManualCheck(org.mockito.kotlin.any())
    }

    @Test
    fun execute_whitelistTurnedOff_callsNotificationSender() = runTest {
        val event = stateChangeEvent(WhitelistStateChangeType.WHITELIST_TURNED_OFF)
        val monitorResult = monitorResult(stateChangeEvent = event)
        whenever(whitelistMonitorUseCase.checkAndUpdateState()).thenReturn(monitorResult)
        whenever(
            localNotificationEventUseCase.notifyIfNeeded(event, monitorResult.checkResult),
        ).thenReturn(LocalNotificationResult.Success)

        val result = useCase.execute()

        assertEquals(LocalNotificationResult.Success, result.localNotificationResult)
    }

    @Test
    fun execute_otherConfirmedChange_doesNotSendTestNotification() = runTest {
        val event = stateChangeEvent(WhitelistStateChangeType.OTHER_CONFIRMED_CHANGE)
        val monitorResult = monitorResult(stateChangeEvent = event)
        whenever(whitelistMonitorUseCase.checkAndUpdateState()).thenReturn(monitorResult)
        whenever(
            localNotificationEventUseCase.notifyIfNeeded(event, monitorResult.checkResult),
        ).thenReturn(null)

        val result = useCase.execute()

        assertNull(result.localNotificationResult)
        verify(localNotificationEventUseCase, never()).sendTestOnManualCheck(org.mockito.kotlin.any())
    }

    private fun monitorResult(stateChangeEvent: WhitelistStateChangeEvent?): WhitelistMonitorResult {
        return WhitelistMonitorResult(
            checkResult = sampleCheckResult(),
            monitorState = WhitelistMonitorState(
                lastConfirmedState = WhitelistState.WHITELIST_OFF,
                pendingState = WhitelistState.UNKNOWN,
                pendingStateCount = 0,
                lastConfirmedAtMillis = null,
            ),
            stateChangeEvent = stateChangeEvent,
        )
    }

    private fun stateChangeEvent(type: WhitelistStateChangeType): WhitelistStateChangeEvent {
        return WhitelistStateChangeEvent(
            oldState = WhitelistState.WHITELIST_OFF,
            newState = WhitelistState.WHITELIST_ON,
            type = type,
            changedAtMillis = 1_000L,
        )
    }

    private fun sampleCheckResult(): NetworkCheckResult {
        return NetworkCheckResult(
            siteResults = emptyList(),
            foreignSummary = TargetGroupSummary(TargetGroup.FOREIGN, 0, 4),
            localSummary = TargetGroupSummary(TargetGroup.LOCAL, 3, 4),
            state = WhitelistState.WHITELIST_ON,
            activeNetworkLabel = "Wi-Fi",
            checkedNetworkLabel = "Mobile",
            checkedAtMillis = 1_000L,
        )
    }
}
