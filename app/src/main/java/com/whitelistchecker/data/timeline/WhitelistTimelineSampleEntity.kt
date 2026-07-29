package com.whitelistchecker.data.timeline

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "whitelist_timeline_samples",
    indices = [
        Index(value = ["checkedAtMillis"]),
        Index(value = ["binaryState"]),
    ],
)
data class WhitelistTimelineSampleEntity(
    @PrimaryKey
    val checkRunId: String,
    val checkedAtMillis: Long,
    val whitelistState: String,
    val binaryState: String,
    val createdAtMillis: Long,
)
