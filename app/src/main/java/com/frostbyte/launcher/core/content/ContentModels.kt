package com.frostbyte.launcher.core.content

/** Which category of installable content this is - drives which on-disk folder it belongs in. */
enum class ContentType {
    MOD, SHADER, RESOURCE_PACK;

    /** Modrinth's project_type facet value for this content type. */
    val modrinthProjectType: String
        get() = when (this) {
            MOD -> "mod"
            SHADER -> "shader"
            RESOURCE_PACK -> "resourcepack"
        }
}

data class ContentSearchResult(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val iconUrl: String?,
    val downloadCount: Int,
    val contentType: ContentType
)

data class ContentVersion(
    val id: String,
    val versionNumber: String,
    val gameVersions: List<String>,
    val loaders: List<String>,
    val fileUrl: String,
    val filename: String,
    val sizeBytes: Long,
    val sha1: String
)
