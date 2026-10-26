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
     * Attempts to grant SYSTEM_ALERT_WINDOW via Shizuku shell.
     * Returns true if the command executed without error.
     */
    fun grantOverlayPermission(): Boolean {
        if (!_isPermissionGranted.value) return false
        return try {
            val pkg = context.packageName
            val cmd = arrayOf("pm", "grant", pkg, "android.permission.SYSTEM_ALERT_WINDOW")
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java
            )
            method.isAccessible = true
            val process = method.invoke(null, cmd, null, null) as java.lang.Process
            val exitCode = process.waitFor()
            Log.d(TAG, "Grant overlay exit code: $exitCode")
            exitCode == 0
        } catch (e: Exception) {
            Log.e(TAG, "Error granting overlay via Shizuku", e)
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
