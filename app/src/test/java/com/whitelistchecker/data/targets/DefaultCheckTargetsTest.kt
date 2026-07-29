package com.whitelistchecker.data.targets

import com.whitelistchecker.domain.model.EditableCheckTarget
import com.whitelistchecker.domain.model.TargetGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultCheckTargetsTest {

    @Test
    fun defaults_includeExpandedForeignAndLocalGroups() {
        val defaults = DefaultCheckTargets.defaults()

        assertTrue(defaults.count { it.group == TargetGroup.FOREIGN } >= 8)
        assertTrue(defaults.count { it.group == TargetGroup.LOCAL } >= 8)
        assertTrue(defaults.all { it.builtIn })
        assertEquals(defaults.size, defaults.map { it.id }.distinct().size)
    }

    @Test
    fun mergeNewBuiltIns_addsNewTargetsWhenLegacyDefaultsAreStillPresent() {
        val legacy = listOf(
            target("old-google", "Google", "https://www.google.com/generate_204", TargetGroup.FOREIGN),
            target("old-cloudflare", "Cloudflare", "https://www.cloudflare.com", TargetGroup.FOREIGN),
            target("old-github", "GitHub", "https://github.com", TargetGroup.FOREIGN),
            target("old-wikipedia", "Wikipedia", "https://www.wikipedia.org", TargetGroup.FOREIGN),
            target("old-yandex", "Yandex", "https://ya.ru", TargetGroup.LOCAL),
            target("old-vk", "VK", "https://vk.com", TargetGroup.LOCAL),
            target("old-mailru", "Mail.ru", "https://mail.ru", TargetGroup.LOCAL),
            target("old-gosuslugi", "Gosuslugi", "https://www.gosuslugi.ru", TargetGroup.LOCAL),
        )

        val merged = DefaultCheckTargets.mergeNewBuiltIns(legacy)

        assertTrue(merged.size > legacy.size)
        assertTrue(merged.any { it.url == "https://www.mozilla.org" })
        assertTrue(merged.any { it.url == "https://dzen.ru" })
    }

    @Test
    fun mergeNewBuiltIns_doesNotRestoreDeletedLegacyTargets() {
        val customized = listOf(
            target("old-google", "Google", "https://www.google.com/generate_204", TargetGroup.FOREIGN),
            target("custom", "Custom", "https://example.com", TargetGroup.FOREIGN, builtIn = false),
        )

        val merged = DefaultCheckTargets.mergeNewBuiltIns(customized)

        assertEquals(customized, merged)
        assertFalse(merged.any { it.url == "https://dzen.ru" })
    }

    private fun target(
        id: String,
        name: String,
        url: String,
        group: TargetGroup,
        builtIn: Boolean = true,
    ): EditableCheckTarget {
        return EditableCheckTarget(
            id = id,
            name = name,
            url = url,
            group = group,
            enabled = true,
            builtIn = builtIn,
        )
    }
}
