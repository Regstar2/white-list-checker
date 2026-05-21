package com.whitelistchecker.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.whitelistchecker.data.telegram.PendingTelegramReportDao
import com.whitelistchecker.data.telegram.PendingTelegramReportEntity

@Database(
    entities = [PendingTelegramReportEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pendingTelegramReportDao(): PendingTelegramReportDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "whitelist_checker.db",
                ).build().also { instance = it }
            }
        }
    }
}
