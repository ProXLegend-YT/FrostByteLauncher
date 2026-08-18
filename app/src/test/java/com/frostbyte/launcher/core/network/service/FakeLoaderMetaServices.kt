package com.frostbyte.launcher.core.network.service

import com.frostbyte.launcher.core.network.model.LoaderProfileResponse
import com.frostbyte.launcher.core.network.model.LoaderVersionEntry

class FakeFabricMetaService : FabricMetaService {
    var versionsByMcVersion: MutableMap<String, List<LoaderVersionEntry>> = mutableMapOf()
    var profilesByKey: MutableMap<String, LoaderProfileResponse> = mutableMapOf()
    var error: Throwable? = null

    override suspend fun getLoaderVersions(minecraftVersion: String): List<LoaderVersionEntry> {
        error?.let { throw it }
        return versionsByMcVersion[minecraftVersion] ?: error("FakeFabricMetaService: no versions configured for $minecraftVersion")
    }

    override suspend fun getLoaderProfile(minecraftVersion: String, loaderVersion: String): LoaderProfileResponse {
        error?.let { throw it }
        val key = "$minecraftVersion:$loaderVersion"
        return profilesByKey[key] ?: error("FakeFabricMetaService: no profile configured for $key")
    }
}

class FakeQuiltMetaService : QuiltMetaService {
    var versionsByMcVersion: MutableMap<String, List<LoaderVersionEntry>> = mutableMapOf()
    var profilesByKey: MutableMap<String, LoaderProfileResponse> = mutableMapOf()
    var error: Throwable? = null

    override suspend fun getLoaderVersions(minecraftVersion: String): List<LoaderVersionEntry> {
        error?.let { throw it }
        return versionsByMcVersion[minecraftVersion] ?: error("FakeQuiltMetaService: no versions configured for $minecraftVersion")
    }

    override suspend fun getLoaderProfile(minecraftVersion: String, loaderVersion: String): LoaderProfileResponse {
        error?.let { throw it }
        val key = "$minecraftVersion:$loaderVersion"
        return profilesByKey[key] ?: error("FakeQuiltMetaService: no profile configured for $key")
    }
}
