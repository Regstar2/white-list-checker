package com.whitelistchecker.data.telegram

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.whitelistchecker.domain.model.TelegramSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.telegramSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "telegram_settings",
)

class TelegramSettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {

    constructor(context: Context) : this(context.applicationContext.telegramSettingsDataStore)

    fun observeSettings(): Flow<TelegramSettings> {
        return dataStore.data.map { preferences -> preferences.toTelegramSettings() }
    }

    suspend fun getSettings(): TelegramSettings = observeSettings().first()

    suspend fun saveSettings(settings: TelegramSettings) {
        dataStore.edit { preferences ->
            preferences[Keys.ENABLED] = settings.enabled
            preferences[Keys.WORKER_URL] = settings.workerUrl
            preferences[Keys.RELAY_SECRET] = settings.relaySecret
            preferences[Keys.CHAT_ID] = settings.chatId
            if (settings.chatDiscoveryOffset == null) {
                preferences.remove(Keys.CHAT_DISCOVERY_OFFSET)
            } else {
                preferences[Keys.CHAT_DISCOVERY_OFFSET] = settings.chatDiscoveryOffset
            }
        }
    }

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.ENABLED] = enabled
        }
    }

    suspend fun saveChatId(chatId: String) {
        dataStore.edit { preferences ->
            preferences[Keys.CHAT_ID] = chatId
        }
    }

    suspend fun getChatDiscoveryOffset(): Long? {
        return dataStore.data.map { preferences ->
            preferences[Keys.CHAT_DISCOVERY_OFFSET]
        }.first()
    }

    suspend fun saveChatDiscoveryOffset(offset: Long?) {
        dataStore.edit { preferences ->
            if (offset == null) {
                preferences.remove(Keys.CHAT_DISCOVERY_OFFSET)
            } else {
                preferences[Keys.CHAT_DISCOVERY_OFFSET] = offset
            }
        }
    }

    suspend fun clearChatDiscoveryOffset() {
        saveChatDiscoveryOffset(null)
    }

    private fun Preferences.toTelegramSettings(): TelegramSettings {
        return TelegramSettings(
            enabled = this[Keys.ENABLED] ?: false,
            workerUrl = this[Keys.WORKER_URL].orEmpty(),
            relaySecret = this[Keys.RELAY_SECRET].orEmpty(),
            chatId = this[Keys.CHAT_ID].orEmpty(),
            chatDiscoveryOffset = this[Keys.CHAT_DISCOVERY_OFFSET],
        )
    }

    private object Keys {
        val ENABLED = booleanPreferencesKey("telegram_enabled")
        val WORKER_URL = stringPreferencesKey("telegram_worker_url")
        val RELAY_SECRET = stringPreferencesKey("telegram_relay_secret")
        val CHAT_ID = stringPreferencesKey("telegram_chat_id")
        val CHAT_DISCOVERY_OFFSET = longPreferencesKey("telegram_chat_discovery_offset")
    }
}
