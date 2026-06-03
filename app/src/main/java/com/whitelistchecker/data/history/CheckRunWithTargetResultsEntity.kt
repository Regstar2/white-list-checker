package com.whitelistchecker.data.history

import androidx.room.Embedded
import androidx.room.Relation

data class CheckRunWithTargetResultsEntity(
    @Embedded
    val run: CheckRunEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "checkRunId",
    )
    val targetResults: List<CheckTargetResultEntity>,
)
