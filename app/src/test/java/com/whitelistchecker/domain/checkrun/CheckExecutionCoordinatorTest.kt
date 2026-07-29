package com.whitelistchecker.domain.checkrun

import com.whitelistchecker.domain.model.history.CheckTriggerType
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CheckExecutionCoordinatorTest {

    @Test
    fun preventsParallelChecks() = runTest {
        val coordinator = CheckExecutionCoordinator()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()

        val first = async {
            coordinator.runSingle(CheckTriggerType.MANUAL_UI) {
                started.complete(Unit)
                release.await()
                "done"
            }
        }
        started.await()

        val second = coordinator.runSingle(CheckTriggerType.WORK_MANAGER) {
            "second"
        }

        assertTrue(second is CheckExecutionResult.AlreadyRunning)
        release.complete(Unit)
        assertEquals(CheckExecutionResult.Completed("done"), first.await())
    }

    @Test
    fun releasesLockAfterException() = runTest {
        val coordinator = CheckExecutionCoordinator()

        try {
            coordinator.runSingle(CheckTriggerType.MANUAL_UI) {
                error("boom")
            }
        } catch (_: IllegalStateException) {
            // Expected.
        }

        val second = coordinator.runSingle(CheckTriggerType.WORK_MANAGER) { "second" }
        assertEquals(CheckExecutionResult.Completed("second"), second)
    }

    @Test
    fun completesNextCheckAfterPreviousFinished() = runTest {
        val coordinator = CheckExecutionCoordinator()

        val first = coordinator.runSingle(CheckTriggerType.MANUAL_UI) { "first" }
        val second = coordinator.runSingle(CheckTriggerType.WORK_MANAGER) { "second" }

        assertEquals(CheckExecutionResult.Completed("first"), first)
        assertEquals(CheckExecutionResult.Completed("second"), second)
    }
}
