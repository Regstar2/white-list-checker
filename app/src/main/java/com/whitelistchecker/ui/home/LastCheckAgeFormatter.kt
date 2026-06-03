package com.whitelistchecker.ui.home

import android.content.res.Resources
import com.whitelistchecker.R
import java.util.concurrent.TimeUnit
import kotlin.math.max

object LastCheckAgeFormatter {

    fun formatAge(
        resources: Resources,
        checkedAtMillis: Long,
        nowMillis: Long,
    ): String {
        val ageMillis = max(0L, nowMillis - checkedAtMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ageMillis)
        return when {
            minutes < 1 -> resources.getString(R.string.last_check_age_just_now)
            minutes < 60 -> resources.getString(R.string.last_check_age_minutes, minutes)
            minutes < TimeUnit.DAYS.toMinutes(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(ageMillis)
                resources.getString(R.string.last_check_age_hours, hours)
            }
            else -> {
                val days = TimeUnit.MILLISECONDS.toDays(ageMillis)
                resources.getString(R.string.last_check_age_days, days)
            }
        }
    }
}
