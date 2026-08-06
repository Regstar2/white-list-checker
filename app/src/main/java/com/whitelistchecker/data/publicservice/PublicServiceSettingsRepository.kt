package com.whitelistchecker.data.publicservice

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.whitelistchecker.data.system.AndroidDeviceAliasProvider
import com.whitelistchecker.domain.model.PublicServiceRegistrationState
import com.whitelistchecker.domain.model.PublicServiceSettings
import com.whitelistchecker.domain.model.AreaSource
import com.whitelistchecker.domain.model.OperatorDetectionSource
import com.whitelistchecker.domain.model.OperatorSelectionMode
import com.whitelistchecker.domain.model.PublicServiceCatalog
import com.whitelistchecker.domain.model.PublicServiceStatus
import com.whitelistchecker.domain.model.UserArea
import com.whitelistchecker.domain.model.DetectedOperator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.publicServiceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "public_service",
)

class PublicServiceSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val defaultDeviceAlias: String = PublicServiceSettings.DEFAULT_DEVICE_ALIAS,
) {

    constructor(context: Context) : this(
        dataStore = context.applicationContext.publicServiceDataStore,
        defaultDeviceAlias = AndroidDeviceAliasProvider(context.applicationContext).getDefaultAlias(),
    )

    fun observeSettings(): Flow<PublicServiceSettings> {
        return dataStore.data.map { preferences -> preferences.toSettings() }
    }

    fun observeStatus(): Flow<PublicServiceStatus> {
        return dataStore.data.map { preferences -> preferences.toStatus() }
    }

    suspend fun getSettings(): PublicServiceSettings = observeSettings().first()

    suspend fun getStatus(): PublicServiceStatus = observeStatus().first()

    suspend fun saveSettings(settings: PublicServiceSettings) {
        dataStore.edit { preferences ->
            preferences.remove(Keys.OLD_BASE_URL)
            preferences[Keys.INSTALLATION_ID] = settings.installationId
            preferences[Keys.SHARE_REPORTS] = settings.shareReports
            preferences[Keys.ALLOW_REMOTE_CHECKS] = settings.allowRemoteChecks
            preferences[Keys.REGION_CODE] = settings.regionCode
            preferences[Keys.REGION_NAME] = settings.regionName
            settings.cityCode?.let { preferences[Keys.CITY_CODE] = it } ?: preferences.remove(Keys.CITY_CODE)
            settings.cityName?.let { preferences[Keys.CITY_NAME] = it } ?: preferences.remove(Keys.CITY_NAME)
            settings.customCityName?.let { preferences[Keys.CUSTOM_CITY_NAME] = it } ?: preferences.remove(Keys.CUSTOM_CITY_NAME)
            preferences[Keys.AREA_SOURCE] = settings.areaSource.name
            preferences[Keys.AREA_CONFIRMED] = settings.areaConfirmedByUser
            preferences[Keys.AREA_UPDATED_AT] = settings.areaUpdatedAtMillis
            preferences[Keys.OPERATOR_CODE] = settings.operatorCode
            preferences[Keys.OPERATOR_SELECTION_MODE] = settings.operatorSelectionMode.name
            preferences[Keys.OPERATOR_SOURCE] = settings.operatorSource.name
            settings.operatorDisplayName?.let { preferences[Keys.OPERATOR_DISPLAY_NAME] = it }
                ?: preferences.remove(Keys.OPERATOR_DISPLAY_NAME)
            settings.operatorMccMnc?.let { preferences[Keys.OPERATOR_MCC_MNC] = it }
                ?: preferences.remove(Keys.OPERATOR_MCC_MNC)
            preferences[Keys.OPERATOR_UPDATED_AT] = settings.operatorUpdatedAtMillis
            preferences[Keys.DEVICE_ALIAS] = settings.deviceAlias
            preferences[Keys.REGISTRATION_STATE] = settings.registrationState.name
        }
    }

    suspend fun saveRegistration(
        installationId: String,
        state: PublicServiceRegistrationState,
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.INSTALLATION_ID] = installationId
            preferences[Keys.REGISTRATION_STATE] = state.name
        }
    }

    suspend fun saveRegistrationState(state: PublicServiceRegistrationState, error: String? = null) {
        dataStore.edit { preferences ->
            preferences[Keys.REGISTRATION_STATE] = state.name
            if (error.isNullOrBlank()) {
                preferences.remove(Keys.LAST_UPLOAD_ERROR)
            } else {
                preferences[Keys.LAST_UPLOAD_ERROR] = error
            }
        }
    }

    suspend fun saveShareReports(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SHARE_REPORTS] = enabled
        }
    }

    suspend fun saveAllowRemoteChecks(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.ALLOW_REMOTE_CHECKS] = enabled
        }
    }

    suspend fun saveArea(area: UserArea) {
        dataStore.edit { preferences ->
            preferences[Keys.REGION_CODE] = area.regionCode
            preferences[Keys.REGION_NAME] = area.regionName
            area.cityCode?.let { preferences[Keys.CITY_CODE] = it } ?: preferences.remove(Keys.CITY_CODE)
            area.cityName?.let { preferences[Keys.CITY_NAME] = it } ?: preferences.remove(Keys.CITY_NAME)
            area.customCityName?.let { preferences[Keys.CUSTOM_CITY_NAME] = it } ?: preferences.remove(Keys.CUSTOM_CITY_NAME)
            preferences[Keys.AREA_SOURCE] = area.source.name
            preferences[Keys.AREA_CONFIRMED] = area.confirmedByUser
            preferences[Keys.AREA_UPDATED_AT] = area.updatedAtMillis
        }
    }

    suspend fun saveDetectedOperator(
        operator: DetectedOperator,
        selectionMode: OperatorSelectionMode,
        nowMillis: Long,
    ) {
        dataStore.edit { preferences ->
            preferences[Keys.OPERATOR_CODE] = operator.operatorCode ?: PublicServiceSettings.DEFAULT_OPERATOR_CODE
            preferences[Keys.OPERATOR_SELECTION_MODE] = selectionMode.name
            preferences[Keys.OPERATOR_SOURCE] = operator.source.name
            operator.displayName?.let { preferences[Keys.OPERATOR_DISPLAY_NAME] = it }
                ?: preferences.remove(Keys.OPERATOR_DISPLAY_NAME)
            operator.mccMnc?.let { preferences[Keys.OPERATOR_MCC_MNC] = it }
                ?: preferences.remove(Keys.OPERATOR_MCC_MNC)
            preferences[Keys.OPERATOR_UPDATED_AT] = nowMillis
        }
    }

    suspend fun recordUploadSuccess(nowMillis: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.LAST_UPLOAD_AT] = nowMillis
            preferences.remove(Keys.LAST_UPLOAD_ERROR)
        }
    }

    suspend fun recordUploadError(error: String?) {
        dataStore.edit { preferences ->
            if (error.isNullOrBlank()) {
                preferences.remove(Keys.LAST_UPLOAD_ERROR)
            } else {
                preferences[Keys.LAST_UPLOAD_ERROR] = error
            }
        }
    }

    suspend fun savePendingReportCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.PENDING_REPORT_COUNT] = count
        }
    }

    suspend fun saveLinkCode(code: String, expiresAtMillis: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.LAST_LINK_CODE] = code
            preferences[Keys.LAST_LINK_CODE_EXPIRES_AT] = expiresAtMillis
            preferences.remove(Keys.LAST_LINK_ERROR)
        }
    }

    suspend fun saveLinkError(error: String?) {
        dataStore.edit { preferences ->
            if (error.isNullOrBlank()) {
                preferences.remove(Keys.LAST_LINK_ERROR)
            } else {
                preferences[Keys.LAST_LINK_ERROR] = error
            }
        }
    }

    suspend fun saveLinkedChatsCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[Keys.LINKED_CHATS_COUNT] = count
        }
    }

    suspend fun recordServiceSyncSuccess(nowMillis: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.LAST_SERVICE_SYNC_AT] = nowMillis
            preferences.remove(Keys.LAST_SERVICE_SYNC_ERROR)
        }
    }

    suspend fun recordServiceSyncError(error: String?) {
        dataStore.edit { preferences ->
            if (error.isNullOrBlank()) {
                preferences.remove(Keys.LAST_SERVICE_SYNC_ERROR)
            } else {
                preferences[Keys.LAST_SERVICE_SYNC_ERROR] = error
            }
        }
    }

    suspend fun recordRemoteCommand(result: String, nowMillis: Long) {
        dataStore.edit { preferences ->
            preferences[Keys.LAST_REMOTE_COMMAND_AT] = nowMillis
            preferences[Keys.LAST_REMOTE_COMMAND_RESULT] = result
        }
    }

    suspend fun clearRegistration() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.INSTALLATION_ID)
            preferences.remove(Keys.OLD_BASE_URL)
            preferences[Keys.REGISTRATION_STATE] = PublicServiceRegistrationState.NOT_REGISTERED.name
            preferences[Keys.SHARE_REPORTS] = false
            preferences[Keys.ALLOW_REMOTE_CHECKS] = false
        }
    }

    private fun Preferences.toSettings(): PublicServiceSettings {
        val regionCode = this[Keys.REGION_CODE] ?: PublicServiceSettings.DEFAULT_REGION_CODE
        val region = PublicServiceCatalog.regionByCode(regionCode)
        val cityCode = this[Keys.CITY_CODE]
        val city = PublicServiceCatalog.cityByCode(cityCode)
        val operatorCode = this[Keys.OPERATOR_CODE] ?: PublicServiceSettings.DEFAULT_OPERATOR_CODE
        val migratedAreaConfirmed = regionCode != PublicServiceSettings.DEFAULT_REGION_CODE
        val migratedOperatorSource = if (operatorCode != PublicServiceSettings.DEFAULT_OPERATOR_CODE) {
            OperatorDetectionSource.MANUAL
        } else {
            OperatorDetectionSource.UNKNOWN
        }
        val storedDeviceAlias = this[Keys.DEVICE_ALIAS]
            ?.trim()
            ?.takeIf { alias ->
                alias.isNotBlank() && alias != PublicServiceSettings.LEGACY_DEFAULT_DEVICE_ALIAS
            }
        return PublicServiceSettings(
            installationId = this[Keys.INSTALLATION_ID].orEmpty(),
            shareReports = this[Keys.SHARE_REPORTS] ?: false,
            allowRemoteChecks = this[Keys.ALLOW_REMOTE_CHECKS] ?: false,
            regionCode = regionCode,
            regionName = this[Keys.REGION_NAME] ?: region?.label.orEmpty(),
            cityCode = cityCode,
            cityName = this[Keys.CITY_NAME] ?: city?.label,
            customCityName = this[Keys.CUSTOM_CITY_NAME],
            areaSource = parseAreaSource(this[Keys.AREA_SOURCE]),
            areaConfirmedByUser = this[Keys.AREA_CONFIRMED] ?: migratedAreaConfirmed,
            areaUpdatedAtMillis = this[Keys.AREA_UPDATED_AT] ?: 0L,
            operatorCode = operatorCode,
            operatorSelectionMode = parseOperatorSelectionMode(this[Keys.OPERATOR_SELECTION_MODE]),
            operatorSource = parseOperatorSource(this[Keys.OPERATOR_SOURCE]) ?: migratedOperatorSource,
            operatorDisplayName = this[Keys.OPERATOR_DISPLAY_NAME],
            operatorMccMnc = this[Keys.OPERATOR_MCC_MNC],
            operatorUpdatedAtMillis = this[Keys.OPERATOR_UPDATED_AT] ?: 0L,
            deviceAlias = storedDeviceAlias ?: defaultDeviceAlias,
            registrationState = parseRegistrationState(this[Keys.REGISTRATION_STATE]),
        )
    }

    private fun Preferences.toStatus(): PublicServiceStatus {
        return PublicServiceStatus(
            lastUploadAtMillis = this[Keys.LAST_UPLOAD_AT],
            lastUploadError = this[Keys.LAST_UPLOAD_ERROR],
            pendingReportCount = this[Keys.PENDING_REPORT_COUNT] ?: 0,
            lastLinkCode = this[Keys.LAST_LINK_CODE],
            lastLinkCodeExpiresAtMillis = this[Keys.LAST_LINK_CODE_EXPIRES_AT],
            lastLinkError = this[Keys.LAST_LINK_ERROR],
            linkedChatsCount = this[Keys.LINKED_CHATS_COUNT] ?: 0,
            lastServiceSyncAtMillis = this[Keys.LAST_SERVICE_SYNC_AT],
            lastServiceSyncError = this[Keys.LAST_SERVICE_SYNC_ERROR],
            lastRemoteCommandAtMillis = this[Keys.LAST_REMOTE_COMMAND_AT],
            lastRemoteCommandResult = this[Keys.LAST_REMOTE_COMMAND_RESULT],
        )
    }

    private fun parseRegistrationState(value: String?): PublicServiceRegistrationState {
        if (value.isNullOrBlank()) return PublicServiceRegistrationState.NOT_REGISTERED
        return runCatching { PublicServiceRegistrationState.valueOf(value) }
            .getOrDefault(PublicServiceRegistrationState.NOT_REGISTERED)
    }

    private fun parseAreaSource(value: String?): AreaSource {
        if (value.isNullOrBlank()) return AreaSource.MANUAL_SELECTION
        return runCatching { AreaSource.valueOf(value) }
            .getOrDefault(AreaSource.MANUAL_SELECTION)
    }

    private fun parseOperatorSelectionMode(value: String?): OperatorSelectionMode {
        if (value.isNullOrBlank()) return OperatorSelectionMode.AUTO
        return runCatching { OperatorSelectionMode.valueOf(value) }
            .getOrDefault(OperatorSelectionMode.AUTO)
    }

    private fun parseOperatorSource(value: String?): OperatorDetectionSource? {
        if (value.isNullOrBlank()) return null
        return runCatching { OperatorDetectionSource.valueOf(value) }.getOrNull()
    }

    private object Keys {
        val OLD_BASE_URL = stringPreferencesKey("public_service_base_url")
        val INSTALLATION_ID = stringPreferencesKey("public_service_installation_id")
        val SHARE_REPORTS = booleanPreferencesKey("public_service_share_reports")
        val ALLOW_REMOTE_CHECKS = booleanPreferencesKey("public_service_allow_remote_checks")
        val REGION_CODE = stringPreferencesKey("public_service_region_code")
        val REGION_NAME = stringPreferencesKey("public_service_region_name")
        val CITY_CODE = stringPreferencesKey("public_service_city_code")
        val CITY_NAME = stringPreferencesKey("public_service_city_name")
        val CUSTOM_CITY_NAME = stringPreferencesKey("public_service_custom_city_name")
        val AREA_SOURCE = stringPreferencesKey("public_service_area_source")
        val AREA_CONFIRMED = booleanPreferencesKey("public_service_area_confirmed")
        val AREA_UPDATED_AT = longPreferencesKey("public_service_area_updated_at")
        val OPERATOR_CODE = stringPreferencesKey("public_service_operator_code")
        val OPERATOR_SELECTION_MODE = stringPreferencesKey("public_service_operator_selection_mode")
        val OPERATOR_SOURCE = stringPreferencesKey("public_service_operator_source")
        val OPERATOR_DISPLAY_NAME = stringPreferencesKey("public_service_operator_display_name")
        val OPERATOR_MCC_MNC = stringPreferencesKey("public_service_operator_mcc_mnc")
        val OPERATOR_UPDATED_AT = longPreferencesKey("public_service_operator_updated_at")
        val DEVICE_ALIAS = stringPreferencesKey("public_service_device_alias")
        val REGISTRATION_STATE = stringPreferencesKey("public_service_registration_state")
        val LAST_UPLOAD_AT = longPreferencesKey("public_service_last_upload_at")
        val LAST_UPLOAD_ERROR = stringPreferencesKey("public_service_last_upload_error")
        val PENDING_REPORT_COUNT = intPreferencesKey("public_service_pending_report_count")
        val LAST_LINK_CODE = stringPreferencesKey("public_service_last_link_code")
        val LAST_LINK_CODE_EXPIRES_AT = longPreferencesKey("public_service_last_link_code_expires_at")
        val LAST_LINK_ERROR = stringPreferencesKey("public_service_last_link_error")
        val LINKED_CHATS_COUNT = intPreferencesKey("public_service_linked_chats_count")
        val LAST_SERVICE_SYNC_AT = longPreferencesKey("public_service_last_service_sync_at")
        val LAST_SERVICE_SYNC_ERROR = stringPreferencesKey("public_service_last_service_sync_error")
        val LAST_REMOTE_COMMAND_AT = longPreferencesKey("public_service_last_remote_command_at")
        val LAST_REMOTE_COMMAND_RESULT = stringPreferencesKey("public_service_last_remote_command_result")
    }
}
