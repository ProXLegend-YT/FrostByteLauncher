package com.frostbyte.launcher.core.network.service

import com.frostbyte.launcher.core.network.model.ForgePromotionsResponse
import com.frostbyte.launcher.core.network.model.NeoForgeVersionsResponse

class FakeForgeMetaService : ForgeMetaService {
    var promotionsResponse: ForgePromotionsResponse? = null
    var error: Throwable? = null

    override suspend fun getPromotions(): ForgePromotionsResponse {
        error?.let { throw it }
        return promotionsResponse ?: error("FakeForgeMetaService: no promotionsResponse configured")
    }
}

class FakeNeoForgeMetaService : NeoForgeMetaService {
    var versionsResponse: NeoForgeVersionsResponse? = null
    var error: Throwable? = null

    override suspend fun getVersions(): NeoForgeVersionsResponse {
        error?.let { throw it }
        return versionsResponse ?: error("FakeNeoForgeMetaService: no versionsResponse configured")
    }
}
