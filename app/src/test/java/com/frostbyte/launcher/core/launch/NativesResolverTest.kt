package com.frostbyte.launcher.core.launch

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class NativesResolverTest {

    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File.createTempFile("natives-test", "").apply { delete(); mkdirs() }
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun buildZip(entries: Map<String, ByteArray>): File {
        val zipFile = File(tempDir, "input-${System.nanoTime()}.jar")
        ZipOutputStream(zipFile.outputStream()).use { zos ->
            entries.forEach { (name, content) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(content)
                zos.closeEntry()
            }
        }
        return zipFile
    }

    @Test
    fun `extracts real so files and flattens their path`() {
        val jar = buildZip(
            mapOf(
                "linux/x64/liblwjgl.so" to "fake-native-bytes".toByteArray(),
                "META-INF/MANIFEST.MF" to "Manifest-Version: 1.0".toByteArray(),
                "readme.txt" to "not a native".toByteArray()
            )
        )
        val destination = File(tempDir, "out")

        val result = NativesResolver.extract(jar, destination)

        assertTrue(result is NativesExtractionResult.Success)
        val extracted = (result as NativesExtractionResult.Success).extractedFiles
        assertEquals(1, extracted.size)
        assertEquals("liblwjgl.so", extracted.first().name)
        assertEquals("fake-native-bytes", extracted.first().readText())
        // META-INF and non-.so files must never be extracted.
        assertFalse(File(destination, "MANIFEST.MF").exists())
        assertFalse(File(destination, "readme.txt").exists())
    }

    @Test
    fun `refuses to extract a zip-slip path traversal entry`() {
        // A malicious/corrupt jar entry attempting to escape the
        // destination directory via path traversal.
        val jar = buildZip(mapOf("../../../evil.so" to "malicious".toByteArray()))
        val destination = File(tempDir, "safe-out")

        val result = NativesResolver.extract(jar, destination)

        // The flatten-to-filename logic (File(entry.name).name) already
        // strips directory components, so this specific traversal can't
        // actually escape - but the guard must still be present and this
        // test locks in that the extraction either safely flattens it or
        // explicitly refuses, and never silently writes outside destination.
        val escapedFile = File(tempDir.parentFile, "evil.so")
        assertFalse(escapedFile.exists())
        when (result) {
            is NativesExtractionResult.Success ->
                result.extractedFiles.forEach {
                    assertTrue(it.canonicalFile.path.startsWith(destination.canonicalFile.path))
                }
            is NativesExtractionResult.Failure -> Unit // also an acceptable outcome
        }
    }

    @Test
    fun `reports Failure when the jar file does not exist`() {
        val result = NativesResolver.extract(File(tempDir, "does-not-exist.jar"), File(tempDir, "out"))
        assertTrue(result is NativesExtractionResult.Failure)
    }

    @Test
    fun `empty jar extracts successfully with zero files`() {
        val jar = buildZip(emptyMap())
        val result = NativesResolver.extract(jar, File(tempDir, "out"))
        assertTrue(result is NativesExtractionResult.Success)
        assertTrue((result as NativesExtractionResult.Success).extractedFiles.isEmpty())
    }

    @Test
    fun `multiple so files at different nested paths all get extracted`() {
        val jar = buildZip(
            mapOf(
                "a/liba.so" to "A".toByteArray(),
                "b/c/libb.so" to "B".toByteArray()
            )
        )
        val destination = File(tempDir, "out")

        val result = NativesResolver.extract(jar, destination) as NativesExtractionResult.Success

        assertEquals(2, result.extractedFiles.size)
        assertEquals(setOf("liba.so", "libb.so"), result.extractedFiles.map { it.name }.toSet())
    }
}
