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
import com.whitelistchecker.data.telegram.PendingTelegramReportDao
import com.whitelistchecker.data.telegram.PendingTelegramReportEntity

@Database(
    entities = [
        PendingTelegramReportEntity::class,
        CheckRunEntity::class,
        CheckTargetResultEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pendingTelegramReportDao(): PendingTelegramReportDao

    abstract fun checkHistoryDao(): CheckHistoryDao

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

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "whitelist_checker.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
