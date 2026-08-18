package com.frostbyte.launcher

import android.content.Context
import android.hardware.input.InputManager
import com.frostbyte.launcher.core.auth.AuthRepository
import com.frostbyte.launcher.core.auth.SecureSessionStore
import com.frostbyte.launcher.core.backup.WorldBackupManager
import com.frostbyte.launcher.core.content.ModrinthContentRepository
import com.frostbyte.launcher.core.controls.ControlsRepository
import com.frostbyte.launcher.core.controls.GamepadDetector
import com.frostbyte.launcher.core.diagnostics.CrashReporter
import com.frostbyte.launcher.core.diagnostics.DeviceCapabilitiesProvider
import com.frostbyte.launcher.core.download.DownloadScheduler
import com.frostbyte.launcher.core.download.FileDownloader
import com.frostbyte.launcher.core.filesystem.FileManager
import com.frostbyte.launcher.core.modloader.FabricQuiltRepository
import com.frostbyte.launcher.core.modloader.ForgeVersionRepository
import com.frostbyte.launcher.core.network.NetworkModule
import com.frostbyte.launcher.core.storage.datastore.SettingsRepository
import com.frostbyte.launcher.core.storage.db.FrostByteDatabase
import com.frostbyte.launcher.core.storage.repository.DownloadRepository
import com.frostbyte.launcher.core.storage.repository.ProfileRepository
import com.frostbyte.launcher.core.storage.repository.VersionRepository
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Minimal manual service locator. A full DI framework (Hilt) is deliberately
 * deferred - see docs/KNOWN_GAPS.md for when to revisit this.
 */
class FrostByteContainer(context: Context) {
    private val database = FrostByteDatabase.getInstance(context)

    val profileRepository = ProfileRepository(database.profileDao())
    val settingsRepository = SettingsRepository(context)
    val versionRepository = VersionRepository(
        service = NetworkModule.mojangMetaService,
        cacheDao = database.versionCacheDao()
    )
    val downloadRepository = DownloadRepository(database.downloadDao())

    // Separate OkHttpClient from NetworkModule's (used for small JSON API
    // calls) with much longer timeouts appropriate for large file transfers
    // (client jars, asset packs) that can legitimately take minutes.
    private val downloadHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.MINUTES)
        .writeTimeout(2, TimeUnit.MINUTES)
        .build()

    val fileDownloader = FileDownloader(downloadHttpClient)
    val downloadScheduler = DownloadScheduler(context.applicationContext)
    val fileManager = FileManager(context.applicationContext)

    val authRepository = AuthRepository(
        microsoftAuthService = NetworkModule.microsoftAuthService,
        xboxAuthService = NetworkModule.xboxAuthService,
        minecraftAuthService = NetworkModule.minecraftAuthService,
        sessionStore = SecureSessionStore(context.applicationContext)
    )

    val fabricQuiltRepository = FabricQuiltRepository(
        fabricMetaService = NetworkModule.fabricMetaService,
        quiltMetaService = NetworkModule.quiltMetaService
    )
    val forgeVersionRepository = ForgeVersionRepository(
        forgeMetaService = NetworkModule.forgeMetaService,
        neoForgeMetaService = NetworkModule.neoForgeMetaService
    )

    val modrinthContentRepository = ModrinthContentRepository(NetworkModule.modrinthService)

    val controlsRepository = ControlsRepository(context.applicationContext)
    val gamepadDetector = GamepadDetector(
        context.applicationContext.getSystemService(Context.INPUT_SERVICE) as InputManager
    )

    val deviceCapabilitiesProvider = DeviceCapabilitiesProvider(context.applicationContext)

    val crashReporter = CrashReporter(File(fileManager.logsDir(), "crash_reports"))
    val worldBackupManager = WorldBackupManager(File(fileManager.profilesDir().parentFile, "world_backups"))
}
