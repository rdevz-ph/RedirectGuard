package com.rdevzph.redirectguard.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

data class AppLogEntry(
    val id: Long = nextId(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null
) {
    companion object {
        private var idCounter: Long = 1L
        @Synchronized
        private fun nextId(): Long = idCounter++
    }
}

object AppLogger {
    private const val MAX_ENTRIES = 500
    private val lock = Any()

    @Volatile
    var isDebugEnabled: Boolean = true

    @Volatile
    var isWarningsEnabled: Boolean = true

    @Volatile
    var isErrorsEnabled: Boolean = true

    private fun isLevelEnabled(level: LogLevel): Boolean {
        return when (level) {
            LogLevel.DEBUG -> isDebugEnabled
            LogLevel.WARN -> isWarningsEnabled
            LogLevel.ERROR -> isErrorsEnabled
            LogLevel.INFO -> true
        }
    }

    private val _entries = MutableStateFlow<List<AppLogEntry>>(emptyList())
    val entries: StateFlow<List<AppLogEntry>> = _entries.asStateFlow()

    fun v(tag: String, message: String, throwable: Throwable? = null): Int {
        if (throwable != null) {
            Log.v(tag, message, throwable)
        } else {
            Log.v(tag, message)
        }
        append(LogLevel.DEBUG, tag, message, throwable)
        return 0
    }

    fun d(tag: String, message: String, throwable: Throwable? = null): Int {
        if (throwable != null) {
            Log.d(tag, message, throwable)
        } else {
            Log.d(tag, message)
        }
        append(LogLevel.DEBUG, tag, message, throwable)
        return 0
    }

    fun i(tag: String, message: String, throwable: Throwable? = null): Int {
        if (throwable != null) {
            Log.i(tag, message, throwable)
        } else {
            Log.i(tag, message)
        }
        append(LogLevel.INFO, tag, message, throwable)
        return 0
    }

    fun w(tag: String, message: String, throwable: Throwable? = null): Int {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
        append(LogLevel.WARN, tag, message, throwable)
        return 0
    }

    fun w(tag: String, throwable: Throwable?): Int {
        Log.w(tag, throwable)
        append(LogLevel.WARN, tag, throwable?.message ?: "", throwable)
        return 0
    }

    fun e(tag: String, message: String, throwable: Throwable? = null): Int {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
        append(LogLevel.ERROR, tag, message, throwable)
        return 0
    }

    private fun append(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (!isLevelEnabled(level)) return
        val entry = AppLogEntry(
            level = level,
            tag = tag,
            message = message,
            throwable = throwable
        )
        synchronized(lock) {
            val current = _entries.value
            val updated = if (current.size >= MAX_ENTRIES) {
                current.drop(current.size - MAX_ENTRIES + 1) + entry
            } else {
                current + entry
            }
            _entries.value = updated
        }
    }

    fun purgeLevel(level: LogLevel) {
        synchronized(lock) {
            _entries.value = _entries.value.filter { it.level != level }
        }
    }

    fun clear() {
        synchronized(lock) {
            _entries.value = emptyList()
        }
    }

    fun captureProcessLogcat(): List<AppLogEntry> {
        val list = mutableListOf<AppLogEntry>()
        try {
            val process = Runtime.getRuntime().exec(arrayOf("logcat", "-d", "-v", "time", "*:D"))
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) {
                    if (line.contains("RedirectGuard") || line.contains("Shizuku") || line.contains("com.rdevzph")) {
                        val level = when {
                            line.contains(" E/") -> LogLevel.ERROR
                            line.contains(" W/") -> LogLevel.WARN
                            line.contains(" I/") -> LogLevel.INFO
                            else -> LogLevel.DEBUG
                        }
                        list.add(
                            AppLogEntry(
                                level = level,
                                tag = "Logcat",
                                message = line
                            )
                        )
                    }
                    line = reader.readLine()
                }
            }
        } catch (_: Exception) {}
        return list
    }
}
