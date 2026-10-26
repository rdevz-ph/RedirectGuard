package com.rdevzph.redirectguard.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.rdevzph.redirectguard.RedirectGuardApplication
import com.rdevzph.redirectguard.model.InterceptionMode
import com.rdevzph.redirectguard.model.InterceptionStrategy
import com.rdevzph.redirectguard.model.RedirectAction
import com.rdevzph.redirectguard.model.RedirectRecord
import com.rdevzph.redirectguard.ui.prompt.RedirectConfirmationActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class RedirectGuardAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastActiveSourcePackage: String? = null
    private var lastTransitionTime: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
        Log.d(TAG, "RedirectGuard Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return

        // Ignore our own app and system framework UI components
        if (packageName == applicationContext.packageName || isIgnoredSystemPackage(packageName)) {
            return
        }

        val prefs = RedirectGuardApplication.instance.preferencesManager

        // Whitelist system apps: system utilities like Smart Panel, Settings, System UI are never shopping apps
        if (prefs.whitelistSystemApps.value && isSystemApp(packageName)) {
            return
        }

        val now = SystemClock.uptimeMillis()

        // Detect if user launched directly from their home launcher or a system utility
        val homeLauncherPackage = getHomeLauncherPackage()
        val isFromSystemOrLauncher = lastActiveSourcePackage != null &&
                (lastActiveSourcePackage == homeLauncherPackage ||
                        (prefs.whitelistSystemApps.value && isSystemApp(lastActiveSourcePackage!!)))

        // Check if transition to a new external package
        if (lastActiveSourcePackage != null && lastActiveSourcePackage != packageName) {
            val isGuarded = prefs.guardAllExternalApps.value ||
                    prefs.guardedPackages.value.contains(packageName)

            val isWhitelisted = prefs.whitelistedSourcePackages.value.contains(lastActiveSourcePackage)
            val isExplicitlyAllowed = allowedTargetPackage == packageName &&
                    (now - allowedTargetTimestamp) < ALLOW_WINDOW_MS

            if (prefs.isGuardEnabled.value && isGuarded && !isWhitelisted && !isFromSystemOrLauncher && !isExplicitlyAllowed) {
                // Throttle repeated triggers within 1 second for the same target
                if (now - lastTransitionTime > MIN_TRIGGER_INTERVAL_MS) {
                    lastTransitionTime = now
                    handleInterceptedRedirect(
                        sourcePkg = lastActiveSourcePackage!!,
                        targetPkg = packageName,
                        prefs = prefs
                    )
                }
            }
        }

        // Update tracking state
        lastActiveSourcePackage = packageName
    }

    private fun handleInterceptedRedirect(
        sourcePkg: String,
        targetPkg: String,
        prefs: com.rdevzph.redirectguard.data.PreferencesManager
    ) {
        val sourceName = getAppNameFromPackage(sourcePkg)
        val targetName = getAppNameFromPackage(targetPkg)

        Log.w(TAG, "Redirect intercepted: $sourceName ($sourcePkg) -> $targetName ($targetPkg)")

        val mode = prefs.interceptionMode.value
        val strategy = prefs.interceptionStrategy.value

        // Execute configured interception mechanism
        executeInterceptionStrategy(targetPkg, strategy)

        if (mode == InterceptionMode.AUTO_RETURN) {
            val record = RedirectRecord(
                id = UUID.randomUUID().toString(),
                sourcePackage = sourcePkg,
                targetPackage = targetPkg,
                sourceAppName = sourceName,
                targetAppName = targetName,
                timestamp = System.currentTimeMillis(),
                actionTaken = RedirectAction.BLOCKED,
                triggerReason = if (strategy == InterceptionStrategy.AUTO_KILL_SHIZUKU) {
                    "Auto-Kill via Shizuku Blocked Redirect"
                } else {
                    "Auto-Return Back Blocked Redirect"
                }
            )
            prefs.addRedirectRecord(record)

            mainHandler.post {
                val actionMessage = if (strategy == InterceptionStrategy.AUTO_KILL_SHIZUKU) {
                    "Redirect Guard killed redirect to $targetName"
                } else {
                    "Redirect Guard blocked automatic redirect to $targetName"
                }
                Toast.makeText(
                    applicationContext,
                    actionMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            // Confirmation mode:
            val record = RedirectRecord(
                id = UUID.randomUUID().toString(),
                sourcePackage = sourcePkg,
                targetPackage = targetPkg,
                sourceAppName = sourceName,
                targetAppName = targetName,
                timestamp = System.currentTimeMillis(),
                actionTaken = RedirectAction.PROMPTED,
                triggerReason = "Confirmation Dialog Requested"
            )
            prefs.addRedirectRecord(record)

            // Launch prompt over source app after brief pause so intercept action completes
            mainHandler.postDelayed({
                val intent = Intent(this, RedirectConfirmationActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    putExtra(RedirectConfirmationActivity.EXTRA_SOURCE_PKG, sourcePkg)
                    putExtra(RedirectConfirmationActivity.EXTRA_TARGET_PKG, targetPkg)
                    putExtra(RedirectConfirmationActivity.EXTRA_SOURCE_NAME, sourceName)
                    putExtra(RedirectConfirmationActivity.EXTRA_TARGET_NAME, targetName)
                }
                startActivity(intent)
            }, 200L)
        }
    }

    private fun executeInterceptionStrategy(targetPkg: String, strategy: InterceptionStrategy) {
        when (strategy) {
            InterceptionStrategy.AUTO_KILL_SHIZUKU -> {
                val shizuku = RedirectGuardApplication.instance.shizukuHelper
                val killed = if (shizuku.isPermissionGranted.value) {
                    shizuku.forceStopPackage(targetPkg)
                } else {
                    false
                }
                if (!killed) {
                    Log.w(TAG, "Shizuku force-stop unavailable or failed for $targetPkg, falling back to Back action")
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
            }
            InterceptionStrategy.USE_BACK -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }
    }


    fun allowTargetTemporarily(packageName: String) {
        allowedTargetPackage = packageName
        allowedTargetTimestamp = SystemClock.uptimeMillis()
    }

    fun returnToPreviousApp() {
        performGlobalAction(GLOBAL_ACTION_BACK)
    }

    private fun getHomeLauncherPackage(): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun isSystemApp(packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } catch (e: Exception) {
            false
        }
    }

    private fun isIgnoredSystemPackage(pkg: String): Boolean {
        return pkg == "com.android.systemui" ||
                pkg == "com.google.android.inputmethod.latin" ||
                pkg == "com.samsung.android.honeyboard" ||
                pkg == "com.google.android.apps.nexuslauncher" ||
                pkg == "com.android.launcher3"
    }

    override fun onInterrupt() {
        Log.d(TAG, "RedirectGuard Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceActive.value = false
    }

    companion object {
        private const val TAG = "RedirectGuardService"
        private const val MIN_TRIGGER_INTERVAL_MS = 1000L
        private const val ALLOW_WINDOW_MS = 15000L

        private var allowedTargetPackage: String? = null
        private var allowedTargetTimestamp: Long = 0L

        private val _isServiceActive = MutableStateFlow(false)
        val isServiceActive: StateFlow<Boolean> = _isServiceActive.asStateFlow()

        var instance: RedirectGuardAccessibilityService? = null
            private set

        fun getServiceComponentName(context: Context): String {
            return "${context.packageName}/${RedirectGuardAccessibilityService::class.java.name}"
        }

        fun getServiceHealth(context: Context): com.rdevzph.redirectguard.model.AccessibilityHealthState {
            val isRunning = _isServiceActive.value && instance != null
            val isSettingOn = isAccessibilitySettingEnabled(context)
            return when {
                isRunning -> com.rdevzph.redirectguard.model.AccessibilityHealthState.HEALTHY
                isSettingOn -> com.rdevzph.redirectguard.model.AccessibilityHealthState.KILLED_DESYNCED
                else -> com.rdevzph.redirectguard.model.AccessibilityHealthState.DISABLED
            }
        }

        fun isAccessibilitySettingEnabled(context: Context): Boolean {
            val expectedService = getServiceComponentName(context)
            val shortExpectedService = "${context.packageName}/.service.RedirectGuardAccessibilityService"
            val enabledServices = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.split(":").any {
                it.equals(expectedService, ignoreCase = true) || it.equals(shortExpectedService, ignoreCase = true)
            }
        }

        fun isAccessibilityEnabled(context: Context): Boolean {
            return _isServiceActive.value && instance != null
        }
    }
}
