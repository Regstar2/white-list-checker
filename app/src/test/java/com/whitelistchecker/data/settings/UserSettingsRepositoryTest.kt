package com.whitelistchecker.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.whitelistchecker.domain.model.AppLanguage
import com.whitelistchecker.domain.model.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSettingsRepositoryTest {

    @Test
    fun getSettings_withoutValues_returnsSystemDefaults() = runTest {
        val repository = UserSettingsRepository(FakePreferencesDataStore())

        val settings = repository.getSettings()

        assertEquals(AppThemeMode.SYSTEM, settings.themeMode)
        assertEquals(AppLanguage.SYSTEM, settings.language)
    }

    @Test
    fun themeMode_persistsAndReadsAllValues() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = UserSettingsRepository(dataStore)

        AppThemeMode.entries.forEach { themeMode ->
            repository.setThemeMode(themeMode)

            assertEquals(themeMode, UserSettingsRepository(dataStore).getSettings().themeMode)
        }
    }

    @Test
    fun language_persistsAndReadsAllValues() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = UserSettingsRepository(dataStore)

        AppLanguage.entries.forEach { language ->
            repository.setLanguage(language)

            assertEquals(language, UserSettingsRepository(dataStore).getSettings().language)
        }
    }

    @Test
    fun addingUserSettings_doesNotClearExistingPreferences() = runTest {
        val existingKey = booleanPreferencesKey("existing_value")
        val dataStore = FakePreferencesDataStore()
        dataStore.edit { preferences ->
            preferences[existingKey] = true
        }

        val repository = UserSettingsRepository(dataStore)
        repository.setThemeMode(AppThemeMode.DARK)
        repository.setLanguage(AppLanguage.ENGLISH)

        assertTrue(dataStore.current()[existingKey] == true)
    }

    private class FakePreferencesDataStore(
        initial: Preferences = emptyPreferences(),
    ) : DataStore<Preferences> {
        private val state = MutableStateFlow(initial)

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            val updated = transform(state.value)
            state.value = updated
            return updated
        }

        fun current(): Preferences = state.value
    }
}
