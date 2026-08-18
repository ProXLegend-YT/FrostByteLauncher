package com.frostbyte.launcher.core.storage.db

import androidx.room.TypeConverter
import com.frostbyte.launcher.core.common.Loader

class Converters {
    @TypeConverter
    fun loaderToString(loader: Loader): String = loader.name

    @TypeConverter
    fun stringToLoader(value: String): Loader = Loader.valueOf(value)

    @TypeConverter
    fun downloadStatusToString(status: DownloadStatus): String = status.name

    @TypeConverter
    fun stringToDownloadStatus(value: String): DownloadStatus = DownloadStatus.valueOf(value)
}
