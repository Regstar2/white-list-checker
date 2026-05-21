package com.whitelistchecker.data.targets

import com.whitelistchecker.domain.model.EditableCheckTarget
import com.whitelistchecker.domain.model.TargetGroup

object DefaultCheckTargets {

    fun defaults(): List<EditableCheckTarget> = listOf(
        EditableCheckTarget.create("Google", "https://www.google.com/generate_204", TargetGroup.FOREIGN, builtIn = true),
        EditableCheckTarget.create("Cloudflare", "https://www.cloudflare.com", TargetGroup.FOREIGN, builtIn = true),
        EditableCheckTarget.create("GitHub", "https://github.com", TargetGroup.FOREIGN, builtIn = true),
        EditableCheckTarget.create("Wikipedia", "https://www.wikipedia.org", TargetGroup.FOREIGN, builtIn = true),
        EditableCheckTarget.create("Yandex", "https://ya.ru", TargetGroup.LOCAL, builtIn = true),
        EditableCheckTarget.create("VK", "https://vk.com", TargetGroup.LOCAL, builtIn = true),
        EditableCheckTarget.create("Mail.ru", "https://mail.ru", TargetGroup.LOCAL, builtIn = true),
        EditableCheckTarget.create("Gosuslugi", "https://www.gosuslugi.ru", TargetGroup.LOCAL, builtIn = true),
    )
}
