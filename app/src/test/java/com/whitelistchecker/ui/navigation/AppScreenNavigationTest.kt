package com.whitelistchecker.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppScreenNavigationTest {

    @Test
    fun parentScreen_returnsExpectedNotificationHierarchy() {
        assertNull(AppScreen.HOME.parentScreen())
        assertEquals(AppScreen.HOME, AppScreen.NOTIFICATIONS.parentScreen())
        assertEquals(AppScreen.NOTIFICATIONS, AppScreen.LOCAL_NOTIFICATIONS.parentScreen())
        assertEquals(AppScreen.NOTIFICATIONS, AppScreen.TELEGRAM_NOTIFICATIONS.parentScreen())
        assertEquals(AppScreen.TELEGRAM_NOTIFICATIONS, AppScreen.TELEGRAM_WORKER_SETUP.parentScreen())
        assertEquals(AppScreen.TELEGRAM_NOTIFICATIONS, AppScreen.TELEGRAM_RECIPIENT_DISCOVERY.parentScreen())
        assertEquals(AppScreen.TELEGRAM_NOTIFICATIONS, AppScreen.TELEGRAM_QUEUE.parentScreen())
    }
}
