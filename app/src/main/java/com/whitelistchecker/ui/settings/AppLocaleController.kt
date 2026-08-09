package com.whitelistchecker.ui.settings

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.whitelistchecker.domain.model.AppLanguage
import java.util.Locale

object AppLocaleController {
    fun apply(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(language.languageTag),
        )
    }

    fun localizedContext(
        baseContext: Context,
        language: AppLanguage,
    ): Context {
        if (language == AppLanguage.SYSTEM) {
            Locale.setDefault(baseContext.resources.configuration.primaryLocale())
            return baseContext
        }
        val locale = Locale.forLanguageTag(language.languageTag)
        Locale.setDefault(locale)
        val configuration = Configuration(baseContext.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return baseContext.createConfigurationContext(configuration)
    }

    private fun Configuration.primaryLocale(): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locales[0]
        } else {
            @Suppress("DEPRECATION")
            locale
        }
    }
}
