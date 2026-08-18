package com.frostbyte.launcher.core.launch

import com.frostbyte.launcher.core.network.model.AssetIndexResponse
import com.frostbyte.launcher.core.network.model.AssetObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AssetResolverTest {

    @Test
    fun `download URL uses the first two hash characters as the shard directory`() {
        val index = AssetIndexResponse(
            objects = mapOf(
                "minecraft/sounds/random/click.ogg" to AssetObject(
                    hash = "de1e2b6a8f4c9d3e5a7b1c8d0f2e4a6b8c0d2e4f",
                    size = 4096L
                )
            )
        )

        val resolved = AssetResolver.resolveAssets(index)

        assertEquals(1, resolved.size)
        val asset = resolved.first()
        assertEquals(
            "https://resources.download.minecraft.net/de/de1e2b6a8f4c9d3e5a7b1c8d0f2e4a6b8c0d2e4f",
            asset.downloadUrl
        )
    }

    @Test
    fun `object relative path is shard-directory then full hash, not the virtual path`() {
        val index = AssetIndexResponse(
            objects = mapOf(
                "minecraft/textures/block/stone.png" to AssetObject(hash = "ab12cd34ef", size = 512L)
            )
        )

        val resolved = AssetResolver.resolveAssets(index).first()

        // Must NOT resemble "minecraft/textures/block/stone.png" - assets are
        // stored content-addressed on disk, same as on Mojang's real CDN.
        assertEquals("objects/ab/ab12cd34ef", resolved.objectRelativePath)
        assertTrue(!resolved.objectRelativePath.contains("textures"))
    }

    @Test
    fun `objectFile resolves to a real File under the assets directory`() {
        val index = AssetIndexResponse(
            objects = mapOf("x" to AssetObject(hash = "ff00aa11bb", size = 10L))
        )
        val asset = AssetResolver.resolveAssets(index).first()
        val assetsDir = File("/data/frostbyte/assets")

        val file = AssetResolver.objectFile(assetsDir, asset)

        assertEquals(File("/data/frostbyte/assets/objects/ff/ff00aa11bb"), file)
    }

    @Test
    fun `totalSizeBytes sums every asset regardless of shard`() {
        val index = AssetIndexResponse(
            objects = mapOf(
                "a" to AssetObject(hash = "aaaa", size = 100L),
                "b" to AssetObject(hash = "bbbb", size = 250L),
                "c" to AssetObject(hash = "cccc", size = 50L)
            )
        )

        val total = AssetResolver.totalSizeBytes(AssetResolver.resolveAssets(index))

        assertEquals(400L, total)
    }

    @Test
    fun `empty asset index resolves to an empty list`() {
        val resolved = AssetResolver.resolveAssets(AssetIndexResponse(objects = emptyMap()))
        assertTrue(resolved.isEmpty())
    }

    @Test
    fun `virtual path is preserved even though it is not used for the download URL`() {
        val index = AssetIndexResponse(
            objects = mapOf("minecraft/lang/en_us.json" to AssetObject(hash = "1234abcd", size = 20L))
        )
        val resolved = AssetResolver.resolveAssets(index).first()
        assertEquals("minecraft/lang/en_us.json", resolved.virtualPath)
    }
}
