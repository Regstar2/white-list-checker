package com.whitelistchecker.data.dns

import com.whitelistchecker.domain.model.EditableDnsServer
import com.whitelistchecker.domain.model.TargetGroup

object DefaultDnsServers {

    fun defaults(): List<EditableDnsServer> = listOf(
        builtIn(
            id = "builtin_dns_foreign_cloudflare",
            name = "Cloudflare",
            address = "1.1.1.1",
            group = TargetGroup.FOREIGN,
        ),
        builtIn(
            id = "builtin_dns_foreign_google",
            name = "Google",
            address = "8.8.8.8",
            group = TargetGroup.FOREIGN,
        ),
        builtIn(
            id = "builtin_dns_local_yandex_primary",
            name = "Yandex DNS",
            address = "77.88.8.8",
            group = TargetGroup.LOCAL,
        ),
        builtIn(
            id = "builtin_dns_local_yandex_secondary",
            name = "Yandex DNS Secondary",
            address = "77.88.8.1",
            group = TargetGroup.LOCAL,
        ),
    )

    fun mergeNewBuiltIns(
        storedServers: List<EditableDnsServer>,
        removedBuiltInIds: Set<String> = emptySet(),
    ): List<EditableDnsServer> {
        val storedIds = storedServers.map { it.id }.toSet()
        val additions = defaults().filter { default ->
            default.id !in storedIds && default.id !in removedBuiltInIds
        }
        return storedServers + additions
    }

    private fun builtIn(
        id: String,
        name: String,
        address: String,
        group: TargetGroup,
    ): EditableDnsServer {
        return EditableDnsServer.create(
            id = id,
            name = name,
            address = address,
            group = group,
            builtIn = true,
        )
    }
}
