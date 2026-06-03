package com.whitelistchecker.domain.system

fun interface AppVersionProvider {
    fun versionName(): String
}
