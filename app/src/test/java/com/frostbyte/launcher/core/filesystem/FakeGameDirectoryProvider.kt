package com.frostbyte.launcher.core.filesystem

import java.io.File

class FakeGameDirectoryProvider(private val root: File) : GameDirectoryProvider {
    override fun versionsDir(): File = File(root, "versions").apply { mkdirs() }
    override fun modsCacheDir(): File = File(root, "mods_cache").apply { mkdirs() }
    override fun shadersCacheDir(): File = File(root, "shaders_cache").apply { mkdirs() }
    override fun resourcePacksCacheDir(): File = File(root, "resource_packs_cache").apply { mkdirs() }
}
