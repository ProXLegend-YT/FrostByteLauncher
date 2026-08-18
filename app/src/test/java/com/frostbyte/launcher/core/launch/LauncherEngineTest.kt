package com.frostbyte.launcher.core.launch

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import java.io.File

/**
 * These tests exercise LauncherEngine against REAL subprocesses (not mocks)
 * where the test JVM environment allows it, and real failure paths
 * (missing/non-executable binary, missing classpath entry) which are
 * exactly the failure modes expected on-device until JavaRuntimeCatalog has
 * a verified JRE build - see docs/KNOWN_GAPS.md.
 */
class LauncherEngineTest {

    private fun minimalConfig(javaBinary: File, classpathEntries: List<File> = emptyList()) = LaunchConfig(
        javaBinary = javaBinary,
        ramAllocationMb = 512,
        extraJvmArguments = emptyList(),
        mainClass = "DoesNotMatterForTheseTests",
        classpathEntries = classpathEntries,
        nativesDirectory = File("/tmp/natives"),
        gameDirectory = createTempGameDir(),
        assetsDirectory = File("/tmp/assets"),
        assetIndexId = "17",
        minecraftVersion = "1.21.1",
        username = "Steve",
        uuid = "00000000-0000-0000-0000-000000000000",
        accessToken = "test-token"
    )

    private fun createTempGameDir(): File =
        File.createTempFile("frostbyte-launch-test", "").apply { delete(); mkdirs() }

    @Test
    fun `reports Failed when java binary does not exist`() = runTest {
        val config = minimalConfig(javaBinary = File("/definitely/does/not/exist/java"))

        val stages = LauncherEngine().launch(config).toList()

        assertEquals(LaunchStage.Checking, stages.first())
        val failed = stages.filterIsInstance<LaunchStage.Failed>()
        assertTrue(failed.isNotEmpty())
        assertTrue(failed.first().reason.contains("not found"))
        assertTrue(stages.none { it is LaunchStage.Running })
    }

    @Test
    fun `reports Failed when java binary is not executable`() = runTest {
        val nonExecutable = File.createTempFile("fake-java", "").apply {
            writeText("not a real binary")
            setExecutable(false)
        }

        val config = minimalConfig(javaBinary = nonExecutable)
        val stages = LauncherEngine().launch(config).toList()

        val failed = stages.filterIsInstance<LaunchStage.Failed>()
        assertTrue(failed.isNotEmpty())
        assertTrue(stages.none { it is LaunchStage.Running })

        nonExecutable.delete()
    }

    @Test
    fun `reports Failed when a classpath entry is missing`() = runTest {
        // Use the test JVM's own java binary (via java.home) so the
        // missing-classpath check is what's actually under test, not
        // binary existence.
        val javaBinary = File(System.getProperty("java.home"), "bin/java")
        val missingJar = File("/tmp/frostbyte-test-missing-${System.nanoTime()}.jar")

        val config = minimalConfig(javaBinary = javaBinary, classpathEntries = listOf(missingJar))
        val stages = LauncherEngine().launch(config).toList()

        val failed = stages.filterIsInstance<LaunchStage.Failed>()
        assertTrue(failed.isNotEmpty())
        assertTrue(failed.first().reason.contains(missingJar.name))
        assertTrue(stages.none { it is LaunchStage.Running })
    }

    @Test
    fun `real process that starts reports Running then a real Exited exit code`() = runTest {
        // This test actually spawns a real process - the test JVM's own
        // java binary. The main class ("DoesNotMatterForTheseTests") isn't
        // real, so the JVM starts, fails to find the class, and exits
        // non-zero. That is still a genuine Running stage followed by a
        // genuine Exited stage with the JVM's real exit code - exactly what
        // this test verifies: LauncherEngine reports what actually
        // happened, never a scripted/pretended outcome.
        val javaBinary = File(System.getProperty("java.home"), "bin/java")
        Assume.assumeTrue("Test JVM's java binary must exist to run this test", javaBinary.exists())

        val config = minimalConfig(javaBinary = javaBinary)
        val stages = LauncherEngine().launch(config).toList()

        assertTrue(stages.any { it is LaunchStage.Running })
        val exited = stages.filterIsInstance<LaunchStage.Exited>()
        assertTrue(exited.isNotEmpty())
        assertTrue("A bogus main class should exit non-zero", exited.first().exitCode != 0)
    }
}
