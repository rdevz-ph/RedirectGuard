package com.rdevzph.redirectguard

import android.app.Application
import com.rdevzph.redirectguard.data.PreferencesManager
import com.rdevzph.redirectguard.shizuku.ShizukuHelper

class RedirectGuardApplication : Application() {

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var shizukuHelper: ShizukuHelper
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        com.rdevzph.redirectguard.crash.CrashHandler.install(this)
        preferencesManager = PreferencesManager(this)
        shizukuHelper = ShizukuHelper(this)
        com.rdevzph.redirectguard.notification.NotificationHelper.createNotificationChannels(this)
        com.rdevzph.redirectguard.notification.NotificationHelper.updateStatusNotification(this)
        com.rdevzph.redirectguard.util.AppLogger.d("RedirectGuardApp", "Application initialized successfully (v1.2)")
    }

    override fun onTerminate() {
        super.onTerminate()
        shizukuHelper.onDestroy()
    }

    companion object {
        lateinit var instance: RedirectGuardApplication
            private set
    }
}
