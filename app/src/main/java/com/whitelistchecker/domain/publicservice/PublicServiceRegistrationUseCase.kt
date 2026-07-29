package com.whitelistchecker.domain.publicservice

import com.whitelistchecker.data.publicservice.PublicServiceSettingsRepository
import com.whitelistchecker.data.publicservice.SecureDeviceTokenStore
import com.whitelistchecker.domain.model.PublicServiceRegistrationState
import com.whitelistchecker.domain.model.PublicServiceSettings

class PublicServiceRegistrationUseCase(
    private val settingsRepository: PublicServiceSettingsRepository,
    private val tokenStore: SecureDeviceTokenStore,
    private val client: PublicServiceClient,
    private val appVersionProvider: () -> String,
) {

    suspend fun ensureRegistered(): PublicServiceSettings {
        val settings = settingsRepository.getSettings()
        val token = tokenStore.loadToken()
        if (settings.isRegistered && !token.isNullOrBlank()) {
            return settings
        }

        settingsRepository.saveRegistrationState(PublicServiceRegistrationState.REGISTERING)
        return try {
            val response = client.register(
                appVersion = appVersionProvider(),
            )
            tokenStore.saveToken(response.deviceToken)
            val registered = settings.copy(
                installationId = response.installationId,
                registrationState = PublicServiceRegistrationState.REGISTERED,
            )
            settingsRepository.saveSettings(registered)
            registered
        } catch (exception: Exception) {
            settingsRepository.saveRegistrationState(
                state = PublicServiceRegistrationState.ERROR,
                error = exception.message ?: exception.javaClass.simpleName,
            )
            throw exception
        }
    }

    suspend fun tokenOrThrow(): String {
        return tokenStore.loadToken()
            ?: throw PublicServiceException("DEVICE_TOKEN_MISSING", "Регистрация общего сервиса не найдена")
    }

    suspend fun saveSettingsToServer(settings: PublicServiceSettings): PublicServiceSettings {
        settingsRepository.saveSettings(settings)
        val registered = ensureRegistered()
        client.saveSettings(registered, tokenOrThrow())
        return registered
    }

    suspend fun revokeServerData() {
        val settings = settingsRepository.getSettings()
        val token = tokenStore.loadToken()
        if (settings.isRegistered && !token.isNullOrBlank()) {
            client.revokeInstallation(settings, token)
        }
        tokenStore.clear()
        settingsRepository.clearRegistration()
    }
}
