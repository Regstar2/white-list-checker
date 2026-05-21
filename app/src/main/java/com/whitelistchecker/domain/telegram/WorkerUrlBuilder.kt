package com.whitelistchecker.domain.telegram

class WorkerUrlBuilder {

    fun buildEndpoint(workerUrl: String, method: String): String {
        require(method in ALLOWED_METHODS) {
            "Unsupported Worker method: $method"
        }
        val normalizedBaseUrl = workerUrl.trim().trimEnd('/')
        return "$normalizedBaseUrl/tg/$method"
    }

    companion object {
        private val ALLOWED_METHODS = setOf("getMe", "getUpdates", "sendMessage")
    }
}
