package com.whitelistchecker.data.json

import com.whitelistchecker.domain.model.EditableCheckTarget
import com.whitelistchecker.domain.model.TargetGroup
import com.whitelistchecker.domain.model.TelegramChatType
import com.whitelistchecker.domain.model.TelegramRecipient
import org.json.JSONArray
import org.json.JSONObject

object TelegramRecipientsJsonCodec {

    fun encode(recipients: List<TelegramRecipient>): String {
        val array = JSONArray()
        recipients.forEach { recipient ->
            array.put(
                JSONObject()
                    .put("id", recipient.id)
                    .put("chatId", recipient.chatId)
                    .put("type", recipient.type.name)
                    .put("displayName", recipient.displayName)
                    .put("username", recipient.username)
                    .put("enabled", recipient.enabled)
                    .put("createdAtMillis", recipient.createdAtMillis),
            )
        }
        return array.toString()
    }

    fun decode(raw: String?): List<TelegramRecipient> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        TelegramRecipient(
                            id = item.optString("id"),
                            chatId = item.optString("chatId"),
                            type = runCatching {
                                TelegramChatType.valueOf(item.optString("type"))
                            }.getOrDefault(TelegramChatType.PRIVATE),
                            displayName = item.optString("displayName"),
                            username = item.optString("username").takeIf { it.isNotBlank() },
                            enabled = item.optBoolean("enabled", true),
                            createdAtMillis = item.optLong("createdAtMillis", System.currentTimeMillis()),
                        ),
                    )
                }
            }.filter { it.id.isNotBlank() && it.chatId.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }
}

object CheckTargetsJsonCodec {

    fun encode(targets: List<EditableCheckTarget>): String {
        val array = JSONArray()
        targets.forEach { target ->
            array.put(
                JSONObject()
                    .put("id", target.id)
                    .put("name", target.name)
                    .put("url", target.url)
                    .put("group", target.group.name)
                    .put("enabled", target.enabled)
                    .put("builtIn", target.builtIn),
            )
        }
        return array.toString()
    }

    fun decode(raw: String?): List<EditableCheckTarget> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        EditableCheckTarget(
                            id = item.optString("id"),
                            name = item.optString("name"),
                            url = item.optString("url"),
                            group = runCatching {
                                TargetGroup.valueOf(item.optString("group"))
                            }.getOrDefault(TargetGroup.FOREIGN),
                            enabled = item.optBoolean("enabled", true),
                            builtIn = item.optBoolean("builtIn", false),
                        ),
                    )
                }
            }.filter { it.id.isNotBlank() && it.name.isNotBlank() && it.url.isNotBlank() }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
