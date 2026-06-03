package com.whitelistchecker.data.history

import com.whitelistchecker.domain.history.CheckHistoryConfig
import com.whitelistchecker.domain.history.CheckHistoryRepository
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

    override suspend fun applyRetentionPolicy(nowMillis: Long) {
        val totalRuns = dao.countRuns()
        if (totalRuns == 0) return

        val olderThanCutoff = nowMillis - CheckHistoryConfig.MAX_CHECK_RUN_AGE_MS
        val idsToDelete = LinkedHashSet<String>()
        idsToDelete.addAll(dao.getRunIdsOlderThan(olderThanCutoff))

        if (totalRuns > CheckHistoryConfig.MAX_CHECK_RUNS) {
            val excessCount = totalRuns - CheckHistoryConfig.MAX_CHECK_RUNS
            idsToDelete.addAll(dao.getRunIdsOldestFirst().take(excessCount))
        }

        if (idsToDelete.isEmpty()) return

        val remainingAfterDelete = totalRuns - idsToDelete.size
        if (remainingAfterDelete <= 0) {
            val latest = dao.getLatestWithTargets()?.run?.id
            if (latest != null) {
                idsToDelete.remove(latest)
            }
        }

        if (idsToDelete.isNotEmpty()) {
            dao.deleteRunsByIds(idsToDelete.toList())
        }
    }
}
