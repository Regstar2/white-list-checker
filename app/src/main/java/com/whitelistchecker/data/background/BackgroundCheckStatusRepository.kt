package com.whitelistchecker.data.background

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.whitelistchecker.domain.model.BackgroundCheckStatus
import com.whitelistchecker.domain.model.TelegramQueueFlushResult
import com.whitelistchecker.domain.model.TelegramSendResult
import com.whitelistchecker.domain.model.WhitelistState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.backgroundCheckStatusDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "background_check_status",
)

class BackgroundCheckStatusRepository(
    private val dataStore: DataStore<Preferences>,
) {

    constructor(context: Context) : this(context.applicationContext.backgroundCheckStatusDataStore)

    suspend fun getStatus(): BackgroundCheckStatus = observeStatus().first()

    fun observeStatus(): Flow<BackgroundCheckStatus> {
        return dataStore.data.map { preferences -> preferences.toStatus() }
    }

    suspend fun saveRunStarted(nowMillis: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.LAST_RUN_AT_MILLIS] = nowMillis
        }
    }

    suspend fun saveRunFinished(
        finishedAtMillis: Long,
        state: WhitelistState,
        error: String?,
        telegramSendResult: TelegramSendResult?,
        queueFlushResult: TelegramQueueFlushResult?,
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.LAST_FINISHED_AT_MILLIS] = finishedAtMillis
            preferences[Keys.LAST_STATE] = state.name
            if (error != null) {
                preferences[Keys.LAST_ERROR] = error
            } else {
                preferences.remove(Keys.LAST_ERROR)
            }
            val telegramSummary = telegramSendResult.toStatusLabel()
            if (telegramSummary != null) {
                preferences[Keys.LAST_TELEGRAM_SEND_RESULT] = telegramSummary
            } else {
                preferences.remove(Keys.LAST_TELEGRAM_SEND_RESULT)
            }
            val queueSummary = queueFlushResult.toSummary()
            if (queueSummary != null) {
                preferences[Keys.LAST_QUEUE_FLUSH_SUMMARY] = queueSummary
            } else {
                preferences.remove(Keys.LAST_QUEUE_FLUSH_SUMMARY)
            }
        }
    }

    private fun Preferences.toStatus(): BackgroundCheckStatus {
        return BackgroundCheckStatus(
            lastRunAtMillis = this[Keys.LAST_RUN_AT_MILLIS],
            lastFinishedAtMillis = this[Keys.LAST_FINISHED_AT_MILLIS],
            lastState = parseWhitelistState(this[Keys.LAST_STATE]),
            lastError = this[Keys.LAST_ERROR],
            lastTelegramSendResult = this[Keys.LAST_TELEGRAM_SEND_RESULT],
            lastQueueFlushSummary = this[Keys.LAST_QUEUE_FLUSH_SUMMARY],
        )
    }

    private fun parseWhitelistState(value: String?): WhitelistState {
        if (value.isNullOrBlank()) return WhitelistState.UNKNOWN
        return runCatching { WhitelistState.valueOf(value) }.getOrDefault(WhitelistState.UNKNOWN)
    }

    private fun TelegramSendResult?.toStatusLabel(): String? = when (this) {
        null -> null
        TelegramSendResult.Success -> "Success"
        is TelegramSendResult.Failure -> "Failure: $reason"
    }

    private fun TelegramQueueFlushResult?.toSummary(): String? {
        if (this == null) return null
        return "отправлено $sentCount, ошибок $failedCount, пропущено $skippedCount"
    }

    private object Keys {
        val LAST_RUN_AT_MILLIS = longPreferencesKey("background_last_run_at_millis")
        val LAST_FINISHED_AT_MILLIS = longPreferencesKey("background_last_finished_at_millis")
        val LAST_STATE = stringPreferencesKey("background_last_state")
        val LAST_ERROR = stringPreferencesKey("background_last_error")
        val LAST_TELEGRAM_SEND_RESULT = stringPreferencesKey("background_last_telegram_send_result")
        val LAST_QUEUE_FLUSH_SUMMARY = stringPreferencesKey("background_last_queue_flush_summary")
    }
}
