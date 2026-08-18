package com.frostbyte.launcher.core.content

/**
 * CurseForge's API (unlike Modrinth's) requires a registered API key tied
 * to a specific application, requested from Overwolf/CurseForge at
 * https://console.curseforge.com/ - this is not optional and cannot be
 * worked around; requests without a valid key are rejected outright.
 *
 * FrostByte does not have a registered CurseForge API key yet. This is a
 * real release blocker (tracked in docs/KNOWN_GAPS.md), not a coding task,
 * exactly like MicrosoftAuthConfig's missing Azure AD client ID and
 * JavaRuntimeCatalog's missing JRE URLs - same pattern, same reasoning.
 *
 * apiKey is deliberately blank in the default instance so a missing
 * registration fails loudly and immediately rather than silently sending a
 * doomed request. CurseForgeContentRepository (once built) must check
 * isConfigured() first, the same way AuthRepository checks
 * MicrosoftAuthConfig.
 */
data class CurseForgeConfig(val apiKey: String) {
    fun isConfigured(): Boolean = apiKey.isNotBlank()

    companion object {
        val default = CurseForgeConfig(apiKey = "")
        const val BASE_URL = "https://api.curseforge.com/"
    }
}
