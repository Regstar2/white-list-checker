package com.whitelistchecker.data.targets

import com.whitelistchecker.domain.model.EditableCheckTarget
import com.whitelistchecker.domain.model.TargetGroup

object DefaultCheckTargets {

    fun defaults(): List<EditableCheckTarget> = foreignDefaults() + localDefaults()

    fun mergeNewBuiltIns(storedTargets: List<EditableCheckTarget>): List<EditableCheckTarget> {
        if (storedTargets.isEmpty()) return defaults()

        val storedUrls = storedTargets.map { it.url.normalizedUrl() }.toSet()
        val legacyUrls = legacyDefaultUrls()
        if (!legacyUrls.all { it in storedUrls }) {
            return storedTargets
        }

        val additions = defaults().filterNot { it.url.normalizedUrl() in storedUrls }
        return storedTargets + additions
    }

    private fun foreignDefaults(): List<EditableCheckTarget> = listOf(
        builtIn("builtin_foreign_google", "Google", "https://www.google.com/generate_204", TargetGroup.FOREIGN),
        builtIn("builtin_foreign_cloudflare", "Cloudflare", "https://www.cloudflare.com", TargetGroup.FOREIGN),
        builtIn("builtin_foreign_github", "GitHub", "https://github.com", TargetGroup.FOREIGN),
        builtIn("builtin_foreign_wikipedia", "Wikipedia", "https://www.wikipedia.org", TargetGroup.FOREIGN),
        builtIn("builtin_foreign_microsoft", "Microsoft", "https://www.microsoft.com", TargetGroup.FOREIGN),
        builtIn("builtin_foreign_apple", "Apple", "https://www.apple.com/library/test/success.html", TargetGroup.FOREIGN),
        builtIn("builtin_foreign_mozilla", "Mozilla", "https://www.mozilla.org", TargetGroup.FOREIGN),
        builtIn("builtin_foreign_debian", "Debian", "https://www.debian.org", TargetGroup.FOREIGN),
    )

    private fun localDefaults(): List<EditableCheckTarget> = listOf(
        builtIn("builtin_local_yandex", "Yandex", "https://ya.ru", TargetGroup.LOCAL),
        builtIn("builtin_local_vk", "VK", "https://vk.com", TargetGroup.LOCAL),
        builtIn("builtin_local_mailru", "Mail.ru", "https://mail.ru", TargetGroup.LOCAL),
        builtIn("builtin_local_gosuslugi", "Gosuslugi", "https://www.gosuslugi.ru", TargetGroup.LOCAL),
        builtIn("builtin_local_dzen", "Dzen", "https://dzen.ru", TargetGroup.LOCAL),
        builtIn("builtin_local_rambler", "Rambler", "https://www.rambler.ru", TargetGroup.LOCAL),
        builtIn("builtin_local_rbc", "RBC", "https://www.rbc.ru", TargetGroup.LOCAL),
        builtIn("builtin_local_habr", "Habr", "https://habr.com/ru/feed/", TargetGroup.LOCAL),
    )

    private fun legacyDefaultUrls(): Set<String> = setOf(
        "https://www.google.com/generate_204",
        "https://www.cloudflare.com",
        "https://github.com",
        "https://www.wikipedia.org",
        "https://ya.ru",
        "https://vk.com",
        "https://mail.ru",
        "https://www.gosuslugi.ru",
    ).map { it.normalizedUrl() }.toSet()

    private fun builtIn(
        id: String,
        name: String,
        url: String,
        group: TargetGroup,
    ): EditableCheckTarget {
        return EditableCheckTarget.create(
            id = id,
            name = name,
            url = url,
            group = group,
            builtIn = true,
        )
    }

    private fun String.normalizedUrl(): String {
        return trim().trimEnd('/').lowercase()
    }
}
