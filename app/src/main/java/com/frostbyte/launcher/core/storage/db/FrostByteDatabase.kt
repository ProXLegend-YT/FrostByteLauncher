package com.frostbyte.launcher.core.storage.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [ProfileEntity::class, VersionCacheEntity::class, DownloadEntity::class],
    version = 3,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class FrostByteDatabase : RoomDatabase() {

    abstract fun profileDao(): ProfileDao
    abstract fun versionCacheDao(): VersionCacheDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        private const val DATABASE_NAME = "frostbyte.db"

        @Volatile
        private var instance: FrostByteDatabase? = null

        fun getInstance(context: Context): FrostByteDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FrostByteDatabase::class.java,
                    DATABASE_NAME
                )
                    // Destructive fallback is deliberate and temporary: this
                    // project has never shipped a public release, so there is
                    // no installed base with real v1 data to preserve. A real
                    // Migration(1, 2) must replace this before Phase 10
                    // (release) - silently wiping user profiles on upgrade
                    // is only acceptable pre-release.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
        }
    }
}
