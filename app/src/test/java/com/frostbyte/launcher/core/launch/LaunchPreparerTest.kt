package com.frostbyte.launcher.core.launch

import com.frostbyte.launcher.core.network.model.AssetIndexRef
import com.frostbyte.launcher.core.network.model.DownloadArtifact
import com.frostbyte.launcher.core.network.model.JavaVersionRequirement
import com.frostbyte.launcher.core.network.model.LibraryArtifact
import com.frostbyte.launcher.core.network.model.LibraryDownloads
import com.frostbyte.launcher.core.network.model.LibraryEntry
import com.frostbyte.launcher.core.network.model.VersionDetailResponse
import com.frostbyte.launcher.core.network.model.VersionDownloads
import com.frostbyte.launcher.core.runtime.JavaRuntimeManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class LaunchPreparerTest {

    private lateinit var tempDir: File
    private lateinit var runtimesDir: File
    private lateinit var librariesDir: File
    private lateinit var runtimeManager: JavaRuntimeManager
    private lateinit var preparer: LaunchPreparer

    @Before
    fun setUp() {
        tempDir = File.createTempFile("launch-preparer-test", "").apply { delete(); mkdirs() }
        runtimesDir = File(tempDir, "runtimes").apply { mkdirs() }
        librariesDir = File(tempDir, "libraries").apply { mkdirs() }
        runtimeManager = JavaRuntimeManager(runtimesDir)
        preparer = LaunchPreparer(runtimeManager)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun sampleVersionDetail(libraries: List<LibraryEntry> = emptyList(), javaMajor: Int = 17) = VersionDetailResponse(
        id = "1.21.1",
        type = "release",
        mainClass = "net.minecraft.client.main.Main",
        downloads = VersionDownloads(
            client = DownloadArtifact(sha1 = "abc", size = 1L, url = "https://example.com/client.jar")
        ),
        javaVersion = JavaVersionRequirement(component = "java-runtime-delta", majorVersion = javaMajor),
        assetIndex = AssetIndexRef(id = "17", sha1 = "def", size = 1L, url = "https://example.com/17.json"),
        libraries = libraries
    )

    private fun identity() = LaunchIdentity(username = "Steve", uuid = "uuid", accessToken = "token")

    @Test
    fun `reports MissingJavaRuntime when the required JRE is not installed`() {
        val result = preparer.prepare(
            versionDetail = sampleVersionDetail(javaMajor = 21),
            librariesDir = librariesDir,
            nativesDir = File(tempDir, "natives"),
            assetsDir = File(tempDir, "assets"),
            gameDirectory = File(tempDir, "game"),
            ramAllocationMb = 2048,
            extraJvmArguments = emptyList(),
            identity = identity()
        )

        assertTrue(result is LaunchPreparationResult.MissingJavaRuntime)
        assertEquals(21, (result as LaunchPreparationResult.MissingJavaRuntime).requiredMajorVersion)
    }

    @Test
    fun `defaults to Java 17 when the version detail specifies no java version`() {
        val detail = sampleVersionDetail().copy(javaVersion = null)

        val result = preparer.prepare(
            versionDetail = detail,
            librariesDir = librariesDir,
            nativesDir = File(tempDir, "natives"),
            assetsDir = File(tempDir, "assets"),
            gameDirectory = File(tempDir, "game"),
            ramAllocationMb = 2048,
            extraJvmArguments = emptyList(),
            identity = identity()
        )

        assertTrue(result is LaunchPreparationResult.MissingJavaRuntime)
        assertEquals(17, (result as LaunchPreparationResult.MissingJavaRuntime).requiredMajorVersion)
    }

    @Test
    fun `reports MissingFiles when a required library jar has not been downloaded`() {
        installFakeJava(majorVersion = 17)
        val library = LibraryEntry(
            name = "org.lwjgl:lwjgl:3.3.3",
            downloads = LibraryDownloads(
                artifact = LibraryArtifact(path = "org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3.jar", sha1 = "x", size = 1L, url = "https://example.com/lwjgl.jar")
            )
        )

        val result = preparer.prepare(
            versionDetail = sampleVersionDetail(libraries = listOf(library)),
            librariesDir = librariesDir,
            nativesDir = File(tempDir, "natives"),
            assetsDir = File(tempDir, "assets"),
            gameDirectory = File(tempDir, "game"),
            ramAllocationMb = 2048,
            extraJvmArguments = emptyList(),
            identity = identity()
        )

        assertTrue(result is LaunchPreparationResult.MissingFiles)
        assertTrue((result as LaunchPreparationResult.MissingFiles).description.contains("lwjgl"))
    }

    @Test
    fun `returns Ready with a correct LaunchConfig once java and all libraries are present`() {
        installFakeJava(majorVersion = 17)
        val libraryPath = "org/lwjgl/lwjgl/3.3.3/lwjgl-3.3.3.jar"
        File(librariesDir, libraryPath).apply { parentFile.mkdirs(); writeText("fake jar bytes") }
        val library = LibraryEntry(
            name = "org.lwjgl:lwjgl:3.3.3",
            downloads = LibraryDownloads(
                artifact = LibraryArtifact(path = libraryPath, sha1 = "x", size = 1L, url = "https://example.com/lwjgl.jar")
            )
        )

        val result = preparer.prepare(
            versionDetail = sampleVersionDetail(libraries = listOf(library)),
            librariesDir = librariesDir,
            nativesDir = File(tempDir, "natives"),
            assetsDir = File(tempDir, "assets"),
            gameDirectory = File(tempDir, "game"),
            ramAllocationMb = 3072,
            extraJvmArguments = listOf("-XX:+UseG1GC"),
            identity = identity(),
            windowWidth = 1280,
            windowHeight = 720
        )

        assertTrue(result is LaunchPreparationResult.Ready)
        val config = (result as LaunchPreparationResult.Ready).config
        assertEquals("net.minecraft.client.main.Main", config.mainClass)
        assertEquals(3072, config.ramAllocationMb)
        assertEquals(1, config.classpathEntries.size)
        assertEquals("1.21.1", config.minecraftVersion)
        assertEquals("Steve", config.username)
        assertEquals("msa", config.userType)
        assertEquals(1280, config.windowWidth)
    }

    private fun installFakeJava(majorVersion: Int) {
        val binary = File(runtimeManager.runtimeDirFor(majorVersion), "bin/java")
        binary.parentFile.mkdirs()
        binary.writeText("#!/bin/sh\necho fake java")
        binary.setExecutable(true)
    }
}
