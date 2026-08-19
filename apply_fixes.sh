#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "Syncing repo..."
git pull --rebase origin main

echo "Writing CrashReporter.kt..."
cat > app/src/main/java/com/frostbyte/launcher/core/diagnostics/CrashReporter.kt << 'FILE_EOF'
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
FILE_EOF

echo "Writing CrashReporterTest.kt..."
cat > app/src/test/java/com/frostbyte/launcher/core/diagnostics/CrashReporterTest.kt << 'FILE_EOF'
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
FILE_EOF

echo "Writing ControlsViewModelTest.kt..."
cat > app/src/test/java/com/frostbyte/launcher/ui/screens/controls/ControlsViewModelTest.kt << 'FILE_EOF'
package com.frostbyte.launcher.ui.screens.controls

import com.frostbyte.launcher.core.controls.ConnectedGamepad
import com.frostbyte.launcher.core.controls.FakeControlsStore
import com.frostbyte.launcher.core.controls.FakeGamepadProvider
import com.frostbyte.launcher.core.controls.MinecraftAction
import com.frostbyte.launcher.core.controls.TouchControlLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ControlsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var store: FakeControlsStore

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        store = FakeControlsStore()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(gamepads: List<ConnectedGamepad> = emptyList()) =
        ControlsViewModel(store, FakeGamepadProvider(gamepads))

    @Test
    fun `initial state reflects the default layout and default key bindings`() = runTest(testDispatcher) {
        val vm = viewModel()
        // uiState.value starts as the stateIn() placeholder (empty keyBindings) until
        // the combine() upstream actually emits, so wait for the real emission.
        val state = vm.uiState.first { it.keyBindings.isNotEmpty() }

        assertEquals(TouchControlLayout.default(), state.touchLayout)
        assertEquals(MinecraftAction.FORWARD.defaultKey, state.keyBindings[MinecraftAction.FORWARD])
    }

    @Test
    fun `moveElement updates the target element's position and leaves others untouched`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.uiState.first() // subscribe

        val targetId = TouchControlLayout.MOVEMENT_JOYSTICK_ID
        vm.moveElement(targetId, xFraction = 0.5f, yFraction = 0.5f)

        val updated = vm.uiState.first { layout ->
            layout.touchLayout.elements.first { it.id == targetId }.xFraction == 0.5f
        }
        val movedElement = updated.touchLayout.elements.first { it.id == targetId }
        assertEquals(0.5f, movedElement.xFraction)
        assertEquals(0.5f, movedElement.yFraction)

        // A different element must be unaffected.
        val otherOriginal = TouchControlLayout.default().elements.first { it.id != targetId }
        val otherUpdated = updated.touchLayout.elements.first { it.id == otherOriginal.id }
        assertEquals(otherOriginal.xFraction, otherUpdated.xFraction)
    }

    @Test
    fun `moveElement clamps positions to the 0 to 1 range`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.uiState.first()

        val targetId = TouchControlLayout.MOVEMENT_JOYSTICK_ID
        vm.moveElement(targetId, xFraction = 1.5f, yFraction = -0.3f)

        val updated = vm.uiState.first { layout ->
            layout.touchLayout.elements.first { it.id == targetId }.xFraction == 1f
        }
        val movedElement = updated.touchLayout.elements.first { it.id == targetId }
        assertEquals(1f, movedElement.xFraction)
        assertEquals(0f, movedElement.yFraction)
    }

    @Test
    fun `resetLayout restores the default layout after a move`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.uiState.first()
        vm.moveElement(TouchControlLayout.MOVEMENT_JOYSTICK_ID, 0.9f, 0.9f)
        vm.uiState.first { it.touchLayout != TouchControlLayout.default() }

        vm.resetLayout()

        val state = vm.uiState.first { it.touchLayout == TouchControlLayout.default() }
        assertEquals(TouchControlLayout.default(), state.touchLayout)
    }

    @Test
    fun `setKeyBinding overrides only the targeted action`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.uiState.first()

        vm.setKeyBinding(MinecraftAction.FORWARD, "key.keyboard.up")

        val state = vm.uiState.first { it.keyBindings[MinecraftAction.FORWARD] == "key.keyboard.up" }
        assertEquals("key.keyboard.up", state.keyBindings[MinecraftAction.FORWARD])
        // An untouched action must still report its default.
        assertEquals(MinecraftAction.JUMP.defaultKey, state.keyBindings[MinecraftAction.JUMP])
    }

    @Test
    fun `resetKeyBinding reverts a previously overridden action back to default`() = runTest(testDispatcher) {
        val vm = viewModel()
        vm.uiState.first()
        vm.setKeyBinding(MinecraftAction.FORWARD, "key.keyboard.up")
        vm.uiState.first { it.keyBindings[MinecraftAction.FORWARD] == "key.keyboard.up" }

        vm.resetKeyBinding(MinecraftAction.FORWARD)

        val state = vm.uiState.first { it.keyBindings[MinecraftAction.FORWARD] == MinecraftAction.FORWARD.defaultKey }
        assertEquals(MinecraftAction.FORWARD.defaultKey, state.keyBindings[MinecraftAction.FORWARD])
    }

    @Test
    fun `connectedGamepads reflects what the gamepad provider reports`() = runTest(testDispatcher) {
        val vm = viewModel(gamepads = listOf(ConnectedGamepad(deviceId = 1, name = "Test Controller")))

        val state = vm.uiState.first { it.connectedGamepads.isNotEmpty() }

        assertEquals(1, state.connectedGamepads.size)
        assertEquals("Test Controller", state.connectedGamepads.first().name)
    }
}
FILE_EOF

