package com.whitelistchecker.domain.publicservice

import com.whitelistchecker.data.publicservice.PublicServiceSettingsRepository
import com.whitelistchecker.domain.model.PublicServiceLink

class PublicServiceLinkUseCase(
    private val settingsRepository: PublicServiceSettingsRepository,
    private val registrationUseCase: PublicServiceRegistrationUseCase,
    private val client: PublicServiceClient,
) {

    suspend fun createLinkCode() {
        val settings = registrationUseCase.saveSettingsToServer(settingsRepository.getSettings())
        val token = registrationUseCase.tokenOrThrow()
        try {
            val response = client.createLinkCode(settings, token)
            settingsRepository.saveLinkCode(response.code, response.expiresAtMillis)
        } catch (exception: Exception) {
            settingsRepository.saveLinkError(exception.message ?: exception.javaClass.simpleName)
            throw exception
        }
    }

    suspend fun refreshLinks(): List<PublicServiceLink> {
        val settings = registrationUseCase.ensureRegistered()
        val token = registrationUseCase.tokenOrThrow()
        val links = client.getLinks(settings, token)
        settingsRepository.saveLinkedChatsCount(links.size)
        return links
    }

    suspend fun revokeLink(linkId: String) {
        val settings = registrationUseCase.ensureRegistered()
        val token = registrationUseCase.tokenOrThrow()
        client.revokeLink(settings, token, linkId)
        refreshLinks()
    }
}
