package com.frostbyte.launcher.core.storage.repository

import com.frostbyte.launcher.core.storage.db.VersionCacheDao
import com.frostbyte.launcher.core.storage.db.VersionCacheEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeVersionCacheDao : VersionCacheDao {
    private val state = MutableStateFlow<List<VersionCacheEntity>>(emptyList())

    override fun observeAll(): StateFlow<List<VersionCacheEntity>> = state

    override suspend fun getAllOnce(): List<VersionCacheEntity> = state.value

    override suspend fun insertAll(versions: List<VersionCacheEntity>) {
        // Mirrors the real DAO's REPLACE conflict strategy keyed by id.
        val byId = state.value.associateBy { it.id }.toMutableMap()
        versions.forEach { byId[it.id] = it }
        state.value = byId.values.toList()
    }

    override suspend fun clear() {
        state.value = emptyList()
    }

    override suspend fun replaceAll(versions: List<VersionCacheEntity>) {
        clear()
        insertAll(versions)
    }
}
