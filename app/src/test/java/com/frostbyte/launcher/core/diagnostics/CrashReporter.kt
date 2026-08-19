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
    // previousHandler == null is a legitimate state (no handler was set
    // before us), so it can't double as the "already installed" flag -
    // that was the actual bug: on a JVM with no default handler, the guard
    // below never tripped and a second install() call wrapped itself again.
    private var isInstalled = false

    fun install() {
        if (isInstalled) return
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
        isInstalled = true
    }

    fun uninstall() {
        Thread.setDefaultUncaughtExceptionHandler(previousHandler)
        previousHandler = null
        isInstalled = false
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
        // Millisecond resolution (not just seconds) so two reports saved in
        // quick succession - e.g. a crash loop, or two saves in the same test -
        // never collide on filename and silently overwrite one another.
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.US).format(Date(report.timestampEpochMillis))
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
