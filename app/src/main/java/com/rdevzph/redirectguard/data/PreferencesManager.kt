package com.rdevzph.redirectguard.data

import android.content.Context
import android.content.SharedPreferences
import com.rdevzph.redirectguard.model.GuardedApp
import com.rdevzph.redirectguard.model.InterceptionMode
import com.rdevzph.redirectguard.model.InterceptionStrategy
import com.rdevzph.redirectguard.model.RedirectAction
import com.rdevzph.redirectguard.model.RedirectRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isGuardEnabled = MutableStateFlow(prefs.getBoolean(KEY_GUARD_ENABLED, true))
    val isGuardEnabled: StateFlow<Boolean> = _isGuardEnabled.asStateFlow()

    private val _interceptionMode = MutableStateFlow(
        getSavedInterceptionMode()
    )
    val interceptionMode: StateFlow<InterceptionMode> = _interceptionMode.asStateFlow()

    private val _interceptionStrategy = MutableStateFlow(
        getSavedInterceptionStrategy()
    )
    val interceptionStrategy: StateFlow<InterceptionStrategy> = _interceptionStrategy.asStateFlow()

    private val _guardAllExternalApps = MutableStateFlow(
        prefs.getBoolean(KEY_GUARD_ALL_EXTERNAL, false)
    )
    val guardAllExternalApps: StateFlow<Boolean> = _guardAllExternalApps.asStateFlow()

    private val _guardedPackages = MutableStateFlow(getSavedGuardedPackages())
    val guardedPackages: StateFlow<Set<String>> = _guardedPackages.asStateFlow()

    private val _whitelistedSourcePackages = MutableStateFlow(getSavedWhitelistedPackages())
    val whitelistedSourcePackages: StateFlow<Set<String>> = _whitelistedSourcePackages.asStateFlow()

    private val _whitelistSystemApps = MutableStateFlow(
        prefs.getBoolean(KEY_WHITELIST_SYSTEM_APPS, true)
    )
    val whitelistSystemApps: StateFlow<Boolean> = _whitelistSystemApps.asStateFlow()

    private val _showStatusNotification = MutableStateFlow(
        prefs.getBoolean(KEY_SHOW_STATUS_NOTIFICATION, true)
    )
    val showStatusNotification: StateFlow<Boolean> = _showStatusNotification.asStateFlow()

    private val _hasCompletedSetup = MutableStateFlow(
        prefs.getBoolean(KEY_HAS_COMPLETED_SETUP, false)
    )
    val hasCompletedSetup: StateFlow<Boolean> = _hasCompletedSetup.asStateFlow()

    private val _recentRedirects = MutableStateFlow(loadRecentRedirects())
    val recentRedirects: StateFlow<List<RedirectRecord>> = _recentRedirects.asStateFlow()

    private val _logFilterRedirects = MutableStateFlow(
        prefs.getBoolean(KEY_LOG_FILTER_REDIRECTS, true)
    )
    val logFilterRedirects: StateFlow<Boolean> = _logFilterRedirects.asStateFlow()

    private val _logFilterDebug = MutableStateFlow(
        prefs.getBoolean(KEY_LOG_FILTER_DEBUG, true)
    )
    val logFilterDebug: StateFlow<Boolean> = _logFilterDebug.asStateFlow()

    private val _logFilterErrors = MutableStateFlow(
        prefs.getBoolean(KEY_LOG_FILTER_ERRORS, true)
    )
    val logFilterErrors: StateFlow<Boolean> = _logFilterErrors.asStateFlow()

    private val _logFilterWarnings = MutableStateFlow(
        prefs.getBoolean(KEY_LOG_FILTER_WARNINGS, true)
    )
    val logFilterWarnings: StateFlow<Boolean> = _logFilterWarnings.asStateFlow()

    init {
        com.rdevzph.redirectguard.util.AppLogger.isDebugEnabled = _logFilterDebug.value
        com.rdevzph.redirectguard.util.AppLogger.isErrorsEnabled = _logFilterErrors.value
        com.rdevzph.redirectguard.util.AppLogger.isWarningsEnabled = _logFilterWarnings.value
        if (!_logFilterDebug.value) {
            com.rdevzph.redirectguard.util.AppLogger.purgeLevel(com.rdevzph.redirectguard.util.LogLevel.DEBUG)
        }
        if (!_logFilterErrors.value) {
            com.rdevzph.redirectguard.util.AppLogger.purgeLevel(com.rdevzph.redirectguard.util.LogLevel.ERROR)
        }
        if (!_logFilterWarnings.value) {
            com.rdevzph.redirectguard.util.AppLogger.purgeLevel(com.rdevzph.redirectguard.util.LogLevel.WARN)
        }
    }

    fun setLogFilterRedirects(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOG_FILTER_REDIRECTS, enabled).apply()
        _logFilterRedirects.value = enabled
    }

    fun setLogFilterDebug(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOG_FILTER_DEBUG, enabled).apply()
        _logFilterDebug.value = enabled
        com.rdevzph.redirectguard.util.AppLogger.isDebugEnabled = enabled
        if (!enabled) {
            com.rdevzph.redirectguard.util.AppLogger.purgeLevel(com.rdevzph.redirectguard.util.LogLevel.DEBUG)
        }
    }

    fun setLogFilterErrors(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOG_FILTER_ERRORS, enabled).apply()
        _logFilterErrors.value = enabled
        com.rdevzph.redirectguard.util.AppLogger.isErrorsEnabled = enabled
        if (!enabled) {
            com.rdevzph.redirectguard.util.AppLogger.purgeLevel(com.rdevzph.redirectguard.util.LogLevel.ERROR)
        }
    }

    fun setLogFilterWarnings(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOG_FILTER_WARNINGS, enabled).apply()
        _logFilterWarnings.value = enabled
        com.rdevzph.redirectguard.util.AppLogger.isWarningsEnabled = enabled
        if (!enabled) {
            com.rdevzph.redirectguard.util.AppLogger.purgeLevel(com.rdevzph.redirectguard.util.LogLevel.WARN)
        }
    }

    fun setSetupCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_COMPLETED_SETUP, completed).apply()
        _hasCompletedSetup.value = completed
    }

    fun setShowStatusNotification(show: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_STATUS_NOTIFICATION, show).apply()
        _showStatusNotification.value = show
        com.rdevzph.redirectguard.notification.NotificationHelper.updateStatusNotification(context)
    }

    fun setWhitelistSystemApps(whitelist: Boolean) {
        prefs.edit().putBoolean(KEY_WHITELIST_SYSTEM_APPS, whitelist).apply()
        _whitelistSystemApps.value = whitelist
    }

    fun setGuardEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_GUARD_ENABLED, enabled).apply()
        _isGuardEnabled.value = enabled
        com.rdevzph.redirectguard.notification.NotificationHelper.updateStatusNotification(context)
    }

    fun setInterceptionMode(mode: InterceptionMode) {
        prefs.edit().putString(KEY_INTERCEPTION_MODE, mode.name).apply()
        _interceptionMode.value = mode
    }

    fun setInterceptionStrategy(strategy: InterceptionStrategy) {
        prefs.edit().putString(KEY_INTERCEPTION_STRATEGY, strategy.name).apply()
        _interceptionStrategy.value = strategy
    }

    fun setGuardAllExternalApps(guardAll: Boolean) {
        prefs.edit().putBoolean(KEY_GUARD_ALL_EXTERNAL, guardAll).apply()
        _guardAllExternalApps.value = guardAll
        com.rdevzph.redirectguard.notification.NotificationHelper.updateStatusNotification(context)
    }

    fun setPackageGuarded(packageName: String, guarded: Boolean) {
        if (isPermanentWhitelisted(packageName)) return
        val updated = _guardedPackages.value.toMutableSet()
        if (guarded) {
            updated.add(packageName)
        } else {
            updated.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_GUARDED_PACKAGES, updated).apply()
        _guardedPackages.value = updated
        com.rdevzph.redirectguard.notification.NotificationHelper.updateStatusNotification(context)
    }

    fun isPackageWhitelisted(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return packageName == context.packageName ||
                packageName in PERMANENT_WHITELISTED_PACKAGES ||
                _whitelistedSourcePackages.value.contains(packageName)
    }

    fun isPermanentWhitelisted(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return packageName == context.packageName || packageName in PERMANENT_WHITELISTED_PACKAGES
    }

    fun addWhitelistedSource(packageName: String) {
        val updated = _whitelistedSourcePackages.value.toMutableSet()
        updated.add(packageName)
        prefs.edit().putStringSet(KEY_WHITELISTED_PACKAGES, updated).apply()
        _whitelistedSourcePackages.value = updated
    }

    fun removeWhitelistedSource(packageName: String) {
        if (isPermanentWhitelisted(packageName)) return
        val updated = _whitelistedSourcePackages.value.toMutableSet()
        updated.remove(packageName)
        prefs.edit().putStringSet(KEY_WHITELISTED_PACKAGES, updated).apply()
        _whitelistedSourcePackages.value = updated
    }

    fun clearWhitelistedSources() {
        prefs.edit().remove(KEY_WHITELISTED_PACKAGES).apply()
        _whitelistedSourcePackages.value = emptySet()
    }

    fun addRedirectRecord(record: RedirectRecord) {
        val currentList = _recentRedirects.value.toMutableList()
        currentList.add(0, record)
        if (currentList.size > MAX_HISTORY_ITEMS) {
            currentList.removeAt(currentList.lastIndex)
        }
        saveRecentRedirects(currentList)
        _recentRedirects.value = currentList
    }

    fun clearRedirectRecords() {
        saveRecentRedirects(emptyList())
        _recentRedirects.value = emptyList()
    }

    private fun getSavedInterceptionMode(): InterceptionMode {
        val modeName = prefs.getString(KEY_INTERCEPTION_MODE, InterceptionMode.CONFIRMATION_DIALOG.name)
        return try {
            InterceptionMode.valueOf(modeName ?: InterceptionMode.CONFIRMATION_DIALOG.name)
        } catch (e: Exception) {
            InterceptionMode.CONFIRMATION_DIALOG
        }
    }

    private fun getSavedInterceptionStrategy(): InterceptionStrategy {
        val strategyName = prefs.getString(KEY_INTERCEPTION_STRATEGY, InterceptionStrategy.AUTO_KILL_SHIZUKU.name)
        return try {
            InterceptionStrategy.valueOf(strategyName ?: InterceptionStrategy.AUTO_KILL_SHIZUKU.name)
        } catch (e: Exception) {
            InterceptionStrategy.AUTO_KILL_SHIZUKU
        }
    }

    private fun getSavedGuardedPackages(): Set<String> {
        val saved = prefs.getStringSet(KEY_GUARDED_PACKAGES, null)
        val defaultSet = GuardedApp.DEFAULT_SHOPPING_PRESETS
            .filter { it.packageName != PERMANENT_WHITELISTED_PACKAGE }
            .map { it.packageName }
            .toSet()
        if (saved != null) {
            // Automatically include built-in presets so new preset additions take effect
            return (saved + defaultSet).filter { it != PERMANENT_WHITELISTED_PACKAGE }.toSet()
        }
        prefs.edit().putStringSet(KEY_GUARDED_PACKAGES, defaultSet).apply()
        return defaultSet
    }

    private fun getSavedWhitelistedPackages(): Set<String> {
        return prefs.getStringSet(KEY_WHITELISTED_PACKAGES, emptySet()) ?: emptySet()
    }

    private fun saveRecentRedirects(records: List<RedirectRecord>) {
        val jsonArray = JSONArray()
        records.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("sourcePackage", item.sourcePackage)
                put("targetPackage", item.targetPackage)
                put("sourceAppName", item.sourceAppName)
                put("targetAppName", item.targetAppName)
                put("timestamp", item.timestamp)
                put("actionTaken", item.actionTaken.name)
                put("triggerReason", item.triggerReason)
                if (item.deepLinkUri != null) {
                    put("deepLinkUri", item.deepLinkUri)
                }
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_RECENT_REDIRECTS, jsonArray.toString()).apply()
    }

    private fun loadRecentRedirects(): List<RedirectRecord> {
        val raw = prefs.getString(KEY_RECENT_REDIRECTS, null) ?: return emptyList()
        val list = mutableListOf<RedirectRecord>()
        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val action = try {
                    RedirectAction.valueOf(obj.getString("actionTaken"))
                } catch (e: Exception) {
                    RedirectAction.PROMPTED
                }
                list.add(
                    RedirectRecord(
                        id = obj.getString("id"),
                        sourcePackage = obj.getString("sourcePackage"),
                        targetPackage = obj.getString("targetPackage"),
                        sourceAppName = obj.getString("sourceAppName"),
                        targetAppName = obj.getString("targetAppName"),
                        timestamp = obj.getLong("timestamp"),
                        actionTaken = action,
                        triggerReason = obj.getString("triggerReason"),
                        deepLinkUri = if (obj.has("deepLinkUri")) obj.getString("deepLinkUri") else null
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore parse error
        }
        return list
    }

    fun hasPromptedBatteryExemption(): Boolean {
        return prefs.getBoolean(KEY_HAS_PROMPTED_BATTERY, false)
    }

    fun setHasPromptedBatteryExemption(prompted: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_PROMPTED_BATTERY, prompted).apply()
    }

    companion object {
        const val PERMANENT_WHITELISTED_PACKAGE = "com.rdevzph.fpsmeter"
        val PERMANENT_WHITELISTED_PACKAGES = setOf(PERMANENT_WHITELISTED_PACKAGE)

        private const val PREFS_NAME = "redirect_guard_prefs"
        private const val KEY_GUARD_ENABLED = "guard_enabled"
        private const val KEY_INTERCEPTION_MODE = "interception_mode"
        private const val KEY_INTERCEPTION_STRATEGY = "interception_strategy"
        private const val KEY_GUARD_ALL_EXTERNAL = "guard_all_external"
        private const val KEY_WHITELIST_SYSTEM_APPS = "whitelist_system_apps"
        private const val KEY_GUARDED_PACKAGES = "guarded_packages"
        private const val KEY_WHITELISTED_PACKAGES = "whitelisted_packages"
        private const val KEY_SHOW_STATUS_NOTIFICATION = "show_status_notification"
        private const val KEY_RECENT_REDIRECTS = "recent_redirects"
        private const val KEY_HAS_PROMPTED_BATTERY = "has_prompted_battery"
        private const val KEY_HAS_COMPLETED_SETUP = "has_completed_setup"
        private const val KEY_LOG_FILTER_REDIRECTS = "log_filter_redirects"
        private const val KEY_LOG_FILTER_DEBUG = "log_filter_debug"
        private const val KEY_LOG_FILTER_ERRORS = "log_filter_errors"
        private const val KEY_LOG_FILTER_WARNINGS = "log_filter_warnings"
        private const val MAX_HISTORY_ITEMS = 50
    }
}
