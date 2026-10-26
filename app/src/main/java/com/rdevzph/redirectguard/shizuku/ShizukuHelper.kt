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
     * Returns true if the command executed without error.
     */
    fun grantOverlayPermission(): Boolean {
        if (!_isPermissionGranted.value) return false
        return try {
            val pkg = context.packageName
            val (exitCode, _) = executeCommand(arrayOf("pm", "grant", pkg, "android.permission.SYSTEM_ALERT_WINDOW"))
            Log.d(TAG, "Grant overlay exit code: $exitCode")
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error granting overlay via Shizuku", e)
            false
        }
    }

    /**
     * Completely recovers the accessibility service by:
     * 1. Clearing our entry from enabled_accessibility_services.
     * 2. Force stopping the app via `am force-stop` to purge Android's cached dead binder.
     * 3. Waiting 1 second for system server cleanup.
     * 4. Re-enabling the service entry in secure settings.
     * 5. Relaunching the app.
     */
    fun forceStopAndRecoverAccessibility(componentName: String): Boolean {
        if (!_isPermissionGranted.value) return false
        return try {
            val pkg = context.packageName
            val (_, currentServices) = executeCommand(
                arrayOf("settings", "get", "secure", "enabled_accessibility_services")
            )
            val cleaned = currentServices.trim().split(":")
                .filter { it.isNotBlank() && !it.contains(pkg) }
                .joinToString(":")
            val disableVal = if (cleaned.isBlank() || cleaned == "null") "null" else cleaned
            val reEnabled = if (cleaned.isBlank() || cleaned == "null") componentName else "$cleaned:$componentName"

            val script = "settings put secure enabled_accessibility_services \"$disableVal\"\n" +
                    "am force-stop \"$pkg\"\n" +
                    "sleep 1\n" +
                    "settings put secure enabled_accessibility_services \"$reEnabled\"\n" +
                    "settings put secure accessibility_enabled 1\n" +
                    "sleep 1\n" +
                    "am start -n \"$pkg/$pkg.MainActivity\"\n"

            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            method.invoke(null, arrayOf("sh", "-c", script), null, null)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error force stopping and recovering via Shizuku", e)
            false
        }
    }

    /**
     * Enables the accessibility service via Shizuku cleanly without force stopping the app.
     */
    fun enableAccessibilityService(componentName: String): Boolean {
        if (!_isPermissionGranted.value) return false
        return try {
            val (_, currentServices) = executeCommand(
                arrayOf("settings", "get", "secure", "enabled_accessibility_services")
            )
            val trimmed = currentServices.trim()
            val list = trimmed.split(":").filter { it.isNotBlank() && it != "null" }.toMutableList()
            if (!list.contains(componentName)) {
                list.add(componentName)
            }
            val newServices = list.joinToString(":")
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
     * Force stops this application using Shizuku shell to clear bad states if needed.
     */
    fun forceStopApp(): Boolean {
        if (!_isPermissionGranted.value) return false
        return try {
            val (exitCode, _) = executeCommand(arrayOf("am", "force-stop", context.packageName))
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error force stopping app via Shizuku", e)
            false
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
