package com.whitelistchecker.data.targets

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.whitelistchecker.data.json.CheckTargetsJsonCodec
import com.whitelistchecker.domain.model.CheckTarget
import com.whitelistchecker.domain.model.EditableCheckTarget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.checkTargetsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "check_targets",
)

class CheckTargetsRepository(
    private val dataStore: DataStore<Preferences>,
) {

    constructor(context: Context) : this(context.applicationContext.checkTargetsDataStore)

    fun observeTargets(): Flow<List<EditableCheckTarget>> {
        return dataStore.data.map { preferences ->
            val stored = CheckTargetsJsonCodec.decode(preferences[Keys.TARGETS_JSON])
            DefaultCheckTargets.mergeNewBuiltIns(stored)
        }
    }

    suspend fun getTargets(): List<EditableCheckTarget> = observeTargets().first()

    suspend fun getEnabledTargets(): List<CheckTarget> {
        return getTargets().mapNotNull { it.toCheckTarget() }
    }

    suspend fun saveTargets(targets: List<EditableCheckTarget>) {
        dataStore.edit { preferences ->
            preferences[Keys.TARGETS_JSON] = CheckTargetsJsonCodec.encode(targets)
        }
    }

    suspend fun addTarget(target: EditableCheckTarget) {
        saveTargets(getTargets() + target)
    }

    suspend fun updateTarget(target: EditableCheckTarget) {
        val updated = getTargets().map { existing ->
            if (existing.id == target.id) target else existing
        }
        saveTargets(updated)
    }

    suspend fun removeTarget(id: String) {
        saveTargets(getTargets().filterNot { it.id == id })
    }

    suspend fun setTargetEnabled(id: String, enabled: Boolean) {
        val updated = getTargets().map { target ->
            if (target.id == id) target.copy(enabled = enabled) else target
        }
        saveTargets(updated)
    }

    suspend fun resetToDefaults() {
        saveTargets(DefaultCheckTargets.defaults())
    }

    private object Keys {
        val TARGETS_JSON = stringPreferencesKey("check_targets_json")
    }
}
