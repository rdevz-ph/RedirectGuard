package com.rdevzph.redirectguard.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceActive.value = true
        Log.d(TAG, "RedirectGuard Accessibility Service connected")
        com.rdevzph.redirectguard.notification.NotificationHelper.updateStatusNotification(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val packageName = event.packageName?.toString() ?: return
        val className = event.className?.toString()
        Log.w(TAG, "onAccessibilityEvent: pkg=$packageName, class=$className, currentSource=$lastActiveSourcePackage")

        // 1. Ignore our own app UI (confirmation prompt and main screen)
        if (packageName == applicationContext.packageName) {
            return
        }

        // 2. Ignore keyboards, input methods, system UI overlays, and transient dialogs so they do not alter source tracking
        if (isInputMethodPackage(packageName) || isTransientSystemPackage(packageName)) {
            return
        }

        // 3. Handle Home Launcher: update source tracking so launches from home are never blocked
        if (isLauncherPackage(this, packageName)) {
            val activeWindowPkg = try { rootInActiveWindow?.packageName?.toString() } catch (e: Exception) { null }
            val nowTime = SystemClock.uptimeMillis()
            val isRecentTrailingEvent = (nowTime - lastAppActiveTime < 1500L) &&
                    lastAppActivePackage != null &&
                    !isLauncherPackage(this, lastAppActivePackage)

            if (activeWindowPkg != null && !isLauncherPackage(this, activeWindowPkg)) {
                Log.d(TAG, "Ignoring launcher event because active window is $activeWindowPkg: $packageName")
                return
            }

            if (isRecentTrailingEvent && (activeWindowPkg == null || !isLauncherPackage(this, activeWindowPkg))) {
                Log.d(TAG, "Ignoring trailing launcher event after returning to $lastAppActivePackage: $packageName")
                return
            }

            Log.d(TAG, "User active on Home Launcher: $packageName")
            lastActiveSourcePackage = packageName
            return
        }

        val prefs = RedirectGuardApplication.instance.preferencesManager

        // 4. Whitelist system utilities: Settings, App Info, etc. (OEM pre-installed guarded apps are never system utilities)
        if (isSystemUtility(packageName, prefs)) {
            Log.d(TAG, "User active in system utility: $packageName")
            lastActiveSourcePackage = packageName
            return
        }

        val now = SystemClock.uptimeMillis()

        // 5. Ignore trailing exit events from recently allowed target packages when returning to previous app
        if (recentlyAllowedPackage != null && packageName == recentlyAllowedPackage && now < recentlyAllowedUntil) {
            Log.d(TAG, "Ignoring trailing exit event from recently allowed target: $packageName")
            return
        }

        // 6. Handle user returning to the originating app from an allowed redirect
        if (originatingSourcePackage != null && packageName == originatingSourcePackage) {
            Log.d(TAG, "User returned to originating app: $packageName (allowed was $allowedTargetPackage)")
            recentlyAllowedPackage = allowedTargetPackage
            recentlyAllowedUntil = now + 3000L
            allowedTargetPackage = null
            allowedTargetUntil = 0L
            originatingSourcePackage = null
            lastActiveSourcePackage = packageName
            lastAppActivePackage = packageName
            lastAppActiveTime = now
            return
        }

        // 7. Explicitly allowed target is active
        if (allowedTargetPackage != null && packageName == allowedTargetPackage && now < allowedTargetUntil) {
            Log.d(TAG, "Allowed target package is active: $packageName")
            lastActiveSourcePackage = packageName
            lastAppActivePackage = packageName
            lastAppActiveTime = now
            return
        }

        // 8. Active suppression guard: if a recently intercepted target attempts to re-appear
        // (via ad retry, secondary trampoline activity, or background intent), kill it immediately.
        if (activeGuardedTargetPackage == packageName && now < activeGuardedTargetUntil) {
            val isExplicitlyAllowed = allowedTargetPackage == packageName && now < allowedTargetUntil
            if (!isExplicitlyAllowed && !prefs.isPackageWhitelisted(packageName)) {
                Log.w(TAG, "Active suppression: killing escape attempt by $packageName")
                RedirectGuardApplication.instance.shizukuHelper.forceStopPackage(packageName)
                if (prefs.interceptionMode.value == InterceptionMode.AUTO_RETURN && lastActiveSourcePackage != null) {
                    bringPackageToFront(lastActiveSourcePackage!!)
                }
                return
            }
        }

        // Resolve source package, falling back to activity stack inspection if null or if launcher was incorrectly recorded
        var effectiveSource = lastActiveSourcePackage
        val isGuarded = !prefs.isPackageWhitelisted(packageName) &&
                (prefs.guardAllExternalApps.value || prefs.guardedPackages.value.contains(packageName))

        // If effectiveSource is missing or is launcher, verify via Shizuku dumpsys whether an app was actually paused
        if (isGuarded && (effectiveSource == null || isLauncherPackage(this, effectiveSource))) {
            val verifiedSource = RedirectGuardApplication.instance.shizukuHelper.getPreviousForegroundPackage(packageName)
            if (verifiedSource != null) {
                Log.d(TAG, "Corrected effectiveSource from '$effectiveSource' to '$verifiedSource' via dumpsys")
                effectiveSource = verifiedSource
                lastActiveSourcePackage = verifiedSource
                lastAppActivePackage = verifiedSource
                lastAppActiveTime = now
            }
        }

        // Detect if user launched directly from their home launcher or a system utility
        val isFromLauncher = effectiveSource != null && isLauncherPackage(this, effectiveSource)
        val isFromSystemUI = effectiveSource != null && isSystemUIPackage(effectiveSource)
        val isFromSystemUtility = effectiveSource != null && isSystemUtility(effectiveSource, prefs)

        // Check if transition to a new external package
        if (effectiveSource != null && effectiveSource != packageName) {
            val isWhitelisted = prefs.isPackageWhitelisted(effectiveSource)
            val isExplicitlyAllowed = allowedTargetPackage == packageName && now < allowedTargetUntil

            // Sanity check: If active window is still the source app (e.g. background/closing event), do not intercept
            val activeWindowPkg = try { rootInActiveWindow?.packageName?.toString() } catch (e: Exception) { null }
            if (activeWindowPkg != null && activeWindowPkg == effectiveSource && activeWindowPkg != packageName) {
                Log.d(TAG, "Target $packageName is not active window (active is $activeWindowPkg), ignoring")
                return
            }

            // Only intercept if:
            // - Guard is enabled
            // - Target is guarded
            // - Source is not whitelisted
            // - Target is not explicitly allowed
            // - Not launched from Home Launcher or System UI
            // - Not launched from a system utility
            val shouldIntercept = prefs.isGuardEnabled.value &&
                    isGuarded &&
                    !isWhitelisted &&
                    !isExplicitlyAllowed &&
                    !isFromLauncher &&
                    !isFromSystemUI &&
                    !isFromSystemUtility

            Log.w(
                TAG,
                "Check: src=$effectiveSource -> target=$packageName | guarded=$isGuarded | whitelisted=$isWhitelisted | allowed=$isExplicitlyAllowed | fromLauncher=$isFromLauncher | fromSysUI=$isFromSystemUI | fromSysUtil=$isFromSystemUtility ==> shouldIntercept=$shouldIntercept"
            )

            if (shouldIntercept) {
                lastTransitionTime = now
                activeGuardedTargetPackage = packageName
                activeGuardedTargetUntil = now + ACTIVE_SUPPRESSION_WINDOW_MS
                handleInterceptedRedirect(
                    sourcePkg = effectiveSource,
                    targetPkg = packageName,
                    prefs = prefs
                )
                // Crucial: Do not update lastActiveSourcePackage to the guarded target package
                // so subsequent ad triggers correctly recognize effectiveSource as the active source app.
                return
            }
        }

        // Update tracking state
        lastActiveSourcePackage = packageName
        lastAppActivePackage = packageName
        lastAppActiveTime = now
    }

    private fun handleInterceptedRedirect(
        sourcePkg: String,
        targetPkg: String,
        prefs: com.rdevzph.redirectguard.data.PreferencesManager
    ) {
        if (prefs.isPackageWhitelisted(targetPkg)) {
            Log.d(TAG, "Target $targetPkg is whitelisted, skipping interception")
            return
        }

        val sourceName = getAppNameFromPackage(sourcePkg)
        val targetName = getAppNameFromPackage(targetPkg)

        Log.w(TAG, "Redirect intercepted: $sourceName ($sourcePkg) -> $targetName ($targetPkg)")

        val deepLinkUri = RedirectGuardApplication.instance.shizukuHelper.extractRecentDeepLinkUri(targetPkg)
        Log.d(TAG, "Captured deep link URI for $targetPkg: $deepLinkUri")

        // Immediately force stop target package via Shizuku shell to prevent it from escaping or continuing activities
        val killed = RedirectGuardApplication.instance.shizukuHelper.forceStopPackage(targetPkg)
        Log.d(TAG, "Force stop via Shizuku for $targetPkg: killed=$killed")

        val mode = prefs.interceptionMode.value

        if (mode == InterceptionMode.AUTO_RETURN) {
            bringPackageToFront(sourcePkg)
            val record = RedirectRecord(
                id = UUID.randomUUID().toString(),
                sourcePackage = sourcePkg,
                targetPackage = targetPkg,
                sourceAppName = sourceName,
                targetAppName = targetName,
                timestamp = System.currentTimeMillis(),
                actionTaken = RedirectAction.BLOCKED,
                triggerReason = "Auto-Kill via Shizuku Blocked Redirect",
                deepLinkUri = deepLinkUri
            )
            prefs.addRedirectRecord(record)

            mainHandler.post {
                val actionMessage = if (killed) {
                    "Redirect Guard killed redirect to $targetName"
                } else {
                    "Redirect to $targetName blocked (Shizuku required to terminate app)"
                }
                Toast.makeText(
                    applicationContext,
                    actionMessage,
                    Toast.LENGTH_SHORT
                ).show()
            }

            com.rdevzph.redirectguard.notification.NotificationHelper.postInterceptedNotification(
                context = applicationContext,
                sourceAppName = sourceName,
                targetAppName = targetName,
                action = RedirectAction.BLOCKED
            )
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
                triggerReason = "Confirmation Dialog Requested",
                deepLinkUri = deepLinkUri
            )
            prefs.addRedirectRecord(record)

            // Launch prompt over source app without bringPackageToFront racing against it
            mainHandler.post {
                val intent = Intent(this, RedirectConfirmationActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                    putExtra(RedirectConfirmationActivity.EXTRA_SOURCE_PKG, sourcePkg)
                    putExtra(RedirectConfirmationActivity.EXTRA_TARGET_PKG, targetPkg)
                    putExtra(RedirectConfirmationActivity.EXTRA_SOURCE_NAME, sourceName)
                    putExtra(RedirectConfirmationActivity.EXTRA_TARGET_NAME, targetName)
                    putExtra(RedirectConfirmationActivity.EXTRA_DEEP_LINK_URI, deepLinkUri)
                }
                startActivity(intent)
            }

            com.rdevzph.redirectguard.notification.NotificationHelper.postInterceptedNotification(
                context = applicationContext,
                sourceAppName = sourceName,
                targetAppName = targetName,
                action = RedirectAction.PROMPTED
            )
        }
    }

    fun bringPackageToFront(packageName: String) {
        if (packageName.isBlank()) return
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bring package $packageName to front", e)
        }
    }


    fun allowTargetTemporarily(targetPackage: String, sourcePackage: String? = null) {
        val now = SystemClock.uptimeMillis()
        allowedTargetPackage = targetPackage
        allowedTargetUntil = now + ALLOW_WINDOW_MS
        originatingSourcePackage = sourcePackage
        recentlyAllowedPackage = null
        recentlyAllowedUntil = 0L
        activeGuardedTargetPackage = null
        activeGuardedTargetUntil = 0L
    }

    private fun isSystemUtility(packageName: String?, prefs: com.rdevzph.redirectguard.data.PreferencesManager): Boolean {
        if (packageName.isNullOrBlank()) return false
        if (!prefs.whitelistSystemApps.value) return false
        // OEM pre-installed shopping or guarded apps are never system utilities
        if (prefs.guardedPackages.value.contains(packageName)) return false
        if (isBrowserApp(packageName)) return false
        return isSystemApp(packageName)
    }

    private fun isLauncherPackage(packageName: String?): Boolean {
        return isLauncherPackage(this, packageName)
    }

    private fun isSystemUIPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return packageName == "com.android.systemui" ||
                packageName == "com.google.android.systemui"
    }

    private fun isTransientSystemPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return packageName == "android" ||
                packageName == "com.android.systemui" ||
                packageName == "com.google.android.systemui" ||
                packageName == "com.android.packageinstaller" ||
                packageName == "com.google.android.packageinstaller" ||
                packageName == "com.android.permissioncontroller" ||
                packageName == "com.google.android.permissioncontroller" ||
                packageName == "com.transsnet.store" ||
                packageName.contains("phonehelper") ||
                packageName.contains("smartpanel") ||
                packageName == "com.google.android.gms"
    }

    private fun isInputMethodPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return packageName == "com.google.android.inputmethod.latin" ||
                packageName == "com.samsung.android.honeyboard" ||
                packageName == "com.touchtype.swiftkey" ||
                packageName == "com.baidu.input"
    }

    private fun isBrowserApp(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        val knownBrowsers = setOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.brave.browser",
            "com.microsoft.emmx",
            "com.sec.android.app.sbrowser",
            "com.transsion.phoenix",
            "com.UCMobile.intl",
            "com.duckduckgo.mobile.android"
        )
        if (knownBrowsers.contains(packageName)) return true

        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                setPackage(packageName)
            }
            val resolveInfos = packageManager.queryIntentActivities(intent, 0)
            resolveInfos.isNotEmpty()
        } catch (e: Exception) {
            false
        }
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

    override fun onInterrupt() {
        Log.d(TAG, "RedirectGuard Accessibility Service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceActive.value = false
        com.rdevzph.redirectguard.notification.NotificationHelper.cancelStatusNotification(applicationContext)
    }

    companion object {
        private const val TAG = "RedirectGuardService"
        private const val MIN_TRIGGER_INTERVAL_MS = 1000L
        private const val ALLOW_WINDOW_MS = 15000L
        private const val ACTIVE_SUPPRESSION_WINDOW_MS = 4000L

        private var lastActiveSourcePackage: String? = null
        private var lastTransitionTime: Long = 0L

        private var lastAppActivePackage: String? = null
        private var lastAppActiveTime: Long = 0L

        private var allowedTargetPackage: String? = null
        private var allowedTargetUntil: Long = 0L
        private var originatingSourcePackage: String? = null
        private var recentlyAllowedPackage: String? = null
        private var recentlyAllowedUntil: Long = 0L

        fun isLauncherPackage(context: Context, packageName: String?): Boolean {
            if (packageName.isNullOrBlank()) return false
            if (packageName == "com.google.android.apps.nexuslauncher" ||
                packageName == "com.android.launcher3" ||
                packageName == "com.transsion.hilauncher" ||
                packageName == "com.sec.android.app.launcher" ||
                packageName == "com.miui.home" ||
                packageName == "com.oppo.launcher" ||
                packageName == "com.coloros.launcher" ||
                packageName == "com.huawei.android.launcher" ||
                packageName == "com.oneplus.launcher" ||
                packageName == "com.teslacoilsw.launcher" ||
                packageName == "com.actionlauncher.playstore" ||
                packageName.contains("launcher") ||
                packageName.contains("home")
            ) {
                return true
            }

            return try {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.queryIntentActivities(
                        intent,
                        PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                }
                resolveInfos.any { it.activityInfo?.packageName == packageName }
            } catch (e: Exception) {
                false
            }
        }

        private var activeGuardedTargetPackage: String? = null
        private var activeGuardedTargetUntil: Long = 0L

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
