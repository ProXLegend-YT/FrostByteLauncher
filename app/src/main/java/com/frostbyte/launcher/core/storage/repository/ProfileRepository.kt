package com.frostbyte.launcher.core.storage.repository

import com.frostbyte.launcher.core.common.FrostByteResult
import com.frostbyte.launcher.core.storage.db.ProfileDao
import com.frostbyte.launcher.core.storage.db.ProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * ProfileRepository - the ContentRepository-family interface referenced in
 * Section 11 of the PRD. ViewModels talk to this, never to ProfileDao
 * directly, so the storage backend (Room today) can change without touching
 * UI code.
 */
class ProfileRepository(private val dao: ProfileDao) {

    fun observeProfiles(): Flow<List<Profile>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getProfile(id: Long): Profile? = dao.getById(id)?.toDomain()

    suspend fun getDefaultProfile(): Profile? = dao.getDefault()?.toDomain()

    suspend fun createProfile(draft: ProfileDraft): FrostByteResult<Long> {
        if (draft.name.isBlank()) {
            return FrostByteResult.Failure("Profile name cannot be empty")
        }
        if (draft.ramAllocationMb <= 0) {
            return FrostByteResult.Failure("RAM allocation must be positive")
        }
        return try {
            val id = dao.insert(
                ProfileEntity(
                    name = draft.name,
                    minecraftVersion = draft.minecraftVersion,
                    loader = draft.loader,
                    javaRuntimeVersion = draft.javaRuntimeVersion,
                    ramAllocationMb = draft.ramAllocationMb,
                    jvmArguments = draft.jvmArguments,
                    resolutionWidth = draft.resolutionWidth,
                    resolutionHeight = draft.resolutionHeight,
                    gameDirectory = draft.gameDirectory,
                    lastPlayedEpochMillis = null,
                    isDefault = false
                )
            )
            FrostByteResult.Success(id)
        } catch (e: Exception) {
            FrostByteResult.Failure("Failed to create profile", e)
        }
    }

    suspend fun updateProfile(profile: Profile): FrostByteResult<Unit> {
        if (profile.name.isBlank()) {
            return FrostByteResult.Failure("Profile name cannot be empty")
        }
        return try {
            dao.update(profile.toEntity())
            FrostByteResult.Success(Unit)
        } catch (e: Exception) {
            FrostByteResult.Failure("Failed to update profile", e)
        }
    }

    suspend fun deleteProfile(profile: Profile): FrostByteResult<Unit> {
        return try {
            dao.delete(profile.toEntity())
            FrostByteResult.Success(Unit)
        } catch (e: Exception) {
            FrostByteResult.Failure("Failed to delete profile", e)
        }
    }

    suspend fun setAsDefault(id: Long) {
        dao.clearDefaultFlag()
        dao.setDefaultFlag(id)
    }

    suspend fun markPlayedNow(id: Long) {
        dao.updateLastPlayed(id, System.currentTimeMillis())
    }
}

private fun ProfileEntity.toDomain() = Profile(
    id = id,
    name = name,
    minecraftVersion = minecraftVersion,
    loader = loader,
    javaRuntimeVersion = javaRuntimeVersion,
    ramAllocationMb = ramAllocationMb,
    jvmArguments = jvmArguments,
    resolutionWidth = resolutionWidth,
    resolutionHeight = resolutionHeight,
    gameDirectory = gameDirectory,
    lastPlayedEpochMillis = lastPlayedEpochMillis,
    isDefault = isDefault
)

private fun Profile.toEntity() = ProfileEntity(
    id = id,
    name = name,
    minecraftVersion = minecraftVersion,
    loader = loader,
    javaRuntimeVersion = javaRuntimeVersion,
    ramAllocationMb = ramAllocationMb,
    jvmArguments = jvmArguments,
    resolutionWidth = resolutionWidth,
    resolutionHeight = resolutionHeight,
    gameDirectory = gameDirectory,
    lastPlayedEpochMillis = lastPlayedEpochMillis,
    isDefault = isDefault
)
