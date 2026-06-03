package com.whitelistchecker.data.history

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "check_target_results",
    foreignKeys = [
        ForeignKey(
            entity = CheckRunEntity::class,
            parentColumns = ["id"],
            childColumns = ["checkRunId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("checkRunId")],
)
data class CheckTargetResultEntity(
    @PrimaryKey
    val id: String,
    val checkRunId: String,
    val targetId: String,
    val targetLabel: String,
    val targetHost: String,
    val routeKind: String,
    val status: String,
    val latencyMs: Long,
    val httpStatusCode: Int?,
    val errorCode: String?,
    val errorCategory: String?,
    val startedAtMillis: Long,
    val finishedAtMillis: Long,
    val createdAtMillis: Long,
)
