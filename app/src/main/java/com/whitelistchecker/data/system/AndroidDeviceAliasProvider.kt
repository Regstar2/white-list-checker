package com.whitelistchecker.data.system

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.whitelistchecker.domain.model.PublicServiceSettings

class AndroidDeviceAliasProvider(
    private val context: Context,
) {

    fun getDefaultAlias(): String {
        val configuredDeviceName = runCatching {
            Settings.Global.getString(
                context.contentResolver,
                Settings.Global.DEVICE_NAME,
            )
        }.getOrNull()

        return DeviceAliasResolver.resolve(
            configuredDeviceName = configuredDeviceName,
            modelName = Build.MODEL,
            deviceName = Build.DEVICE,
        )
    }
}

internal object DeviceAliasResolver {

    fun resolve(
        configuredDeviceName: String?,
        modelName: String?,
        deviceName: String?,
    ): String {
        return sequenceOf(configuredDeviceName, modelName, deviceName)
            .mapNotNull { candidate -> candidate?.trim()?.takeIf(String::isNotBlank) }
            .firstOrNull()
            ?.take(MAX_ALIAS_LENGTH)
            ?: PublicServiceSettings.DEFAULT_DEVICE_ALIAS
    }

    private const val MAX_ALIAS_LENGTH = 64
}
