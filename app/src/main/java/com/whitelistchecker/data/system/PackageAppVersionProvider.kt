package com.whitelistchecker.data.system

import android.content.Context
import com.whitelistchecker.domain.system.AppVersionProvider

class PackageAppVersionProvider(
    context: Context,
) : AppVersionProvider {

    private val versionName: String = run {
        val packageManager = context.packageManager
        val packageName = context.packageName
        @Suppress("DEPRECATION")
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        packageInfo.versionName ?: "unknown"
    }

    override fun versionName(): String = versionName
}
