package com.rdevzph.redirectguard.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

class ShizukuHelper(private val context: Context) {

    private val _isShizukuInstalled = MutableStateFlow(false)
    val isShizukuInstalled: StateFlow<Boolean> = _isShizukuInstalled.asStateFlow()

    private val _isShizukuRunning = MutableStateFlow(false)
    val isShizukuRunning: StateFlow<Boolean> = _isShizukuRunning.asStateFlow()

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Log.d(TAG, "Shizuku binder received")
        refreshStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Log.d(TAG, "Shizuku binder died")
        _isShizukuRunning.value = false
        _isPermissionGranted.value = false
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_SHIZUKU) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                Log.d(TAG, "Shizuku permission response: granted=$granted")
                _isPermissionGranted.value = granted
            }
        }

    init {
        try {
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
            refreshStatus()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register Shizuku listeners", e)
        }
    }

    fun refreshStatus() {
        val installed = checkShizukuInstalled()
        _isShizukuInstalled.value = installed

        val running = try {
            Shizuku.pingBinder()
        } catch (e: Exception) {
            false
        }
        _isShizukuRunning.value = running

        if (running) {
            val granted = try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                false
            }
            _isPermissionGranted.value = granted
        } else {
            _isPermissionGranted.value = false
        }
    }

    fun requestPermission() {
        try {
            if (_isShizukuRunning.value) {
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting Shizuku permission", e)
        }
    }

    /**
     * Executes a shell command via Shizuku binder reflection.
     */
    fun executeCommand(cmd: Array<String>): Pair<Int, String> {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        method.isAccessible = true
        val process = method.invoke(null, cmd, null, null) as java.lang.Process
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        return Pair(exitCode, output)
    }

    /**
     * Attempts to grant SYSTEM_ALERT_WINDOW via Shizuku shell.
     * Uses appops set which is the standard shell method across Android versions.
     */
    fun grantOverlayPermission(): Boolean {
        if (!_isPermissionGranted.value) return false
        return try {
            val pkg = context.packageName
            val (exitCode, _) = executeCommand(arrayOf("appops", "set", pkg, "SYSTEM_ALERT_WINDOW", "allow"))
            try {
                executeCommand(arrayOf("pm", "grant", pkg, "android.permission.SYSTEM_ALERT_WINDOW"))
            } catch (ignored: Exception) {}
            Log.d(TAG, "Grant overlay exit code: $exitCode")
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error granting overlay via Shizuku", e)
            false
        }
    }

    /**
     * Attempts to grant POST_NOTIFICATIONS runtime permission via Shizuku shell.
     */
    fun grantNotificationPermission(): Boolean {
        if (!_isPermissionGranted.value) return false
        return try {
            val pkg = context.packageName
            val (exitCode, _) = executeCommand(arrayOf("pm", "grant", pkg, "android.permission.POST_NOTIFICATIONS"))
            try {
                executeCommand(arrayOf("appops", "set", pkg, "POST_NOTIFICATIONS", "allow"))
            } catch (ignored: Exception) {}
            Log.d(TAG, "Grant notification exit code: $exitCode")
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error granting notification via Shizuku", e)
            false
        }
    }

    /**
     * Attempts to whitelist Redirect Guard from battery optimization via Shizuku shell.
     * Returns true if command executed without error.
     */
    fun whitelistBatteryOptimization(): Boolean {
        if (!_isPermissionGranted.value) return false
        return try {
            val pkg = context.packageName
            val (exitCode, _) = executeCommand(arrayOf("cmd", "deviceidle", "whitelist", "+$pkg"))
            if (exitCode != 0) {
                executeCommand(arrayOf("dumpsys", "deviceidle", "whitelist", "+$pkg"))
            }
            Log.d(TAG, "Whitelist battery optimization exit code: $exitCode")
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error whitelisting battery optimization via Shizuku", e)
            false
        }
    }

    /**
     * Enables the accessibility service via Shizuku cleanly.
     */
    fun enableAccessibilityService(componentName: String): Boolean {
        if (!_isPermissionGranted.value) return false
        return try {
            val (_, currentServices) = executeCommand(
                arrayOf("settings", "get", "secure", "enabled_accessibility_services")
            )
            val trimmed = currentServices.trim()
            val list = trimmed.split(":").filter { it.isNotBlank() && it != "null" }
            val cleaned = list.filter { !it.contains(context.packageName) }
            if (list.any { it.contains(context.packageName) }) {
                executeCommand(
                    arrayOf("settings", "put", "secure", "enabled_accessibility_services", cleaned.joinToString(":").ifEmpty { "null" })
                )
            }
            val newServices = (cleaned + componentName).joinToString(":")
            val (exitCode, _) = executeCommand(
                arrayOf("settings", "put", "secure", "enabled_accessibility_services", newServices)
            )
            executeCommand(arrayOf("settings", "put", "secure", "accessibility_enabled", "1"))
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling accessibility service via Shizuku", e)
            false
        }
    }

    /**
     * Clears our accessibility service entry from secure settings.
     */
    fun disableAccessibilityService(): Boolean {
        if (!_isPermissionGranted.value) return false
        return try {
            val (_, currentServices) = executeCommand(
                arrayOf("settings", "get", "secure", "enabled_accessibility_services")
            )
            val cleaned = currentServices.trim().split(":")
                .filter { it.isNotBlank() && !it.contains(context.packageName) }
                .joinToString(":")
            val (exitCode, _) = executeCommand(
                arrayOf("settings", "put", "secure", "enabled_accessibility_services", cleaned.ifEmpty { "null" })
            )
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing accessibility service via Shizuku", e)
            false
        }
    }

    /**
     * Force stops the specified application package using Shizuku shell.
     */
    fun forceStopPackage(packageName: String): Boolean {
        if (!_isPermissionGranted.value) return false
        if (packageName == "com.rdevzph.fpsmeter") {
            Log.w(TAG, "Prevented force-stop of whitelisted app: $packageName")
            return false
        }
        return try {
            val (exitCode, _) = executeCommand(arrayOf("am", "force-stop", packageName))
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error force stopping $packageName via Shizuku", e)
            false
        }
    }

    /**
     * Force stops this application using Shizuku shell to clear stuck accessibility states.
     */
    fun forceStopApp(): Boolean {
        return forceStopPackage(context.packageName)
    }

    /**
     * Inspects recent tasks via dumpsys activity recents to extract the deep link URI (dat=...)
     * associated with the newly launched target package.
     */
    fun extractRecentDeepLinkUri(packageName: String): String? {
        if (!_isPermissionGranted.value) return null
        return try {
            val (exitCode, output) = executeCommand(arrayOf("dumpsys", "activity", "recents"))
            if (exitCode != 0 || output.isBlank()) return null
            parseDeepLinkUriFromRecents(output, packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting deep link URI for $packageName via Shizuku", e)
            null
        }
    }

    /**
     * Parses recent tasks dump to locate the intent for the given package and extract its data URI.
     */
    fun parseDeepLinkUriFromRecents(dumpsysOutput: String, packageName: String): String? {
        val intentLineRegex = Regex("""intent=\{[^\n]*\bdat=([^\s}]+)""")
        val generalDatRegex = Regex("""\bdat=([^\s}]+)""")

        // 1. Check Recent tasks section, dropping the initial header block before the first task
        val recentSection = dumpsysOutput.split("Visible recent tasks")[0]
        val tasks = recentSection.split(Regex("""\*\s+Recent\s+#\d+:""")).drop(1)

        for (task in tasks) {
            val firstLine = task.trimStart().lines().firstOrNull() ?: ""
            if (task.contains("pkg=$packageName") ||
                task.contains("cmp=$packageName/") ||
                task.contains(":$packageName") ||
                firstLine.contains(packageName)
            ) {
                val intentMatch = intentLineRegex.find(task) ?: generalDatRegex.find(task)
                if (intentMatch != null) {
                    val uri = intentMatch.groupValues[1]
                    if (uri.isNotBlank()) {
                        return uri
                    }
                }
                return null
            }
        }

        // 2. Fallback to Visible recent tasks section
        if (dumpsysOutput.contains("Visible recent tasks")) {
            val visibleSection = dumpsysOutput.split("Visible recent tasks")[1]
            val vTasks = visibleSection.split(Regex("""\*\s+RecentTaskInfo\s+#\d+:""")).drop(1)
            for (vTask in vTasks) {
                if (vTask.contains("pkg=$packageName") || vTask.contains("cmp=$packageName/")) {
                    val m = generalDatRegex.find(vTask)
                    if (m != null) {
                        val uri = m.groupValues[1]
                        if (uri.isNotBlank()) {
                            return uri
                        }
                    }
                    return null
                }
            }
        }

        return null
    }

    /**
     * Checks recent tasks to determine if the specified package was launched as a standard launcher
     * task without any deep link URI (e.g. from the home screen launcher, app drawer, or sidebar).
     */
    fun isNormalLauncherIntent(packageName: String): Boolean {
        if (!_isPermissionGranted.value) return false
        return try {
            val (exitCode, output) = executeCommand(arrayOf("dumpsys", "activity", "recents"))
            if (exitCode != 0 || output.isBlank()) return false

            val recentSection = output.split("Visible recent tasks")[0]
            val tasks = recentSection.split(Regex("""\*\s+Recent\s+#\d+:""")).drop(1)

            for (task in tasks) {
                val firstLine = task.trimStart().lines().firstOrNull() ?: ""
                if (task.contains("pkg=$packageName") ||
                    task.contains("cmp=$packageName/") ||
                    task.contains(":$packageName") ||
                    firstLine.contains(packageName)
                ) {
                    val hasLauncherCategory = task.contains("cat=[android.intent.category.LAUNCHER]")
                    val hasHomeCategory = task.contains("cat=[android.intent.category.HOME]")
                    val hasDataUri = task.contains("dat=")
                    return (hasLauncherCategory || hasHomeCategory) && !hasDataUri
                }
            }

            if (output.contains("Visible recent tasks")) {
                val visibleSection = output.split("Visible recent tasks")[1]
                val vTasks = visibleSection.split(Regex("""\*\s+RecentTaskInfo\s+#\d+:""")).drop(1)
                for (vTask in vTasks) {
                    if (vTask.contains("pkg=$packageName") || vTask.contains("cmp=$packageName/")) {
                        val hasLauncherCategory = vTask.contains("cat=[android.intent.category.LAUNCHER]")
                        val hasHomeCategory = vTask.contains("cat=[android.intent.category.HOME]")
                        val hasDataUri = vTask.contains("dat=")
                        return (hasLauncherCategory || hasHomeCategory) && !hasDataUri
                    }
                }
            }

            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Inspects the activity stack via Shizuku dumpsys to identify the previous foreground app.
     */
    fun getPreviousForegroundPackage(currentPackage: String): String? {
        if (!_isPermissionGranted.value) return null
        return try {
            val (exitCode, output) = executeCommand(arrayOf("dumpsys", "activity", "activities"))
            if (exitCode != 0 || output.isBlank()) return null
            val pattern = Regex("""mLastPausedActivity:\s*ActivityRecord\{[0-9a-fA-F]+\s+u\d+\s+([a-zA-Z0-9._]+)/""")
            val matches = pattern.findAll(output)
            for (match in matches) {
                val pkg = match.groupValues[1]
                if (pkg != currentPackage &&
                    pkg != context.packageName &&
                    !pkg.contains("systemui") &&
                    !com.rdevzph.redirectguard.service.RedirectGuardAccessibilityService.isLauncherPackage(context, pkg)
                ) {
                    Log.d(TAG, "Resolved previous foreground package via dumpsys: $pkg")
                    return pkg
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving previous foreground package", e)
            null
        }
    }

    private fun checkShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun onDestroy() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(requestPermissionResultListener)
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up Shizuku listeners", e)
        }
    }

    companion object {
        private const val TAG = "ShizukuHelper"
        private const val REQUEST_CODE_SHIZUKU = 2001
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }
}
