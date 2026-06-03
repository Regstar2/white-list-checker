package com.whitelistchecker.domain.history

import com.whitelistchecker.domain.model.history.CheckRun
import com.whitelistchecker.domain.model.history.CheckRunWithTargetResults
import com.whitelistchecker.domain.model.history.CheckTargetResult

interface CheckHistoryRepository {

    suspend fun saveCheckRun(
        checkRun: CheckRun,
        targetResults: List<CheckTargetResult>,
    )

    suspend fun getLatestCheckRun(): CheckRunWithTargetResults?

    suspend fun getRecentCheckRuns(limit: Int): List<CheckRunWithTargetResults>

    suspend fun applyRetentionPolicy(nowMillis: Long)
}
