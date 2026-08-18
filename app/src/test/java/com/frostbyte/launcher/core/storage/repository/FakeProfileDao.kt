package com.frostbyte.launcher.core.storage.repository

import com.frostbyte.launcher.core.storage.db.ProfileDao
import com.frostbyte.launcher.core.storage.db.ProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Pure in-memory fake of ProfileDao for JVM-level unit tests of
 * ProfileRepository, without needing Robolectric or an instrumented
 * in-memory Room database.
 *
 * Note: this fake does NOT replicate the real DAO's ORDER BY clause -
 * observeAll() here returns insertion order, not "recently played first."
 * Tests that care about sort order belong in an instrumented test against
 * the real Room database, not against this fake.
 */
class FakeProfileDao : ProfileDao {
    private var nextId = 1L
    private val state = MutableStateFlow<List<ProfileEntity>>(emptyList())

    override fun observeAll(): StateFlow<List<ProfileEntity>> = state

    override suspend fun getById(id: Long): ProfileEntity? =
        state.value.firstOrNull { it.id == id }

    override suspend fun getDefault(): ProfileEntity? =
        state.value.firstOrNull { it.isDefault }

    override suspend fun insert(profile: ProfileEntity): Long {
        val id = nextId++
        state.value = state.value + profile.copy(id = id)
        return id
    }

    override suspend fun update(profile: ProfileEntity) {
        state.value = state.value.map { if (it.id == profile.id) profile else it }
    }

    override suspend fun delete(profile: ProfileEntity) {
        state.value = state.value.filterNot { it.id == profile.id }
    }

    override suspend fun clearDefaultFlag() {
        state.value = state.value.map { it.copy(isDefault = false) }
    }

    override suspend fun setDefaultFlag(id: Long) {
        state.value = state.value.map { if (it.id == id) it.copy(isDefault = true) else it }
    }

    override suspend fun updateLastPlayed(id: Long, timestamp: Long) {
        state.value = state.value.map {
            if (it.id == id) it.copy(lastPlayedEpochMillis = timestamp) else it
        }
    }
}
