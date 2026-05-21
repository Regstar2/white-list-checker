package com.whitelistchecker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.whitelistchecker.data.telegram.PendingTelegramReportDao
import com.whitelistchecker.data.telegram.PendingTelegramReportEntity

@Database(
    entities = [PendingTelegramReportEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pendingTelegramReportDao(): PendingTelegramReportDao

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

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "whitelist_checker.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
        }
    }
}
