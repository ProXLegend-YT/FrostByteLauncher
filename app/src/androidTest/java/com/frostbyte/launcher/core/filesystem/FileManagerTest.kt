package com.frostbyte.launcher.core.filesystem

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileManagerTest {

    private lateinit var fileManager: FileManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        fileManager = FileManager(context)
    }

    @Test
    fun categoryDirectories_areCreatedOnAccess() {
        val dirs = listOf(
            fileManager.versionsDir(),
            fileManager.librariesDir(),
            fileManager.assetsDir(),
            fileManager.profilesDir(),
            fileManager.modsCacheDir(),
            fileManager.shadersCacheDir(),
            fileManager.resourcePacksCacheDir(),
            fileManager.javaRuntimesDir(),
            fileManager.logsDir()
        )
        dirs.forEach { assertTrue("${it.path} should exist", it.exists()) }
    }

    @Test
    fun directorySizeBytes_sumsNestedFiles() {
        val dir = fileManager.modsCacheDir()
        File(dir, "a.jar").writeText("1234567890") // 10 bytes
        File(dir, "sub").mkdirs()
        File(dir, "sub/b.jar").writeText("12345") // 5 bytes

        assertEquals(15L, fileManager.directorySizeBytes(dir))
    }

    @Test
    fun safeDelete_refusesPathOutsideBaseDirectory() {
        val outsideFile = File.createTempFile("outside", ".tmp")
        outsideFile.writeText("should not be deletable")

        val deleted = fileManager.safeDelete(outsideFile)

        assertFalse(deleted)
        assertTrue(outsideFile.exists())
        outsideFile.delete() // manual cleanup since safeDelete correctly refused
    }

    @Test
    fun safeDelete_allowsPathInsideBaseDirectory() {
        val target = File(fileManager.modsCacheDir(), "deleteme.jar")
        target.writeText("bye")

        val deleted = fileManager.safeDelete(target)

        assertTrue(deleted)
        assertFalse(target.exists())
    }

    @Test
    fun clearCache_removesCacheDirsButRecreatesThem() {
        val marker = File(fileManager.modsCacheDir(), "marker.jar")
        marker.writeText("data")
        assertTrue(marker.exists())

        fileManager.clearCache()

        assertFalse(marker.exists())
        assertTrue(fileManager.modsCacheDir().exists()) // recreated, just empty
    }
}
