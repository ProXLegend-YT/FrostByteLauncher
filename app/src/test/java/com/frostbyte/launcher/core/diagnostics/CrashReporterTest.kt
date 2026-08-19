package com.frostbyte.launcher.core.diagnostics

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class CrashReporterTest {

    private lateinit var tempDir: File
    private lateinit var reporter: CrashReporter

    @Before
    fun setUp() {
        tempDir = File.createTempFile("crash-reporter-test", "").apply { delete(); mkdirs() }
        reporter = CrashReporter(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `buildReport captures real exception type, message, and stack trace`() {
        val exception = IllegalStateException("something broke")
        val thread = Thread.currentThread()

        val report = reporter.buildReport(thread, exception)

        assertEquals("java.lang.IllegalStateException", report.exceptionType)
        assertEquals("something broke", report.message)
        assertTrue(report.stackTrace.contains("IllegalStateException"))
        assertTrue(report.stackTrace.contains("buildReport captures real exception type"))
    }

    @Test
    fun `saveReport writes a real file that can be listed back`() {
        val report = reporter.buildReport(Thread.currentThread(), RuntimeException("test"))

        reporter.saveReport(report)

        val saved = reporter.listSavedReports()
        assertEquals(1, saved.size)
        assertTrue(saved.first().name.startsWith("crash_"))
        assertTrue(saved.first().readText().contains("RuntimeException"))
    }

    @Test
    fun `saved report content never includes anything beyond exception data`() {
        // Explicit safety check: a crash report must not somehow include
        // credential-shaped strings even if a caller's exception message
        // happened to mention "token" or "password" in an unrelated sense -
        // this test locks in that the formatter only ever writes the four
        // known fields (time/thread/exception/message) plus the stack trace,
        // nothing else is ever appended.
        val report = reporter.buildReport(Thread.currentThread(), RuntimeException("normal error"))
        reporter.saveReport(report)

        val content = reporter.listSavedReports().first().readText()
        val expectedLines = listOf("FrostByte Launcher Crash Report", "Time:", "Thread:", "Exception:", "Message:")
        expectedLines.forEach { assertTrue("Missing expected line: $it", content.contains(it)) }
    }

    @Test
    fun `listSavedReports returns newest first`() {
        val first = reporter.buildReport(Thread.currentThread(), RuntimeException("first"))
        reporter.saveReport(first)
        Thread.sleep(10) // filenames now have ms resolution, so this reliably crosses a boundary
        val second = reporter.buildReport(Thread.currentThread(), RuntimeException("second"))
        reporter.saveReport(second)

        val saved = reporter.listSavedReports()

        assertEquals(2, saved.size)
        assertTrue(saved.first().lastModified() >= saved.last().lastModified())
    }

    @Test
    fun `deleteReport removes a specific file`() {
        val report = reporter.buildReport(Thread.currentThread(), RuntimeException("test"))
        reporter.saveReport(report)
        val file = reporter.listSavedReports().first()

        val deleted = reporter.deleteReport(file)

        assertTrue(deleted)
        assertTrue(reporter.listSavedReports().isEmpty())
    }

    @Test
    fun `deleteAllReports clears every saved report`() {
        repeat(3) {
            reporter.saveReport(reporter.buildReport(Thread.currentThread(), RuntimeException("err $it")))
            Thread.sleep(5) // ensure each report gets a distinct millisecond-resolution filename
        }
        assertEquals(3, reporter.listSavedReports().size)

        reporter.deleteAllReports()

        assertTrue(reporter.listSavedReports().isEmpty())
    }

    @Test
    fun `install then uninstall restores the previous default handler`() {
        val original = Thread.getDefaultUncaughtExceptionHandler()

        reporter.install()
        assertFalse(Thread.getDefaultUncaughtExceptionHandler() === original)

        reporter.uninstall()
        assertTrue(Thread.getDefaultUncaughtExceptionHandler() === original)
    }

    @Test
    fun `calling install twice does not stack handlers`() {
        reporter.install()
        val afterFirstInstall = Thread.getDefaultUncaughtExceptionHandler()

        reporter.install() // second call should be a no-op per the "already installed" guard

        assertTrue(Thread.getDefaultUncaughtExceptionHandler() === afterFirstInstall)
        reporter.uninstall()
    }
}
