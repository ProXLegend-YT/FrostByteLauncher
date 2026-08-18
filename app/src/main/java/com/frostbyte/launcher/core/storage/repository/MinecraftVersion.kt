package com.frostbyte.launcher.core.storage.repository

import com.frostbyte.launcher.core.network.model.MinecraftVersionType

data class MinecraftVersion(
    val id: String,
    val type: MinecraftVersionType,
    val releaseTimeEpochMillis: Long,
    val detailUrl: String,
    val sha1: String
)
