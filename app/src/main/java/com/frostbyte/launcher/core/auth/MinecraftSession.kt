package com.frostbyte.launcher.core.auth

/**
 * A fully authenticated Minecraft session, resulting from a successful walk
 * through the entire Microsoft -> Xbox Live -> XSTS -> Minecraft Services
 * chain, including a verified ownership check (Section 5 of the PRD).
 *
 * minecraftAccessToken and msRefreshToken are secrets - see
 * SecureSessionStore for how they're persisted. This class itself is just
 * an in-memory holder; nothing in this file writes to disk.
 */
data class MinecraftSession(
    val minecraftUuid: String,
    val minecraftUsername: String,
    val minecraftAccessToken: String,
    val minecraftAccessTokenExpiresAtEpochMillis: Long,
    val msRefreshToken: String
) {
    fun isAccessTokenExpired(nowEpochMillis: Long = System.currentTimeMillis()): Boolean =
        nowEpochMillis >= minecraftAccessTokenExpiresAtEpochMillis
}
