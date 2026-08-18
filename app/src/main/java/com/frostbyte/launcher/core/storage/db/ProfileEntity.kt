package com.frostbyte.launcher.core.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.frostbyte.launcher.core.common.Loader

/**
 * Profile entity per Section 7 of the PRD. Mods/shaders/resource-packs are
 * deliberately NOT inline fields here - they belong to their own tables
 * (added in Phase 7) linked by profileId, since a profile can reference many
 * of each and that's a one-to-many relationship, not a scalar column.
 *
 * jvmArguments is stored as a single string (space-separated), matching how
 * they'll actually be passed to the Java process in Phase 4, rather than a
 * List<String> that would need its own TypeConverter for no real benefit.
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val minecraftVersion: String,
    val loader: Loader,
    val javaRuntimeVersion: Int,
    val ramAllocationMb: Int,
    val jvmArguments: String,
    val resolutionWidth: Int?,
    val resolutionHeight: Int?,
    val gameDirectory: String,
    val lastPlayedEpochMillis: Long?,
    val isDefault: Boolean = false
)
