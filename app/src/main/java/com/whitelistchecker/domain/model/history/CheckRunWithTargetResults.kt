package com.whitelistchecker.domain.model.history

data class CheckRunWithTargetResults(
    val run: CheckRun,
    val targetResults: List<CheckTargetResult>,
)
