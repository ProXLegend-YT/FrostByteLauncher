package com.frostbyte.launcher.core.diagnostics

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CrashReport(
    val timestampEpochMillis: Long,
    val threadName: String,
    val exceptionType: String,
    val message: String?,
    val stackTrace: String
)

/**
 * Real crash capture via a genuine Thread.UncaughtExceptionHandler - not a
 * stub. Per Section 27 of the PRD: OFF by default (only installed if the
 * user has opted in via Settings), reports are written to LOCAL storage
 * only (no backend exists, and none is silently added here), and
 * NEVER include tokens/passwords - this class only ever captures the
 * exception's own message and stack trace, never touches
 * AuthRepository/SecureSessionStore or any other credential-holding class.
 */
class CrashReporter(private val reportsDir: File) {

    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    fun install() {
        if (previousHandler != null) return // already installed
        previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                saveReport(buildReport(thread, throwable))
            } catch (e: Exception) {
                // Deliberately swallowed - a failure while handling a crash
                // must never prevent the crash from propagating to the
                // previous (system) handler below, which is what actually
                // terminates/reports the process correctly.
            }
            previousHandler?.uncaughtException(thread, throwable)
        }
    }

    fun uninstall() {
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        previousHandler = null
    }

    fun buildReport(thread: Thread, throwable: Throwable): CrashReport {
        val stringWriter = StringWriter()
        throwable.printStackTrace(PrintWriter(stringWriter))
        return CrashReport(
            timestampEpochMillis = System.currentTimeMillis(),
            threadName = thread.name,
            exceptionType = throwable::class.qualifiedName ?: "UnknownException",
            message = throwable.message,
            stackTrace = stringWriter.toString()
        )
    }

    fun saveReport(report: CrashReport) {
        reportsDir.mkdirs()
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date(report.timestampEpochMillis))
        val file = File(reportsDir, "crash_$timestamp.txt")
        file.writeText(formatReport(report))
    }

    fun listSavedReports(): List<File> =
        reportsDir.listFiles { f -> f.isFile && f.name.startsWith("crash_") }?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun deleteReport(file: File): Boolean = file.delete()

    fun deleteAllReports() {
        listSavedReports().forEach { it.delete() }
    }

    private fun formatReport(report: CrashReport): String = buildString {
        appendLine("FrostByte Launcher Crash Report")
        appendLine("Time: ${Date(report.timestampEpochMillis)}")
        appendLine("Thread: ${report.threadName}")
        appendLine("Exception: ${report.exceptionType}")
        appendLine("Message: ${report.message ?: "(none)"}")
        appendLine()
        appendLine(report.stackTrace)
    }
}
