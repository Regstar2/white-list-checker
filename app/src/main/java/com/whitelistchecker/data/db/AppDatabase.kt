package com.whitelistchecker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.whitelistchecker.data.history.CheckHistoryDao
import com.whitelistchecker.data.history.CheckRunEntity
import com.whitelistchecker.data.history.CheckTargetResultEntity
import com.whitelistchecker.data.availability.WhitelistAvailabilityDao
import com.whitelistchecker.data.availability.WhitelistAvailabilityEventEntity
import com.whitelistchecker.data.availability.WhitelistAvailabilitySummaryEntity
import com.whitelistchecker.data.availability.WhitelistDailyAvailabilityEntity
import com.whitelistchecker.data.availability.WhitelistTargetAvailabilityEntity
import com.whitelistchecker.data.statistics.CheckStatisticsDao
import com.whitelistchecker.data.statistics.CheckStatisticsSummaryEntity
import com.whitelistchecker.data.statistics.DailyCheckStatisticsEntity
import com.whitelistchecker.data.statistics.NetworkStatisticsEntity
import com.whitelistchecker.data.statistics.RouteKindStatisticsEntity
import com.whitelistchecker.data.statistics.TargetStatisticsEntity
import com.whitelistchecker.data.telegram.PendingTelegramReportDao
import com.whitelistchecker.data.telegram.PendingTelegramReportEntity
import com.whitelistchecker.data.timeline.WhitelistTimelineDao
import com.whitelistchecker.data.timeline.WhitelistTimelineSampleEntity

