package com.frostbyte.launcher.core.network.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModrinthFacetsBuilderTest {

    @Test
    fun `returns null when nothing is specified`() {
        assertNull(ModrinthFacetsBuilder.build())
    }

    @Test
    fun `single project type produces one group`() {
        val result = ModrinthFacetsBuilder.build(projectType = "mod")
        assertEquals("""[["project_type:mod"]]""", result)
    }

    @Test
    fun `project type and minecraft version produce two AND-ed groups`() {
        val result = ModrinthFacetsBuilder.build(projectType = "mod", minecraftVersion = "1.21.1")
        assertEquals("""[["project_type:mod"],["versions:1.21.1"]]""", result)
    }

    @Test
    fun `all three filters produce three AND-ed groups in a fixed order`() {
        val result = ModrinthFacetsBuilder.build(projectType = "shader", minecraftVersion = "1.21.1", loader = "fabric")
        assertEquals(
            """[["project_type:shader"],["versions:1.21.1"],["categories:fabric"]]""",
            result
        )
    }

    @Test
    fun `loader only, no project type or version`() {
        val result = ModrinthFacetsBuilder.build(loader = "forge")
        assertEquals("""[["categories:forge"]]""", result)
    }
}
