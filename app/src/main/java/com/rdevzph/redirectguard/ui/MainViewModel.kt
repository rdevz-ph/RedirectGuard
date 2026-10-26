package com.rdevzph.redirectguard.ui

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rdevzph.redirectguard.RedirectGuardApplication
import com.rdevzph.redirectguard.model.GuardedApp
import com.rdevzph.redirectguard.model.InterceptionMode
import com.rdevzph.redirectguard.model.RedirectRecord
import com.rdevzph.redirectguard.service.RedirectGuardAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class InstalledAppItem(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = RedirectGuardApplication.instance.preferencesManager
    private val shizuku = RedirectGuardApplication.instance.shizukuHelper

    val isGuardEnabled: StateFlow<Boolean> = prefs.isGuardEnabled
    val interceptionMode: StateFlow<InterceptionMode> = prefs.interceptionMode
    val interceptionStrategy: StateFlow<com.rdevzph.redirectguard.model.InterceptionStrategy> = prefs.interceptionStrategy
    val guardAllExternalApps: StateFlow<Boolean> = prefs.guardAllExternalApps
    val guardedPackages: StateFlow<Set<String>> = prefs.guardedPackages
    val whitelistedPackages: StateFlow<Set<String>> = prefs.whitelistedSourcePackages
    val whitelistSystemApps: StateFlow<Boolean> = prefs.whitelistSystemApps
    val showStatusNotification: StateFlow<Boolean> = prefs.showStatusNotification
    val recentRedirects: StateFlow<List<RedirectRecord>> = prefs.recentRedirects

    val isAccessibilityActive: StateFlow<Boolean> =
        RedirectGuardAccessibilityService.isServiceActive

    private val _accessibilityHealth = MutableStateFlow(
        RedirectGuardAccessibilityService.getServiceHealth(application)
    )
    val accessibilityHealth: StateFlow<com.rdevzph.redirectguard.model.AccessibilityHealthState> =
        _accessibilityHealth.asStateFlow()

    private val _areNotificationsEnabled = MutableStateFlow(
        androidx.core.app.NotificationManagerCompat.from(application).areNotificationsEnabled()
    )
    val areNotificationsEnabled: StateFlow<Boolean> = _areNotificationsEnabled.asStateFlow()

    val isShizukuInstalled: StateFlow<Boolean> = shizuku.isShizukuInstalled
    val isShizukuRunning: StateFlow<Boolean> = shizuku.isShizukuRunning
    val isShizukuAuthorized: StateFlow<Boolean> = shizuku.isPermissionGranted

    private val _canDrawOverlays = MutableStateFlow(checkCanDrawOverlays())
    val canDrawOverlays: StateFlow<Boolean> = _canDrawOverlays.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    init {
        refreshStatus()
        loadInstalledApps()
    }

    fun refreshStatus() {
        shizuku.refreshStatus()
        _canDrawOverlays.value = checkCanDrawOverlays()
        _accessibilityHealth.value = RedirectGuardAccessibilityService.getServiceHealth(getApplication())
        _areNotificationsEnabled.value = androidx.core.app.NotificationManagerCompat.from(getApplication()).areNotificationsEnabled()
        com.rdevzph.redirectguard.notification.NotificationHelper.updateStatusNotification(getApplication())
    }

    fun toggleGuard(enabled: Boolean) {
        prefs.setGuardEnabled(enabled)
    }

    fun toggleStatusNotification(enabled: Boolean) {
        prefs.setShowStatusNotification(enabled)
    }

    fun setInterceptionMode(mode: InterceptionMode) {
        prefs.setInterceptionMode(mode)
    }

    fun setInterceptionStrategy(strategy: com.rdevzph.redirectguard.model.InterceptionStrategy) {
        prefs.setInterceptionStrategy(strategy)
    }

    fun toggleGuardAll(guardAll: Boolean) {
        prefs.setGuardAllExternalApps(guardAll)
    }

    fun toggleWhitelistSystemApps(whitelist: Boolean) {
        prefs.setWhitelistSystemApps(whitelist)
    }

    fun togglePackageGuarded(packageName: String, guarded: Boolean) {
        prefs.setPackageGuarded(packageName, guarded)
    }

    fun removeWhitelistedSource(packageName: String) {
        prefs.removeWhitelistedSource(packageName)
    }

    fun clearHistory() {
        prefs.clearRedirectRecords()
    }

    fun requestShizukuPermission() {
        shizuku.requestPermission()
    }

    fun grantOverlayViaShizuku(): Boolean {
        val success = shizuku.grantOverlayPermission()
        _canDrawOverlays.value = checkCanDrawOverlays()
        return success
    }

    fun recoverAccessibilityViaShizuku() {
        viewModelScope.launch(Dispatchers.IO) {
            val component = RedirectGuardAccessibilityService.getServiceComponentName(getApplication())
            shizuku.forceStopAndRecoverAccessibility(component)
            withContext(Dispatchers.Main) {
                refreshStatus()
            }
        }
    }

    fun enableAccessibilityViaShizuku() {
        viewModelScope.launch(Dispatchers.IO) {
            val component = RedirectGuardAccessibilityService.getServiceComponentName(getApplication())
            shizuku.enableAccessibilityService(component)
            Thread.sleep(350)
            withContext(Dispatchers.Main) {
                refreshStatus()
            }
        }
    }

    fun clearAccessibilityEntriesViaShizuku() {
        viewModelScope.launch(Dispatchers.IO) {
            shizuku.disableAccessibilityService()
            withContext(Dispatchers.Main) {
                refreshStatus()
            }
        }
    }

    fun forceStopAppViaShizuku() {
        viewModelScope.launch(Dispatchers.IO) {
            shizuku.forceStopApp()
        }
    }

    fun openAccessibilitySettings() {
        val context = getApplication<Application>()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openAppInfoSettings() {
        val context = getApplication<Application>()
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openOverlaySettings() {
        val context = getApplication<Application>()
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun openNotificationSettings() {
        val context = getApplication<Application>()
        val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        context.startActivity(intent)
    }

    val appVersion: String by lazy {
        try {
            val context = getApplication<Application>()
            val pm = context.packageManager
            val pInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
            val vName = pInfo.versionName ?: "1.0"
            val vCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
            "v$vName (Build $vCode)"
        } catch (e: Exception) {
            "v1.0"
        }
    }

    fun openRepositoryUrl() {
        val context = getApplication<Application>()
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://github.com/rdevz-ph/RedirectGuard")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Browser not available
        }
    }

    private fun checkCanDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(getApplication())
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                installed.mapNotNull { appInfo ->
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val label = pm.getApplicationLabel(appInfo).toString()
                    InstalledAppItem(
                        packageName = appInfo.packageName,
                        appName = label,
                        isSystemApp = isSystem
                    )
                }.sortedBy { it.appName.lowercase() }
            }
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }
}
