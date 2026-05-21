package com.whitelistchecker.domain.model

data class TelegramSettings(
    val enabled: Boolean = false,
    val workerUrl: String = "",
    val relaySecret: String = "",
    val chatId: String = "",
    val chatDiscoveryOffset: Long? = null,
) {
    val canTestWorker: Boolean
        get() = workerUrl.isNotBlank() && relaySecret.isNotBlank()

    val isReadyForDiscovery: Boolean
        get() = canTestWorker

    val isConfigured: Boolean
        get() = enabled &&
            workerUrl.isNotBlank() &&
            relaySecret.isNotBlank() &&
            chatId.isNotBlank()
}
