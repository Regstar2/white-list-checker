package com.whitelistchecker.data.timeline

import androidx.room.withTransaction
import com.whitelistchecker.data.db.AppDatabase
import com.whitelistchecker.domain.statistics.WhitelistTimelineConfig
import com.whitelistchecker.domain.statistics.WhitelistTimelineRepository
import com.whitelistchecker.domain.statistics.WhitelistTimelineSample

class RoomWhitelistTimelineRepository(
    private val database: AppDatabase,
    private val dao: WhitelistTimelineDao,
) : WhitelistTimelineRepository {

    override suspend fun saveSample(sample: WhitelistTimelineSample) {
        dao.upsertSample(WhitelistTimelineEntityMapper.toEntity(sample))
    }

    override suspend fun getSamplesSince(cutoffMillis: Long): List<WhitelistTimelineSample> {
        return dao.getSamplesSince(cutoffMillis)
            .map(WhitelistTimelineEntityMapper::toDomain)
    }

    override suspend fun countSamples(): Int = dao.countSamples()

    override suspend fun replaceAll(samples: List<WhitelistTimelineSample>) {
        database.withTransaction {
            dao.clear()
            if (samples.isNotEmpty()) {
                dao.upsertSamples(samples.map(WhitelistTimelineEntityMapper::toEntity))
            }
        }
    }

    override suspend fun clear() {
        dao.clear()
    }

    override suspend fun applyRetentionPolicy(nowMillis: Long): Int {
        return database.withTransaction {
            val before = dao.countSamples()
            dao.deleteOlderThan(nowMillis - WhitelistTimelineConfig.MAX_SAMPLE_AGE_MS)

            val afterAgeRetention = dao.countSamples()
            if (afterAgeRetention > WhitelistTimelineConfig.MAX_SAMPLES) {
                val excess = afterAgeRetention - WhitelistTimelineConfig.MAX_SAMPLES
                val ids = dao.getOldestSampleIds(excess)
                if (ids.isNotEmpty()) {
                    dao.deleteSamplesByIds(ids)
                }
            }

            before - dao.countSamples()
        }
    }
}
