package com.whitelistchecker.data.targets

import com.whitelistchecker.domain.model.CheckTarget
import com.whitelistchecker.domain.model.TargetGroup

class DefaultTargetsRepository {

    fun getTargets(): List<CheckTarget> = listOf(
        CheckTarget(
            name = "Google",
            url = "https://www.google.com/generate_204",
            group = TargetGroup.FOREIGN,
        ),
        CheckTarget(
            name = "Cloudflare",
            url = "https://www.cloudflare.com",
            group = TargetGroup.FOREIGN,
        ),
        CheckTarget(
            name = "GitHub",
            url = "https://github.com",
            group = TargetGroup.FOREIGN,
        ),
        CheckTarget(
            name = "Wikipedia",
            url = "https://www.wikipedia.org",
            group = TargetGroup.FOREIGN,
        ),
        CheckTarget(
            name = "Yandex",
            url = "https://ya.ru",
            group = TargetGroup.LOCAL,
        ),
        CheckTarget(
            name = "VK",
            url = "https://vk.com",
            group = TargetGroup.LOCAL,
        ),
        CheckTarget(
            name = "Mail.ru",
            url = "https://mail.ru",
            group = TargetGroup.LOCAL,
        ),
        CheckTarget(
            name = "Gosuslugi",
            url = "https://www.gosuslugi.ru",
            group = TargetGroup.LOCAL,
        ),
    )
}
