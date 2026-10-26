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
        preferencesManager = PreferencesManager(this)
        shizukuHelper = ShizukuHelper(this)
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
