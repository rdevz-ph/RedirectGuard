package com.rdevzph.redirectguard.crash

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.rdevzph.redirectguard.util.AppLogger as Log
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class CrashHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)

            val crashReport = buildCrashReport(thread, throwable)

            val intent = Intent(context, CrashReportActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(CrashReportActivity.EXTRA_CRASH_REPORT, crashReport)
                putExtra(CrashReportActivity.EXTRA_EXCEPTION_CLASS, throwable.javaClass.simpleName)
                putExtra(CrashReportActivity.EXTRA_EXCEPTION_MESSAGE, throwable.message ?: "No error message")
            }
            context.startActivity(intent)

            // Terminate the crashing process
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        } catch (e: Exception) {
            Log.e(TAG, "Error in CrashHandler, delegating to default handler", e)
            defaultHandler?.uncaughtException(thread, throwable) ?: exitProcess(1)
        }
    }

    @Suppress("DEPRECATION")
    private fun buildCrashReport(thread: Thread, throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val currentDate = dateFormat.format(Date())

        val (versionName, versionCode) = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toString()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toString()
            }
            Pair(pInfo.versionName ?: "Unknown", code)
        } catch (e: PackageManager.NameNotFoundException) {
            Pair("Unknown", "Unknown")
        }

        return buildString {
            appendLine("=== REDIRECT GUARD CRASH REPORT ===")
            appendLine("Timestamp: $currentDate")
            appendLine("App Version: $versionName ($versionCode)")
            appendLine("Android OS: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
            appendLine("Crashing Thread: ${thread.name} (id: ${thread.id})")
            appendLine("Exception: ${throwable.javaClass.name}")
            appendLine("Message: ${throwable.message ?: "None"}")
            appendLine()
            appendLine("--- STACK TRACE ---")
            appendLine(stackTrace.trimEnd())
            appendLine("=== END CRASH REPORT ===")
        }
    }

    companion object {
        private const val TAG = "CrashHandler"

        fun install(context: Context) {
            val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (currentHandler !is CrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(
                    CrashHandler(context.applicationContext, currentHandler)
                )
                Log.d(TAG, "CrashHandler installed successfully")
            }
        }
    }
}
