package com.frostbyte.launcher.core.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Narrow interface for persisting/loading/clearing the current session.
 * Lets AuthRepository be unit-tested with an in-memory fake instead of
 * needing a real Android Keystore.
 */
interface SessionStore {
    fun save(session: MinecraftSession)
    fun load(): MinecraftSession?
    fun clear()
}

/**
 * Stores the Minecraft session (including the Microsoft refresh token) via
 * EncryptedSharedPreferences, which is backed by a real Android Keystore
 * key - not plaintext SharedPreferences, not DataStore (which has no
 * built-in encryption). This is exactly the class of storage Section 27 of
 * the PRD requires for credentials/tokens: "Android Keystore-backed secure
 * storage," and access tokens/refresh tokens are NEVER logged anywhere in
 * this class or its callers.
 */
class SecureSessionStore(context: Context) : SessionStore {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "frostbyte_secure_session",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun save(session: MinecraftSession) {
        prefs.edit()
            .putString(KEY_UUID, session.minecraftUuid)
            .putString(KEY_USERNAME, session.minecraftUsername)
            .putString(KEY_ACCESS_TOKEN, session.minecraftAccessToken)
            .putLong(KEY_ACCESS_TOKEN_EXPIRES, session.minecraftAccessTokenExpiresAtEpochMillis)
            .putString(KEY_MS_REFRESH_TOKEN, session.msRefreshToken)
            .apply()
    }

    override fun load(): MinecraftSession? {
        val uuid = prefs.getString(KEY_UUID, null) ?: return null
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val expiresAt = prefs.getLong(KEY_ACCESS_TOKEN_EXPIRES, 0L)
        val refreshToken = prefs.getString(KEY_MS_REFRESH_TOKEN, null) ?: return null

        return MinecraftSession(
            minecraftUuid = uuid,
            minecraftUsername = username,
            minecraftAccessToken = accessToken,
            minecraftAccessTokenExpiresAtEpochMillis = expiresAt,
            msRefreshToken = refreshToken
        )
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_UUID = "minecraft_uuid"
        const val KEY_USERNAME = "minecraft_username"
        const val KEY_ACCESS_TOKEN = "minecraft_access_token"
        const val KEY_ACCESS_TOKEN_EXPIRES = "minecraft_access_token_expires"
        const val KEY_MS_REFRESH_TOKEN = "ms_refresh_token"
    }
}
