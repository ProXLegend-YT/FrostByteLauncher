package com.frostbyte.launcher.core.auth

class FakeSessionStore : SessionStore {
    private var stored: MinecraftSession? = null
    var saveCallCount = 0
        private set

    override fun save(session: MinecraftSession) {
        stored = session
        saveCallCount++
    }

    override fun load(): MinecraftSession? = stored

    override fun clear() {
        stored = null
    }
}
