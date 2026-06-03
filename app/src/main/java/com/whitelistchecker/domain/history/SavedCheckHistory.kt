package com.whitelistchecker.domain.history

import com.whitelistchecker.domain.model.history.CheckRun
import com.whitelistchecker.domain.model.history.CheckTargetResult

data class SavedCheckHistory(
    val checkRun: CheckRun,
    val targetResults: List<CheckTargetResult>,
)
