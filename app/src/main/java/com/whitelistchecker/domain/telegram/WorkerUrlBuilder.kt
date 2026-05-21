package com.whitelistchecker.domain.telegram

class WorkerUrlBuilder {

    fun buildEndpoint(workerUrl: String, method: String): String {
        require(method in ALLOWED_METHODS) {
            "Unsupported Worker method: $method"
        }
        val normalizedBaseUrl = normalizeBaseUrl(workerUrl)
        return "$normalizedBaseUrl/tg/$method"
    }

    private fun normalizeBaseUrl(workerUrl: String): String {
        var base = workerUrl.trim().trimEnd('/')
        val tgPathIndex = base.indexOf("/tg/")
        if (tgPathIndex >= 0) {
            base = base.substring(0, tgPathIndex)
        } else if (base.endsWith("/tg")) {
            base = base.removeSuffix("/tg")
        }
        return base.trimEnd('/')
    }

    companion object {
        const val METHOD_GET_ME = "getMe"
        const val METHOD_GET_UPDATES = "getUpdates"
        const val METHOD_SEND_MESSAGE = "sendMessage"

        private val ALLOWED_METHODS = setOf(
            METHOD_GET_ME,
            METHOD_GET_UPDATES,
            METHOD_SEND_MESSAGE,
        )
    }
}
