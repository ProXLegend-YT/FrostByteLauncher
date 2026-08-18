package com.frostbyte.launcher.core.launch

import com.frostbyte.launcher.core.network.model.LibraryArtifact
import com.frostbyte.launcher.core.network.model.LibraryDownloads
import com.frostbyte.launcher.core.network.model.LibraryEntry
import com.frostbyte.launcher.core.network.model.LibraryOsConstraint
import com.frostbyte.launcher.core.network.model.LibraryRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LibraryResolverTest {

    private fun libraryWithArtifact(name: String, path: String = "$name.jar", rules: List<LibraryRule>? = null) = LibraryEntry(
        name = name,
        downloads = LibraryDownloads(
            artifact = LibraryArtifact(path = path, sha1 = "sha1-$name", size = 100L, url = "https://example.com/$path")
        ),
        rules = rules
    )

    @Test
    fun `library with no rules is always included`() {
        val resolved = LibraryResolver.resolveClasspathLibraries(listOf(libraryWithArtifact("commons-io")))
        assertEquals(1, resolved.size)
        assertEquals("commons-io", resolved.first().mavenName)
    }

    @Test
    fun `library with an unconditional allow rule is included`() {
        val entry = libraryWithArtifact(
            "lwjgl",
            rules = listOf(LibraryRule(action = "allow", os = null))
        )
        val resolved = LibraryResolver.resolveClasspathLibraries(listOf(entry))
        assertEquals(1, resolved.size)
    }

    @Test
    fun `library allowed only on windows is excluded on linux target`() {
        val entry = libraryWithArtifact(
            "windows-only-lib",
            rules = listOf(LibraryRule(action = "allow", os = LibraryOsConstraint(name = "windows")))
        )
        val resolved = LibraryResolver.resolveClasspathLibraries(listOf(entry))
        assertTrue(resolved.isEmpty())
    }

    @Test
    fun `library allowed only on linux is included`() {
        val entry = libraryWithArtifact(
            "linux-only-lib",
            rules = listOf(LibraryRule(action = "allow", os = LibraryOsConstraint(name = "linux")))
        )
        val resolved = LibraryResolver.resolveClasspathLibraries(listOf(entry))
        assertEquals(1, resolved.size)
    }

    @Test
    fun `global allow followed by an OS-specific disallow excludes on that OS`() {
        // Mirrors a real-world pattern: "allow everywhere EXCEPT osx".
        val entry = libraryWithArtifact(
            "cross-platform-lib",
            rules = listOf(
                LibraryRule(action = "allow", os = null),
                LibraryRule(action = "disallow", os = LibraryOsConstraint(name = "osx"))
            )
        )
        val resolved = LibraryResolver.resolveClasspathLibraries(listOf(entry))
        // Target OS is linux, so the osx-disallow rule does NOT match here -
        // the global allow should still stand and the library is included.
        assertEquals(1, resolved.size)
    }

    @Test
    fun `global allow followed by a linux-specific disallow excludes on linux`() {
        val entry = libraryWithArtifact(
            "linux-excluded-lib",
            rules = listOf(
                LibraryRule(action = "allow", os = null),
                LibraryRule(action = "disallow", os = LibraryOsConstraint(name = "linux"))
            )
        )
        val resolved = LibraryResolver.resolveClasspathLibraries(listOf(entry))
        // The later, more specific linux-disallow rule must win over the
        // earlier global allow - this is the "last matching rule wins"
        // semantics the resolver is specifically designed to implement.
        assertTrue(resolved.isEmpty())
    }

    @Test
    fun `library with rules but no artifact download is skipped entirely`() {
        val entry = LibraryEntry(
            name = "native-classifier-only-lib",
            downloads = LibraryDownloads(artifact = null), // some entries only carry natives-classifiers, no plain artifact
            rules = null
        )
        val resolved = LibraryResolver.resolveClasspathLibraries(listOf(entry))
        assertTrue(resolved.isEmpty())
    }

    @Test
    fun `resolveClasspathFiles builds absolute paths under the libraries directory`() {
        val librariesDir = File("/data/frostbyte/libraries")
        val resolved = LibraryResolver.resolveClasspathLibraries(
            listOf(libraryWithArtifact("commons-io", path = "commons-io/commons-io/2.11.0/commons-io-2.11.0.jar"))
        )

        val files = LibraryResolver.resolveClasspathFiles(librariesDir, resolved)

        assertEquals(1, files.size)
        assertEquals(
            File("/data/frostbyte/libraries/commons-io/commons-io/2.11.0/commons-io-2.11.0.jar"),
            files.first()
        )
    }

    @Test
    fun `multiple libraries preserve their relative order`() {
        val entries = listOf(
            libraryWithArtifact("a"),
            libraryWithArtifact("b"),
            libraryWithArtifact("c")
        )
        val resolved = LibraryResolver.resolveClasspathLibraries(entries)
        assertEquals(listOf("a", "b", "c"), resolved.map { it.mavenName })
    }
}
