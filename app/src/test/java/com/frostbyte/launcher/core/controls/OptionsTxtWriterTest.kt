package com.frostbyte.launcher.core.controls

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class OptionsTxtWriterTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File.createTempFile("options-writer-test", "").apply { delete(); mkdirs() }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `write produces real Minecraft options_txt line format`() {
        val file = File(tempDir, "options.txt")
        val bindings = mapOf(MinecraftAction.FORWARD to "key.keyboard.up")

        OptionsTxtWriter.write(file, bindings)

        val lines = file.readLines()
        assertTrue(lines.contains("key_key.forward:key.keyboard.up"))
    }

    @Test
    fun `write falls back to defaultKey for any action not in the bindings map`() {
        val file = File(tempDir, "options.txt")

        OptionsTxtWriter.write(file, emptyMap())

        val lines = file.readLines()
        assertTrue(lines.contains("key_key.jump:key.keyboard.space"))
    }

    @Test
    fun `write preserves unrelated pre-existing options_txt content`() {
        val file = File(tempDir, "options.txt")
        file.writeText(
            listOf(
                "version:3955",
                "fov:0.5",
                "key_key.forward:key.keyboard.w" // an existing binding this app WILL overwrite
            ).joinToString("\n")
        )

        OptionsTxtWriter.write(file, mapOf(MinecraftAction.FORWARD to "key.keyboard.up"))

        val lines = file.readLines()
        assertTrue("Unrelated version line must survive", lines.contains("version:3955"))
        assertTrue("Unrelated fov line must survive", lines.contains("fov:0.5"))
        assertTrue("The managed key line must be updated", lines.contains("key_key.forward:key.keyboard.up"))
        // Must not contain the OLD value for the key this app manages.
        assertTrue(lines.none { it == "key_key.forward:key.keyboard.w" })
    }

    @Test
    fun `write does not duplicate a managed key line on repeated writes`() {
        val file = File(tempDir, "options.txt")

        OptionsTxtWriter.write(file, mapOf(MinecraftAction.JUMP to "key.keyboard.space"))
        OptionsTxtWriter.write(file, mapOf(MinecraftAction.JUMP to "key.keyboard.up"))

        val jumpLines = file.readLines().filter { it.startsWith("key_key.jump:") }
        assertEquals(1, jumpLines.size)
        assertEquals("key_key.jump:key.keyboard.up", jumpLines.first())
    }

    @Test
    fun `readBindings parses only the managed key lines, ignoring everything else`() {
        val file = File(tempDir, "options.txt")
        file.writeText(
            listOf(
                "version:3955",
                "key_key.forward:key.keyboard.up",
                "key_key.jump:key.keyboard.space",
                "someUnrelatedSetting:true"
            ).joinToString("\n")
        )

        val bindings = OptionsTxtWriter.readBindings(file)

        assertEquals("key.keyboard.up", bindings[MinecraftAction.FORWARD])
        assertEquals("key.keyboard.space", bindings[MinecraftAction.JUMP])
        assertEquals(2, bindings.size) // only the two managed keys present in the file, nothing else
    }

    @Test
    fun `readBindings on a non-existent file returns an empty map, not an error`() {
        val file = File(tempDir, "does-not-exist.txt")
        assertEquals(emptyMap<MinecraftAction, String>(), OptionsTxtWriter.readBindings(file))
    }

    @Test
    fun `round trip write then read returns the same bindings`() {
        val file = File(tempDir, "options.txt")
        val bindings = mapOf(
            MinecraftAction.FORWARD to "key.keyboard.up",
            MinecraftAction.LEFT to "key.keyboard.left",
            MinecraftAction.SPRINT to "key.keyboard.left.alt"
        )

        OptionsTxtWriter.write(file, bindings)
        val readBack = OptionsTxtWriter.readBindings(file)

        assertEquals("key.keyboard.up", readBack[MinecraftAction.FORWARD])
        assertEquals("key.keyboard.left", readBack[MinecraftAction.LEFT])
        assertEquals("key.keyboard.left.alt", readBack[MinecraftAction.SPRINT])
    }
}
