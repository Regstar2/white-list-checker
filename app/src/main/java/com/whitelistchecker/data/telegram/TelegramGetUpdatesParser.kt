package com.whitelistchecker.data.telegram

import com.whitelistchecker.domain.model.TelegramChatCandidate
import com.whitelistchecker.domain.model.TelegramChatType
import org.json.JSONArray
import org.json.JSONObject

object TelegramGetUpdatesParser {

    fun parseResponse(body: String): ParseResult {
        val root = JSONObject(body)
        val ok = root.optBoolean("ok", false)
        if (!ok) {
            val description = root.optString("description").takeIf { it.isNotBlank() }
            return ParseResult.Failure(
                description?.let { "Worker вернул ok=false: $it" }
                    ?: "Worker вернул ok=false",
            )
        }
        val resultArray = root.optJSONArray("result") ?: JSONArray()
        val updates = mutableListOf<RawUpdate>()
        for (index in 0 until resultArray.length()) {
            val updateObject = resultArray.optJSONObject(index) ?: continue
            val updateId = updateObject.optLong("update_id", -1)
            if (updateId < 0) continue
            val message = updateObject.optJSONObject("message")
                ?: updateObject.optJSONObject("edited_message")
            if (message == null) {
                updates.add(RawUpdate(updateId, null))
                continue
            }
            val chat = message.optJSONObject("chat") ?: continue
            val chatId = chatIdAsString(chat)
            updates.add(
                RawUpdate(
                    updateId = updateId,
                    message = RawMessage(
                        chatId = chatId,
                        type = chat.optString("type"),
                        title = chat.optString("title").takeIf { it.isNotBlank() },
                        username = chat.optString("username").takeIf { it.isNotBlank() },
                        firstName = chat.optString("first_name").takeIf { it.isNotBlank() },
                        lastName = chat.optString("last_name").takeIf { it.isNotBlank() },
                        text = message.optString("text").takeIf { it.isNotBlank() },
                    ),
                ),
            )
        }
        return ParseResult.Success(updates)
    }

    fun toCandidates(updates: List<RawUpdate>): List<TelegramChatCandidate> {
        val byChatId = linkedMapOf<String, TelegramChatCandidate>()
        for (update in updates) {
            val message = update.message ?: continue
            val candidate = TelegramChatCandidate(
                chatId = message.chatId,
                type = mapChatType(message.type),
                title = message.title,
                username = message.username,
                firstName = message.firstName,
                lastName = message.lastName,
                sourceMessageText = message.text,
            )
            byChatId[candidate.chatId] = candidate
        }
        return byChatId.values.sortedWith(
            compareByDescending<TelegramChatCandidate> { candidate ->
                candidate.sourceMessageText?.contains("/start", ignoreCase = true) == true
            }.thenBy { it.chatId },
        )
    }

    fun maxUpdateId(updates: List<RawUpdate>): Long? {
        return updates.maxOfOrNull { it.updateId }
    }

    private fun chatIdAsString(chat: JSONObject): String {
        if (!chat.has("id")) return ""
        val idValue = chat.get("id")
        return when (idValue) {
            is Number -> {
                val asLong = idValue.toLong()
                asLong.toString()
            }
            else -> idValue.toString()
        }
    }

    private fun mapChatType(rawType: String?): TelegramChatType {
        return when (rawType?.lowercase()) {
            "private" -> TelegramChatType.PRIVATE
            "group" -> TelegramChatType.GROUP
            "supergroup" -> TelegramChatType.SUPERGROUP
            "channel" -> TelegramChatType.CHANNEL
            else -> TelegramChatType.UNKNOWN
        }
    }

    data class RawUpdate(
        val updateId: Long,
        val message: RawMessage?,
    )

    data class RawMessage(
        val chatId: String,
        val type: String?,
        val title: String?,
        val username: String?,
        val firstName: String?,
        val lastName: String?,
        val text: String?,
    )

    sealed interface ParseResult {
        data class Success(val updates: List<RawUpdate>) : ParseResult
        data class Failure(val reason: String) : ParseResult
    }
}