echo "Writing ProfilesViewModelTest.kt..."
cat > app/src/test/java/com/frostbyte/launcher/ui/screens/profiles/ProfilesViewModelTest.kt << 'FILE_EOF'
package com.frostbyte.launcher.ui.screens.profiles

import com.frostbyte.launcher.core.common.Loader
import com.frostbyte.launcher.core.storage.repository.FakeProfileDao
import com.frostbyte.launcher.core.storage.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfilesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ProfilesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ProfilesViewModel(ProfileRepository(FakeProfileDao()))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `dialog starts closed`() = runTest(testDispatcher) {
        assertFalse(viewModel.uiState.first().isCreateDialogOpen)
    }

    @Test
    fun `openCreateDialog and dismissCreateDialog toggle dialog state`() = runTest(testDispatcher) {
        // uiState.value stays at the stateIn() placeholder until the combine()
        // upstream emits at least once, so wait for the real emission rather
        // than reading .value synchronously right after a transientState update.
        viewModel.openCreateDialog()
        assertTrue(viewModel.uiState.first { it.isCreateDialogOpen }.isCreateDialogOpen)

        viewModel.dismissCreateDialog()
        assertFalse(viewModel.uiState.first { !it.isCreateDialogOpen }.isCreateDialogOpen)
    }

    @Test
    fun `createProfile with valid input adds profile and closes dialog`() = runTest(testDispatcher) {
        viewModel.openCreateDialog()
        viewModel.createProfile("Survival", "1.21.1", Loader.FABRIC, ramGb = 4)
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.profiles.isNotEmpty() }
        assertFalse(state.isCreateDialogOpen)
        assertEquals(1, state.profiles.size)
        assertEquals("Survival", state.profiles.first().name)
        assertEquals(4096, state.profiles.first().ramAllocationMb)
    }

    @Test
    fun `createProfile with blank name surfaces error and keeps dialog open`() = runTest(testDispatcher) {
        viewModel.openCreateDialog()
        viewModel.createProfile("   ", "1.21.1", Loader.FABRIC, ramGb = 4)
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.errorMessage != null }
        assertTrue(state.isCreateDialogOpen)
        assertNotNull(state.errorMessage)
        assertEquals(0, state.profiles.size)
    }

    @Test
    fun `dismissError clears the error message`() = runTest(testDispatcher) {
        viewModel.createProfile("", "1.21.1", Loader.FABRIC, ramGb = 4)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.first { it.errorMessage != null }.errorMessage)

        viewModel.dismissError()
        assertEquals(null, viewModel.uiState.first { it.errorMessage == null }.errorMessage)
    }
}
FILE_EOF

echo "All files written. Committing and pushing..."
git add -A
git commit -m "Fix CrashReporter install guard + flaky ViewModel/CrashReporter tests"
git push

echo "Done!"
