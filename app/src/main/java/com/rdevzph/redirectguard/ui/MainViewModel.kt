package com.rdevzph.redirectguard.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rdevzph.redirectguard.RedirectGuardApplication
import com.rdevzph.redirectguard.data.PreferencesManager
import com.rdevzph.redirectguard.model.GuardedApp
import com.rdevzph.redirectguard.model.InterceptionMode
import com.rdevzph.redirectguard.model.RedirectRecord
import com.rdevzph.redirectguard.service.RedirectGuardAccessibilityService
import com.rdevzph.redirectguard.util.AppLogEntry
import com.rdevzph.redirectguard.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MainScreenTab {
    DASHBOARD,
    APPS,
    LOGS
}

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

    private val _isIgnoringBatteryOptimizations = MutableStateFlow(checkIgnoringBatteryOptimizations())
    val isIgnoringBatteryOptimizations: StateFlow<Boolean> = _isIgnoringBatteryOptimizations.asStateFlow()

    val hasCompletedSetup: StateFlow<Boolean> = prefs.hasCompletedSetup

    private val _isAllRequirementsMet = MutableStateFlow(false)
    val isAllRequirementsMet: StateFlow<Boolean> = _isAllRequirementsMet.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    val appLogs: StateFlow<List<AppLogEntry>> = AppLogger.entries

    private val _selectedTab = MutableStateFlow(MainScreenTab.DASHBOARD)
    val selectedTab: StateFlow<MainScreenTab> = _selectedTab.asStateFlow()

    val showRedirectLogs: StateFlow<Boolean> = prefs.logFilterRedirects
    val showDebugLogs: StateFlow<Boolean> = prefs.logFilterDebug
    val showErrorLogs: StateFlow<Boolean> = prefs.logFilterErrors
    val showWarnLogs: StateFlow<Boolean> = prefs.logFilterWarnings

    private val _logSearchQuery = MutableStateFlow("")
    val logSearchQuery: StateFlow<String> = _logSearchQuery.asStateFlow()

    init {
        refreshStatus()
        loadInstalledApps()
    }

    fun refreshStatus() {
        shizuku.refreshStatus()
        _canDrawOverlays.value = checkCanDrawOverlays()
        _isIgnoringBatteryOptimizations.value = checkIgnoringBatteryOptimizations()
        _accessibilityHealth.value = RedirectGuardAccessibilityService.getServiceHealth(getApplication())
        _areNotificationsEnabled.value = androidx.core.app.NotificationManagerCompat.from(getApplication()).areNotificationsEnabled()
        com.rdevzph.redirectguard.notification.NotificationHelper.updateStatusNotification(getApplication())

        val shizukuReady = shizuku.isShizukuRunning.value && shizuku.isPermissionGranted.value
        val accessibilityReady = _accessibilityHealth.value != com.rdevzph.redirectguard.model.AccessibilityHealthState.DISABLED
        val overlayReady = _canDrawOverlays.value
        val notificationsReady = _areNotificationsEnabled.value
        val batteryReady = _isIgnoringBatteryOptimizations.value

        _isAllRequirementsMet.value = shizukuReady && accessibilityReady && overlayReady && notificationsReady && batteryReady
    }

    fun setSetupCompleted(completed: Boolean) {
        prefs.setSetupCompleted(completed)
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
        val userAppPackages = _installedApps.value.filter { !it.isSystemApp }.map { it.packageName }
        if (userAppPackages.isNotEmpty()) {
            if (guardAll) {
                prefs.addGuardedPackages(userAppPackages)
            } else {
                prefs.removeGuardedPackages(userAppPackages)
                val installedPackageNames = _installedApps.value.map { it.packageName }.toSet()
                prefs.applyInstalledShoppingPresets(installedPackageNames, ignoreDisabled = true)
            }
        }
    }

    fun toggleWhitelistSystemApps(whitelist: Boolean) {
        prefs.setWhitelistSystemApps(whitelist)
        val systemAppPackages = _installedApps.value.filter { it.isSystemApp }.map { it.packageName }
        if (systemAppPackages.isNotEmpty()) {
            if (whitelist) {
                prefs.addWhitelistedSources(systemAppPackages)
            } else {
                prefs.removeWhitelistedSources(systemAppPackages)
            }
        }
    }

    fun togglePackageGuarded(packageName: String, guarded: Boolean) {
        if (packageName == PreferencesManager.PERMANENT_WHITELISTED_PACKAGE) return
        prefs.setPackageGuarded(packageName, guarded)
        val app = _installedApps.value.find { it.packageName == packageName }
        if (app?.isSystemApp == false) {
            if (!guarded && prefs.guardAllExternalApps.value) {
                prefs.setGuardAllExternalApps(false)
            } else if (guarded) {
                checkAndUpdateGuardAllState()
            }
        }
        if (guarded && app?.isSystemApp == true && prefs.whitelistSystemApps.value) {
            prefs.setWhitelistSystemApps(false)
        }
    }

    private fun checkAndUpdateGuardAllState() {
        val userApps = _installedApps.value.filter { !it.isSystemApp }
        if (userApps.isNotEmpty()) {
            val allUserGuarded = userApps.all { prefs.guardedPackages.value.contains(it.packageName) }
            if (allUserGuarded && !prefs.guardAllExternalApps.value) {
                prefs.setGuardAllExternalApps(true)
            } else if (!allUserGuarded && prefs.guardAllExternalApps.value) {
                prefs.setGuardAllExternalApps(false)
            }
        }
    }

    fun togglePackageWhitelisted(packageName: String, whitelisted: Boolean) {
        if (packageName == PreferencesManager.PERMANENT_WHITELISTED_PACKAGE) return
        if (whitelisted) {
            prefs.addWhitelistedSource(packageName)
            checkAndUpdateSystemAppsWhitelistState()
            val app = _installedApps.value.find { it.packageName == packageName }
            if (app?.isSystemApp == false && prefs.guardAllExternalApps.value) {
                prefs.setGuardAllExternalApps(false)
            }
        } else {
            prefs.removeWhitelistedSource(packageName)
            val app = _installedApps.value.find { it.packageName == packageName }
            if (app?.isSystemApp == true && prefs.whitelistSystemApps.value) {
                prefs.setWhitelistSystemApps(false)
            }
        }
    }

    private fun checkAndUpdateSystemAppsWhitelistState() {
        val systemApps = _installedApps.value.filter { it.isSystemApp }
        if (systemApps.isNotEmpty()) {
            val allWhitelisted = systemApps.all { prefs.isPackageWhitelisted(it.packageName) }
            if (allWhitelisted && !prefs.whitelistSystemApps.value) {
                prefs.setWhitelistSystemApps(true)
            } else if (!allWhitelisted && prefs.whitelistSystemApps.value) {
                prefs.setWhitelistSystemApps(false)
            }
        }
    }

    fun isPackageWhitelisted(packageName: String): Boolean {
        return prefs.isPackageWhitelisted(packageName)
    }

    fun isPermanentWhitelisted(packageName: String): Boolean {
        return prefs.isPermanentWhitelisted(packageName)
    }

    fun removeWhitelistedSource(packageName: String) {
        prefs.removeWhitelistedSource(packageName)
    }

    fun removeWhitelistedSources(packageNames: Collection<String>) {
        prefs.removeWhitelistedSources(packageNames)
        checkAndUpdateSystemAppsWhitelistState()
    }

    fun clearWhitelistedSources() {
        prefs.clearWhitelistedSources()
    }

    fun selectTab(tab: MainScreenTab) {
        _selectedTab.value = tab
    }

    fun toggleShowRedirectLogs(show: Boolean) {
        prefs.setLogFilterRedirects(show)
    }

    fun toggleShowDebugLogs(show: Boolean) {
        prefs.setLogFilterDebug(show)
    }

    fun toggleShowErrorLogs(show: Boolean) {
        prefs.setLogFilterErrors(show)
    }

    fun toggleShowWarnLogs(show: Boolean) {
        prefs.setLogFilterWarnings(show)
    }

    fun setLogSearchQuery(query: String) {
        _logSearchQuery.value = query
    }

    fun clearHistory() {
        prefs.clearRedirectRecords()
    }

    fun clearAllLogs() {
        prefs.clearRedirectRecords()
        AppLogger.clear()
    }

    fun requestShizukuPermission() {
        shizuku.requestPermission()
    }

    fun openShizukuApp() {
        val context = getApplication<Application>()
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(com.rdevzph.redirectguard.shizuku.ShizukuHelper.SHIZUKU_PACKAGE)
        if (intent != null) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } else {
            try {
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${com.rdevzph.redirectguard.shizuku.ShizukuHelper.SHIZUKU_PACKAGE}")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(marketIntent)
            } catch (e: Exception) {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app")).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
            }
        }
    }

    fun grantOverlayViaShizuku(): Boolean {
        val success = shizuku.grantOverlayPermission()
        refreshStatus()
        return success
    }

    fun grantNotificationViaShizuku(): Boolean {
        val success = shizuku.grantNotificationPermission()
        refreshStatus()
        return success
    }

    fun enableAllRequirementsViaShizuku() {
        viewModelScope.launch(Dispatchers.IO) {
            val component = RedirectGuardAccessibilityService.getServiceComponentName(getApplication())
            shizuku.enableAccessibilityService(component)
            shizuku.grantOverlayPermission()
            shizuku.grantNotificationPermission()
            shizuku.whitelistBatteryOptimization()
            Thread.sleep(400)
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

    private fun checkIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(getApplication<Application>().packageName) ?: false
    }

    fun hasPromptedBatteryExemption(): Boolean = prefs.hasPromptedBatteryExemption()

    fun setHasPromptedBatteryExemption(prompted: Boolean) = prefs.setHasPromptedBatteryExemption(prompted)

    fun requestIgnoreBatteryOptimizations() {
        val context = getApplication<Application>()
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                openAppInfoSettings()
            }
        }
    }

    fun whitelistBatteryOptimizationViaShizuku(): Boolean {
        val success = shizuku.whitelistBatteryOptimization()
        refreshStatus()
        return success
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

    fun openIssuesUrl() {
        val context = getApplication<Application>()
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://github.com/rdevz-ph/RedirectGuard/issues")
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
                    if (appInfo.packageName == PreferencesManager.PERMANENT_WHITELISTED_PACKAGE ||
                        appInfo.packageName == getApplication<Application>().packageName
                    ) {
                        null
                    } else {
                        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val label = pm.getApplicationLabel(appInfo).toString()
                        InstalledAppItem(
                            packageName = appInfo.packageName,
                            appName = label,
                            isSystemApp = isSystem
                        )
                    }
                }.sortedBy { it.appName.lowercase() }
            }
            _installedApps.value = apps
            val installedPackageNames = apps.map { it.packageName }.toSet()
            val currentGuarded = prefs.guardedPackages.value
            val uninstalledGuarded = currentGuarded.filter { !installedPackageNames.contains(it) }
            if (uninstalledGuarded.isNotEmpty()) {
                prefs.removeGuardedPackages(uninstalledGuarded)
            }
            prefs.applyInstalledShoppingPresets(installedPackageNames)
            val systemPackages = apps.filter { it.isSystemApp }.map { it.packageName }
            if (systemPackages.isNotEmpty()) {
                if (prefs.whitelistSystemApps.value) {
                    val currentWhitelisted = prefs.whitelistedSourcePackages.value
                    val missing = systemPackages.filter { !currentWhitelisted.contains(it) && !prefs.isPermanentWhitelisted(it) }
                    if (missing.isNotEmpty()) {
                        prefs.addWhitelistedSources(systemPackages)
                    }
                } else {
                    checkAndUpdateSystemAppsWhitelistState()
                }
            }
            val userPackages = apps.filter { !it.isSystemApp }.map { it.packageName }
            if (userPackages.isNotEmpty()) {
                if (prefs.guardAllExternalApps.value) {
                    val currentGuarded = prefs.guardedPackages.value
                    val missing = userPackages.filter { !currentGuarded.contains(it) && !prefs.isPermanentWhitelisted(it) }
                    if (missing.isNotEmpty()) {
                        prefs.addGuardedPackages(userPackages)
                    }
                } else {
                    checkAndUpdateGuardAllState()
                }
            }
            _isLoadingApps.value = false
        }
    }
}
