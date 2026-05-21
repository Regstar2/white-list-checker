package com.whitelistchecker.domain.model

data class TelegramSettings(
    val enabled: Boolean = false,
    val workerUrl: String = "",
    val relaySecret: String = "",
    /** Legacy single-recipient field. Kept only for migration/backward compatibility. */
    val chatId: String = "",
    val chatDiscoveryOffset: Long? = null,
    val recipients: List<TelegramRecipient> = emptyList(),
) {
    val canTestWorker: Boolean
        get() = workerUrl.isNotBlank() && relaySecret.isNotBlank()

    val isReadyForDiscovery: Boolean
        get() = canTestWorker

    val enabledRecipients: List<TelegramRecipient>
        get() = recipients.filter { it.enabled && it.chatId.isNotBlank() }

    val isConfigured: Boolean
        get() = enabled &&
            workerUrl.isNotBlank() &&
            relaySecret.isNotBlank() &&
            enabledRecipients.isNotEmpty()
}
