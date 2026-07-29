package com.whitelistchecker.domain.model

import java.util.UUID

data class EditableCheckTarget(
    val id: String,
    val name: String,
    val url: String,
    val group: TargetGroup,
    val enabled: Boolean = true,
    val builtIn: Boolean = false,
) {
    fun toCheckTarget(): CheckTarget? {
        if (!enabled) return null
        return CheckTarget(name = name, url = url, group = group)
    }

    companion object {
        fun create(
            name: String,
            url: String,
            group: TargetGroup,
            builtIn: Boolean = false,
            id: String = UUID.randomUUID().toString(),
        ): EditableCheckTarget {
            return EditableCheckTarget(
                id = id,
                name = name,
                url = url,
                group = group,
                builtIn = builtIn,
            )
        }
    }
}
