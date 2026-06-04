package com.whitelistchecker.domain.availability

import com.whitelistchecker.domain.model.history.CheckRun
import com.whitelistchecker.domain.model.history.CheckTargetResult

class WhitelistAvailabilityWriter(
    private val repository: WhitelistAvailabilityRepository,
) {

    suspend fun onCheckRunSaved(
        checkRun: CheckRun,
        targetResults: List<CheckTargetResult>,
    ): Boolean {
        return try {
            repository.updateFromCheckRun(checkRun, targetResults)
            true
        } catch (_: Exception) {
            false
        }
    }
}
