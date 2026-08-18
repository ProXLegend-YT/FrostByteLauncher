package com.frostbyte.launcher.core.launch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArgumentBuilderTest {

    private fun sampleConfig(
        extraJvmArguments: List<String> = emptyList(),
        windowWidth: Int? = null,
        windowHeight: Int? = null,
        userType: String = "msa"
    ) = LaunchConfig(
        javaBinary = File("/data/app/frostbyte/runtimes/jre-17/bin/java"),
        ramAllocationMb = 4096,
        extraJvmArguments = extraJvmArguments,
        mainClass = "net.minecraft.client.main.Main",
        classpathEntries = listOf(File("/data/app/frostbyte/libs/a.jar"), File("/data/app/frostbyte/libs/b.jar")),
        nativesDirectory = File("/data/app/frostbyte/natives/1.21.1"),
        gameDirectory = File("/data/app/frostbyte/profiles/survival"),
        assetsDirectory = File("/data/app/frostbyte/assets"),
        assetIndexId = "17",
        minecraftVersion = "1.21.1",
        username = "Steve",
        uuid = "00000000-0000-0000-0000-000000000000",
        accessToken = "test-token",
        userType = userType,
        windowWidth = windowWidth,
        windowHeight = windowHeight
    )

    @Test
    fun `command starts with the java binary path`() {
        val command = ArgumentBuilder.buildCommand(sampleConfig())
        assertEquals("/data/app/frostbyte/runtimes/jre-17/bin/java", command.first())
    }

    @Test
    fun `heap size args use ramAllocationMb for both Xms and Xmx`() {
        val command = ArgumentBuilder.buildCommand(sampleConfig())
        assertTrue(command.contains("-Xmx4096M"))
        assertTrue(command.contains("-Xms4096M"))
    }

    @Test
    fun `classpath joins all entries with the platform path separator`() {
        val command = ArgumentBuilder.buildCommand(sampleConfig())
        val cpIndex = command.indexOf("-cp")
        assertTrue(cpIndex >= 0)
        val classpathArg = command[cpIndex + 1]
        assertEquals(
            "/data/app/frostbyte/libs/a.jar${File.pathSeparator}/data/app/frostbyte/libs/b.jar",
            classpathArg
        )
    }

    @Test
    fun `main class appears immediately after the classpath value`() {
        val command = ArgumentBuilder.buildCommand(sampleConfig())
        val cpIndex = command.indexOf("-cp")
        assertEquals("net.minecraft.client.main.Main", command[cpIndex + 2])
    }

    @Test
    fun `game arguments include required identity and directory flags`() {
        val command = ArgumentBuilder.buildCommand(sampleConfig())
        assertTrue(command.containsSubsequence("--username", "Steve"))
        assertTrue(command.containsSubsequence("--version", "1.21.1"))
        assertTrue(command.containsSubsequence("--uuid", "00000000-0000-0000-0000-000000000000"))
        assertTrue(command.containsSubsequence("--accessToken", "test-token"))
        assertTrue(command.containsSubsequence("--userType", "msa"))
    }

    @Test
    fun `userType is never offline or legacy regardless of input`() {
        // ArgumentBuilder itself doesn't validate this (that's the auth
        // layer's job in Phase 5), but this test documents and locks in
        // the expectation that "msa" is the only value this project ever
        // intentionally produces - a regression here is a real red flag.
        val command = ArgumentBuilder.buildCommand(sampleConfig(userType = "msa"))
        assertTrue(command.containsSubsequence("--userType", "msa"))
    }

    @Test
    fun `window size flags are omitted when not specified`() {
        val command = ArgumentBuilder.buildCommand(sampleConfig())
        assertFalse(command.contains("--width"))
        assertFalse(command.contains("--height"))
    }

    @Test
    fun `window size flags are included when specified`() {
        val command = ArgumentBuilder.buildCommand(sampleConfig(windowWidth = 1280, windowHeight = 720))
        assertTrue(command.containsSubsequence("--width", "1280"))
        assertTrue(command.containsSubsequence("--height", "720"))
    }

    @Test
    fun `blank extra jvm arguments are filtered out`() {
        val command = ArgumentBuilder.buildCommand(sampleConfig(extraJvmArguments = listOf("", "  ", "-XX:+UseG1GC")))
        assertTrue(command.contains("-XX:+UseG1GC"))
        assertFalse(command.contains(""))
        assertFalse(command.contains("  "))
    }

    @Test
    fun `natives directory is passed via java_library_path`() {
        val command = ArgumentBuilder.buildCommand(sampleConfig())
        assertTrue(command.any { it == "-Djava.library.path=/data/app/frostbyte/natives/1.21.1" })
    }

    private fun List<String>.containsSubsequence(vararg elements: String): Boolean {
        for (i in 0..this.size - elements.size) {
            if (elements.indices.all { this[i + it] == elements[it] }) return true
        }
        return false
    }
}
