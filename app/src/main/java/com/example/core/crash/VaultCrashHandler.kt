package com.example.core.crash

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

class VaultCrashHandler(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTraceStr = sw.toString()

            // Sanitize sensitive tokens/IDs
            val sanitized = sanitizeLog(stackTraceStr)

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val logFile = File(context.cacheDir, "crash_$timestamp.log")
            FileOutputStream(logFile).use {
                it.write(sanitized.toByteArray())
            }

            // Save in SharedPreferences or local blackbox log ledger
            val prefs = context.getSharedPreferences("aethervault_crashes", Context.MODE_PRIVATE)
            val existing = prefs.getString("last_crash", "")
            prefs.edit().putString("last_crash", "[$timestamp] ${throwable.localizedMessage}\n$sanitized").apply()

        } catch (e: Exception) {
            // Fallback if writing fails
        }

        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun sanitizeLog(raw: String): String {
        // Redact potential bot tokens (e.g. 123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ)
        var cleaned = raw.replace(Regex("[0-9]{8,10}:[a-zA-Z0-9_-]{35}"), "[REDACTED_BOT_TOKEN]")
        // Redact channel IDs or phone numbers
        cleaned = cleaned.replace(Regex("-?[0-9]{10,14}"), "[REDACTED_ID]")
        return cleaned
    }

    companion object {
        fun init(context: Context) {
            val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (currentHandler !is VaultCrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(VaultCrashHandler(context, currentHandler))
            }
        }
    }
}
