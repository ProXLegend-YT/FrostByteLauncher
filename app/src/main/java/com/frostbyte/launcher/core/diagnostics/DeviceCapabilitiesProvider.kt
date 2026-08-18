package com.frostbyte.launcher.core.diagnostics

import android.app.ActivityManager
import android.content.Context
import android.os.StatFs

data class DeviceCapabilities(
    val cpuCoreCount: Int,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val isLowRamDevice: Boolean,
    val freeStorageMb: Long,
    val totalStorageMb: Long
)

/**
 * Real device capability detection via Android's own APIs - genuinely
 * queries actual hardware, no placeholder numbers. Backs both the
 * Performance Center screen and SpaceQualityAdvisor's auto-detection.
 */
class DeviceCapabilitiesProvider(private val context: Context) {

    fun detect(): DeviceCapabilities {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val storageDir = context.getExternalFilesDir(null) ?: context.filesDir
        val statFs = StatFs(storageDir.absolutePath)
        val blockSize = statFs.blockSizeLong
        val freeStorageMb = (statFs.availableBlocksLong * blockSize) / (1024 * 1024)
        val totalStorageMb = (statFs.blockCountLong * blockSize) / (1024 * 1024)

        return DeviceCapabilities(
            cpuCoreCount = Runtime.getRuntime().availableProcessors(),
            totalRamMb = memoryInfo.totalMem / (1024 * 1024),
            availableRamMb = memoryInfo.availMem / (1024 * 1024),
            isLowRamDevice = activityManager.isLowRamDevice,
            freeStorageMb = freeStorageMb,
            totalStorageMb = totalStorageMb
        )
    }
}
