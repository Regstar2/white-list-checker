package com.whitelistchecker.domain.checkrun

import com.whitelistchecker.domain.model.history.CheckTriggerType
import kotlinx.coroutines.sync.Mutex

class CheckExecutionCoordinator {
    private val guard = Mutex()
    private var runningTrigger: CheckTriggerType? = null

    suspend fun <T> runSingle(
        trigger: CheckTriggerType,
        block: suspend () -> T,
    ): CheckExecutionResult<T> {
        if (!guard.tryLock()) {
            return CheckExecutionResult.AlreadyRunning(
                runningTrigger = runningTrigger,
            )
        }
        runningTrigger = trigger
        return try {
            CheckExecutionResult.Completed(block())
        } finally {
            runningTrigger = null
            guard.unlock()
        }
    }

    fun isRunning(): Boolean = guard.isLocked
}

sealed interface CheckExecutionResult<out T> {
    data class Completed<T>(
        val value: T,
    ) : CheckExecutionResult<T>

    data class AlreadyRunning(
        val runningTrigger: CheckTriggerType?,
    ) : CheckExecutionResult<Nothing>
}

class CheckAlreadyRunningException(
    val runningTrigger: CheckTriggerType?,
) : IllegalStateException("Whitelist check is already running")