@Database(
    entities = [
        PendingTelegramReportEntity::class,
        CheckRunEntity::class,
        CheckTargetResultEntity::class,
        CheckStatisticsSummaryEntity::class,
        TargetStatisticsEntity::class,
        RouteKindStatisticsEntity::class,
        NetworkStatisticsEntity::class,
        DailyCheckStatisticsEntity::class,
        WhitelistAvailabilityEventEntity::class,
        WhitelistAvailabilitySummaryEntity::class,
        WhitelistDailyAvailabilityEntity::class,
        WhitelistTargetAvailabilityEntity::class,
        WhitelistTimelineSampleEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pendingTelegramReportDao(): PendingTelegramReportDao

    abstract fun checkHistoryDao(): CheckHistoryDao

    abstract fun checkStatisticsDao(): CheckStatisticsDao

    abstract fun whitelistAvailabilityDao(): WhitelistAvailabilityDao

    abstract fun whitelistTimelineDao(): WhitelistTimelineDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE pending_telegram_reports ADD COLUMN recipientId TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "ALTER TABLE pending_telegram_reports ADD COLUMN chatId TEXT NOT NULL DEFAULT ''",
                )
                db.execSQL(
                    "ALTER TABLE pending_telegram_reports ADD COLUMN recipientName TEXT NOT NULL DEFAULT ''",
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS check_runs (
                        id TEXT NOT NULL PRIMARY KEY,
                        startedAtMillis INTEGER NOT NULL,
                        finishedAtMillis INTEGER NOT NULL,
                        durationMs INTEGER NOT NULL,
                        triggerType TEXT NOT NULL,
                        networkType TEXT NOT NULL,
                        operatorName TEXT,
                        routeMode TEXT NOT NULL,
                        overallStatus TEXT NOT NULL,
                        whitelistState TEXT NOT NULL,
                        successCount INTEGER NOT NULL,
                        failureCount INTEGER NOT NULL,
                        skippedCount INTEGER NOT NULL,
                        appVersion TEXT NOT NULL,
                        schemaVersion INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        checkError TEXT,
                        diagnosticsMessage TEXT
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS check_target_results (
                        id TEXT NOT NULL PRIMARY KEY,
                        checkRunId TEXT NOT NULL,
                        targetId TEXT NOT NULL,
                        targetLabel TEXT NOT NULL,
                        targetHost TEXT NOT NULL,
                        routeKind TEXT NOT NULL,
                        status TEXT NOT NULL,
                        latencyMs INTEGER NOT NULL,
                        httpStatusCode INTEGER,
                        errorCode TEXT,
                        errorCategory TEXT,
                        startedAtMillis INTEGER NOT NULL,
                        finishedAtMillis INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL,
                        FOREIGN KEY(checkRunId) REFERENCES check_runs(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_check_target_results_checkRunId ON check_target_results(checkRunId)",
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS check_statistics_summary (
                        id INTEGER NOT NULL PRIMARY KEY,
                        totalRuns INTEGER NOT NULL,
                        successRuns INTEGER NOT NULL,
                        partialFailureRuns INTEGER NOT NULL,
                        failureRuns INTEGER NOT NULL,
                        cancelledRuns INTEGER NOT NULL,
                        unknownRuns INTEGER NOT NULL,
                        successRate REAL,
                        averageLatencyMs INTEGER,
                        lastRunAt INTEGER,
                        lastSuccessAt INTEGER,
                        lastFailureAt INTEGER,
                        consecutiveFailureCount INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        latencySampleCount INTEGER NOT NULL,
                        latencySumMs INTEGER NOT NULL,
                        schemaVersion INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS target_statistics (
                        targetId TEXT NOT NULL PRIMARY KEY,
                        targetLabel TEXT NOT NULL,
                        targetHost TEXT NOT NULL,
                        totalChecks INTEGER NOT NULL,
                        successChecks INTEGER NOT NULL,
                        failureChecks INTEGER NOT NULL,
                        timeoutChecks INTEGER NOT NULL,
                        successRate REAL,
                        averageLatencyMs INTEGER,
                        lastCheckedAt INTEGER,
                        lastSuccessAt INTEGER,
                        lastFailureAt INTEGER,
                        consecutiveFailureCount INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        latencySampleCount INTEGER NOT NULL,
                        latencySumMs INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS route_kind_statistics (
                        routeKind TEXT NOT NULL PRIMARY KEY,
                        totalChecks INTEGER NOT NULL,
                        successChecks INTEGER NOT NULL,
                        failureChecks INTEGER NOT NULL,
                        successRate REAL,
                        averageLatencyMs INTEGER,
                        lastCheckedAt INTEGER,
                        updatedAt INTEGER NOT NULL,
                        latencySampleCount INTEGER NOT NULL,
                        latencySumMs INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS network_statistics (
                        networkKey TEXT NOT NULL PRIMARY KEY,
                        networkType TEXT NOT NULL,
                        operatorName TEXT,
                        totalRuns INTEGER NOT NULL,
                        successRuns INTEGER NOT NULL,
                        failureRuns INTEGER NOT NULL,
                        successRate REAL,
                        averageLatencyMs INTEGER,
                        lastRunAt INTEGER,
                        updatedAt INTEGER NOT NULL,
                        latencySampleCount INTEGER NOT NULL,
                        latencySumMs INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_check_statistics (
                        date TEXT NOT NULL PRIMARY KEY,
                        totalRuns INTEGER NOT NULL,
                        successRuns INTEGER NOT NULL,
                        partialFailureRuns INTEGER NOT NULL,
                        failureRuns INTEGER NOT NULL,
                        totalTargetChecks INTEGER NOT NULL,
                        successTargetChecks INTEGER NOT NULL,
                        failureTargetChecks INTEGER NOT NULL,
                        averageLatencyMs INTEGER,
                        updatedAt INTEGER NOT NULL,
                        latencySampleCount INTEGER NOT NULL,
                        latencySumMs INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS whitelist_availability_events (
                        id TEXT NOT NULL PRIMARY KEY,
                        targetId TEXT NOT NULL,
                        targetLabel TEXT NOT NULL,
                        previousState TEXT NOT NULL,
                        newState TEXT NOT NULL,
                        transitionType TEXT NOT NULL,
                        detectedAt INTEGER NOT NULL,
                        checkRunId TEXT NOT NULL,
                        routeKind TEXT,
                        networkType TEXT,
                        operatorName TEXT,
                        latencyMs INTEGER,
                        errorCode TEXT,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_whitelist_availability_events_detectedAt ON whitelist_availability_events(detectedAt)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_whitelist_availability_events_targetId ON whitelist_availability_events(targetId)",
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS whitelist_availability_summary (
                        id INTEGER NOT NULL PRIMARY KEY,
                        totalTargets INTEGER NOT NULL,
                        currentlyAvailableTargets INTEGER NOT NULL,
                        currentlyUnavailableTargets INTEGER NOT NULL,
                        unknownTargets INTEGER NOT NULL,
                        totalBecameAvailableEvents INTEGER NOT NULL,
                        totalBecameUnavailableEvents INTEGER NOT NULL,
                        availabilityPercent REAL,
                        lastBecameAvailableAt INTEGER,
                        lastBecameUnavailableAt INTEGER,
                        lastUpdatedAt INTEGER NOT NULL,
                        dataRangeStart INTEGER,
                        dataRangeEnd INTEGER,
                        mostStableTargetLabel TEXT,
                        mostUnstableTargetLabel TEXT,
                        schemaVersion INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS whitelist_daily_availability (
                        date TEXT NOT NULL PRIMARY KEY,
                        availableTargetCount INTEGER NOT NULL,
                        unavailableTargetCount INTEGER NOT NULL,
                        becameAvailableCount INTEGER NOT NULL,
                        becameUnavailableCount INTEGER NOT NULL,
                        availabilityPercent REAL,
                        checkRunCount INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS whitelist_target_availability (
                        targetId TEXT NOT NULL PRIMARY KEY,
                        displayLabel TEXT NOT NULL,
                        currentState TEXT NOT NULL,
                        becameAvailableCount INTEGER NOT NULL,
                        becameUnavailableCount INTEGER NOT NULL,
                        availabilityPercent REAL,
                        lastBecameAvailableAt INTEGER,
                        lastBecameUnavailableAt INTEGER,
                        lastSeenAt INTEGER,
                        unstableScore INTEGER NOT NULL,
                        availableChecks INTEGER NOT NULL,
                        unavailableChecks INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS whitelist_timeline_samples (
                        checkRunId TEXT NOT NULL PRIMARY KEY,
                        checkedAtMillis INTEGER NOT NULL,
                        whitelistState TEXT NOT NULL,
                        binaryState TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_whitelist_timeline_samples_checkedAtMillis ON whitelist_timeline_samples(checkedAtMillis)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_whitelist_timeline_samples_binaryState ON whitelist_timeline_samples(binaryState)",
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO whitelist_timeline_samples (
                        checkRunId,
                        checkedAtMillis,
                        whitelistState,
                        binaryState,
                        createdAtMillis
                    )
                    SELECT
                        id,
                        finishedAtMillis,
                        whitelistState,
                        CASE
                            WHEN whitelistState = 'WHITELIST_ON' THEN 'ON'
                            WHEN whitelistState = 'WHITELIST_OFF' THEN 'OFF'
                            ELSE 'UNKNOWN'
                        END,
                        createdAtMillis
                    FROM check_runs
                    """.trimIndent(),
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "whitelist_checker.db",
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                    )
                    .build()
                    .also { instance = it }
            }
        }
    }
}
