package com.whitelistchecker.data.history

import com.whitelistchecker.domain.history.CheckHistoryConfig
import com.whitelistchecker.domain.history.CheckHistoryRepository
import com.whitelistchecker.domain.history.CheckRunTimeRange
import com.whitelistchecker.domain.model.history.CheckRun
import com.whitelistchecker.domain.model.history.CheckRunWithTargetResults
import com.whitelistchecker.domain.model.history.CheckTargetResult

class RoomCheckHistoryRepository(
    private val dao: CheckHistoryDao,
) : CheckHistoryRepository {

    override suspend fun saveCheckRun(
        checkRun: CheckRun,
        targetResults: List<CheckTargetResult>,
    ) {
        dao.insertCheckRunWithTargets(
            run = CheckHistoryEntityMapper.toEntity(checkRun),
            targets = targetResults.map(CheckHistoryEntityMapper::toEntity),
        )
    }

    override suspend fun getLatestCheckRun(): CheckRunWithTargetResults? {
        return dao.getLatestWithTargets()?.let(CheckHistoryEntityMapper::toDomain)
    }

    override suspend fun getRecentCheckRuns(limit: Int): List<CheckRunWithTargetResults> {
        if (limit <= 0) return emptyList()
        return dao.getRecentWithTargets(limit).map(CheckHistoryEntityMapper::toDomain)
    }

    override suspend fun countCheckRuns(): Int = dao.countRuns()

    override suspend fun countTargetResults(): Int = dao.countTargetResults()

    override suspend fun getCheckRunTimeRange(): CheckRunTimeRange? {
        if (dao.countRuns() == 0) return null
        return CheckRunTimeRange(
            oldestAt = dao.getOldestRunAt(),
            newestAt = dao.getNewestRunAt(),
        )
    }

    override suspend fun applyRetentionPolicy(nowMillis: Long): Int {
        val totalRuns = dao.countRuns()
        if (totalRuns == 0) return 0

        val olderThanCutoff = nowMillis - CheckHistoryConfig.MAX_CHECK_RUN_AGE_MS
        val idsToDelete = LinkedHashSet<String>()
        idsToDelete.addAll(dao.getRunIdsOlderThan(olderThanCutoff))

        if (totalRuns > CheckHistoryConfig.MAX_CHECK_RUNS) {
            val excessCount = totalRuns - CheckHistoryConfig.MAX_CHECK_RUNS
            idsToDelete.addAll(dao.getRunIdsOldestFirst().take(excessCount))
        }

        if (idsToDelete.isEmpty()) return 0

        val remainingAfterDelete = totalRuns - idsToDelete.size
        if (remainingAfterDelete <= 0) {
            val latest = dao.getLatestWithTargets()?.run?.id
            if (latest != null) {
                idsToDelete.remove(latest)
            }
        }

        if (idsToDelete.isEmpty()) return 0

        val deletedCount = idsToDelete.size
        dao.deleteRunsByIds(idsToDelete.toList())
        return deletedCount
    }
}
