package com.whitelistchecker.data.publicservice

import java.util.concurrent.TimeUnit

class PendingPublicReportRepository(
    private val dao: PendingPublicReportDao,
) {

    suspend fun getAll(): List<PendingPublicReportEntity> = dao.getAll()

    suspend fun count(): Int = dao.count()

    suspend fun enqueue(reportId: String, payloadJson: String, checkedAtMillis: Long) {
        val nowMillis = System.currentTimeMillis()
        dao.insert(
            PendingPublicReportEntity(
                reportId = reportId,
                payloadJson = payloadJson,
                createdAtMillis = nowMillis,
                checkedAtMillis = checkedAtMillis,
                attemptCount = 0,
                lastAttemptAtMillis = null,
                lastError = null,
            ),
        )
        cleanup(nowMillis)
    }

    suspend fun delete(reportId: String) {
        dao.deleteByReportId(reportId)
    }

    suspend fun markAttempt(entity: PendingPublicReportEntity, error: String?) {
        dao.updateAttempt(
            reportId = entity.reportId,
            attemptCount = entity.attemptCount + 1,
            lastAttemptAtMillis = System.currentTimeMillis(),
            lastError = error,
        )
    }

    suspend fun clear() {
        dao.clear()
    }

    suspend fun cleanup(nowMillis: Long = System.currentTimeMillis()) {
        dao.deleteOlderThan(nowMillis - TimeUnit.DAYS.toMillis(MAX_REPORT_AGE_DAYS))
        val all = dao.getAll()
        if (all.size <= MAX_PENDING_REPORTS) return
        all.take(all.size - MAX_PENDING_REPORTS).forEach { dao.deleteByReportId(it.reportId) }
    }

    companion object {
        const val MAX_PENDING_REPORTS = 100
        const val MAX_ATTEMPT_COUNT = 12
        const val MAX_REPORT_AGE_DAYS = 7L
    }
}
