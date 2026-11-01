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
        val versionName = try {
            val pInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            pInfo.versionName
        } catch (_: Exception) {
            null
        }

        val initMessage = if (!versionName.isNullOrBlank() && versionName != "Unknown") {
            "Application initialized successfully (v$versionName)"
        } else {
            "Application initialized successfully"
        }
        com.rdevzph.redirectguard.util.AppLogger.d("RedirectGuardApp", initMessage)
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
