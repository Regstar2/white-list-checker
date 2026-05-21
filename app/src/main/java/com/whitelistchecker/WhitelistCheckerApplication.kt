package com.whitelistchecker

import android.app.Application
import androidx.work.Configuration
import com.whitelistchecker.worker.WhitelistCheckWorkerFactory

class WhitelistCheckerApplication : Application(), Configuration.Provider {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        // WorkManager reads workManagerConfiguration during super.onCreate().
        appContainer = AppContainer(this)
        super.onCreate()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(WhitelistCheckWorkerFactory(appContainer))
            .build()
}
