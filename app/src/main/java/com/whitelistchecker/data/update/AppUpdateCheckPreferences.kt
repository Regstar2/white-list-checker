package com.whitelistchecker.data.update

import android.content.Context

class AppUpdateCheckPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun tryAcquireAutomaticCheck(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val lastAttemptMillis = preferences.getLong(KEY_LAST_AUTOMATIC_ATTEMPT, 0L)
        if (lastAttemptMillis > 0L) {
            val elapsed = nowMillis - lastAttemptMillis
            if (elapsed in 0 until AUTOMATIC_CHECK_INTERVAL_MILLIS) {
                return false
            }
        }

        preferences.edit()
            .putLong(KEY_LAST_AUTOMATIC_ATTEMPT, nowMillis)
            .apply()
        return true
    }

    companion object {
        internal const val AUTOMATIC_CHECK_INTERVAL_MILLIS = 24L * 60L * 60L * 1000L
        private const val PREFERENCES_NAME = "app_update_check"
        private const val KEY_LAST_AUTOMATIC_ATTEMPT = "last_automatic_attempt_millis"
    }
}
