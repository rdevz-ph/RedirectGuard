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
    val guardAllExternalApps: StateFlow<Boolean> = prefs.guardAllExternalApps
    val guardedPackages: StateFlow<Set<String>> = prefs.guardedPackages
    val whitelistedPackages: StateFlow<Set<String>> = prefs.whitelistedSourcePackages
    val whitelistSystemApps: StateFlow<Boolean> = prefs.whitelistSystemApps
    val recentRedirects: StateFlow<List<RedirectRecord>> = prefs.recentRedirects

    val isAccessibilityActive: StateFlow<Boolean> =
        RedirectGuardAccessibilityService.isServiceActive

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
    }

    fun toggleGuard(enabled: Boolean) {
        prefs.setGuardEnabled(enabled)
    }

    fun setInterceptionMode(mode: InterceptionMode) {
        prefs.setInterceptionMode(mode)
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

    fun openAccessibilitySettings() {
        val context = getApplication<Application>()
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
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
