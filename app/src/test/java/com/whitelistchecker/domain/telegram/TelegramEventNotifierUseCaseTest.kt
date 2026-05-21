package com.whitelistchecker.domain.telegram

import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TargetGroupSummary
import com.whitelistchecker.domain.model.TelegramBroadcastResult
import com.whitelistchecker.domain.model.TelegramSendResult
import com.whitelistchecker.domain.model.NetworkCheckResult
import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.model.WhitelistStateChangeEvent
import com.whitelistchecker.domain.model.WhitelistStateChangeType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class TelegramEventNotifierUseCaseTest {

    private val telegramBroadcastUseCase: TelegramBroadcastUseCase = mock()
    private val reportFormatter = TelegramReportFormatter()
    private val useCase = TelegramEventNotifierUseCase(
        telegramBroadcastUseCase = telegramBroadcastUseCase,
        reportFormatter = reportFormatter,
    )

    @Test
    fun notifyIfNeeded_nullEvent_doesNotSend() = runTest {
        val result = useCase.notifyIfNeeded(null, sampleCheckResult())

        assertNull(result)
        verifyNoInteractions(telegramBroadcastUseCase)
    }

    @Test
    fun notifyIfNeeded_otherConfirmedChange_doesNotSend() = runTest {
        val event = WhitelistStateChangeEvent(
            oldState = WhitelistState.WHITELIST_OFF,
            newState = WhitelistState.PARTIAL_PROBLEM,
            type = WhitelistStateChangeType.OTHER_CONFIRMED_CHANGE,
            changedAtMillis = 1_000L,
        )

        val result = useCase.notifyIfNeeded(event, sampleCheckResult())

        assertNull(result)
        verifyNoInteractions(telegramBroadcastUseCase)
    }

    @Test
    fun notifyIfNeeded_whitelistTurnedOn_sends() = runTest {
        val event = WhitelistStateChangeEvent(
            oldState = WhitelistState.WHITELIST_OFF,
            newState = WhitelistState.WHITELIST_ON,
            type = WhitelistStateChangeType.WHITELIST_TURNED_ON,
            changedAtMillis = 1_000L,
        )
        whenever(
            telegramBroadcastUseCase.sendToEnabledRecipients(
                text = any(),
                baseReportId = any(),
                eventType = eq(WhitelistStateChangeType.WHITELIST_TURNED_ON.name),
                oldState = any(),
                newState = any(),
                queueOnFailure = any(),
            ),
        ).thenReturn(TelegramBroadcastResult(sentCount = 1, failedCount = 0, failures = emptyList()))

        val result = useCase.notifyIfNeeded(event, sampleCheckResult())

        assertEquals(TelegramSendResult.Success, result)
        verify(telegramBroadcastUseCase).sendToEnabledRecipients(
            text = any(),
            baseReportId = eq("${WhitelistStateChangeType.WHITELIST_TURNED_ON.name}_1000"),
            eventType = eq(WhitelistStateChangeType.WHITELIST_TURNED_ON.name),
            oldState = eq(WhitelistState.WHITELIST_OFF.name),
            newState = eq(WhitelistState.WHITELIST_ON.name),
            queueOnFailure = eq(true),
        )
    }

    @Test
    fun notifyIfNeeded_whitelistTurnedOff_sends() = runTest {
        val event = WhitelistStateChangeEvent(
            oldState = WhitelistState.WHITELIST_ON,
            newState = WhitelistState.WHITELIST_OFF,
            type = WhitelistStateChangeType.WHITELIST_TURNED_OFF,
            changedAtMillis = 2_000L,
        )
        whenever(
            telegramBroadcastUseCase.sendToEnabledRecipients(
                text = any(),
                baseReportId = any(),
                eventType = eq(WhitelistStateChangeType.WHITELIST_TURNED_OFF.name),
                oldState = any(),
                newState = any(),
                queueOnFailure = any(),
            ),
        ).thenReturn(TelegramBroadcastResult(sentCount = 1, failedCount = 0, failures = emptyList()))

        val result = useCase.notifyIfNeeded(event, sampleCheckResult())

        assertEquals(TelegramSendResult.Success, result)
    }

    private fun sampleCheckResult(): NetworkCheckResult {
        return NetworkCheckResult(
            siteResults = emptyList(),
            foreignSummary = TargetGroupSummary(TargetGroup.FOREIGN, 0, 4),
            localSummary = TargetGroupSummary(TargetGroup.LOCAL, 4, 4),
            state = WhitelistState.WHITELIST_OFF,
            activeNetworkLabel = "Wi-Fi",
            checkedNetworkLabel = "Mobile",
            checkedAtMillis = 1_000L,
        )
    }
}
