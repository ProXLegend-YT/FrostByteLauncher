package com.frostbyte.launcher.core.storage.repository

import com.frostbyte.launcher.core.common.Loader

/**
 * Domain-level profile model. Kept separate from ProfileEntity (the Room
 * row) so the UI/ViewModel layer never depends on a @Entity-annotated class -
 * if the DB schema changes shape later, only ProfileRepository's mapping
 * needs to change, not every screen that displays a profile.
 */
data class Profile(
    val id: Long,
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
    val isDefault: Boolean
) {
    val ramAllocationGb: Float get() = ramAllocationMb / 1024f
}

/** Fields needed to create or edit a profile - deliberately excludes id/isDefault/lastPlayed. */
data class ProfileDraft(
    val name: String,
    val minecraftVersion: String,
    val loader: Loader,
    val javaRuntimeVersion: Int = 17,
    val ramAllocationMb: Int = 4096,
    val jvmArguments: String = "",
    val resolutionWidth: Int? = null,
    val resolutionHeight: Int? = null,
    val gameDirectory: String
)
