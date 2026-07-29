package com.whitelistchecker.data.timeline

import com.whitelistchecker.domain.model.WhitelistState
import com.whitelistchecker.domain.statistics.WhitelistBinaryState
import com.whitelistchecker.domain.statistics.WhitelistTimelineSample

internal object WhitelistTimelineEntityMapper {

    fun toEntity(sample: WhitelistTimelineSample): WhitelistTimelineSampleEntity {
        return WhitelistTimelineSampleEntity(
            checkRunId = sample.checkRunId,
            checkedAtMillis = sample.checkedAtMillis,
            whitelistState = sample.whitelistState.name,
            binaryState = sample.binaryState.name,
            createdAtMillis = sample.createdAtMillis,
        )
    }

    fun toDomain(entity: WhitelistTimelineSampleEntity): WhitelistTimelineSample {
        return WhitelistTimelineSample(
            checkRunId = entity.checkRunId,
            checkedAtMillis = entity.checkedAtMillis,
            whitelistState = parseWhitelistState(entity.whitelistState),
            binaryState = parseBinaryState(entity.binaryState),
            createdAtMillis = entity.createdAtMillis,
        )
    }

    private fun parseWhitelistState(value: String): WhitelistState {
        return runCatching { WhitelistState.valueOf(value) }
            .getOrDefault(WhitelistState.UNKNOWN)
    }

    private fun parseBinaryState(value: String): WhitelistBinaryState {
        return runCatching { WhitelistBinaryState.valueOf(value) }
            .getOrDefault(WhitelistBinaryState.UNKNOWN)
    }
}
