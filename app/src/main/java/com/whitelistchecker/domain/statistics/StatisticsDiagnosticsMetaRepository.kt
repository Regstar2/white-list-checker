package com.whitelistchecker.domain.statistics

data class StatisticsDiagnosticsMeta(
    val lastRebuildAtMillis: Long? = null,
    val lastCleanupAtMillis: Long? = null,
)

interface StatisticsDiagnosticsMetaRepository {

    suspend fun getMeta(): StatisticsDiagnosticsMeta

    suspend fun recordRebuildCompleted(atMillis: Long)

    suspend fun recordCleanupCompleted(atMillis: Long)
}
