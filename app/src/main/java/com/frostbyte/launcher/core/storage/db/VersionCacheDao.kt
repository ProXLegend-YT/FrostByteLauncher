package com.frostbyte.launcher.core.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface VersionCacheDao {

    @Query("SELECT * FROM version_cache ORDER BY releaseTimeEpochMillis DESC")
    fun observeAll(): Flow<List<VersionCacheEntity>>

    @Query("SELECT * FROM version_cache ORDER BY releaseTimeEpochMillis DESC")
    suspend fun getAllOnce(): List<VersionCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(versions: List<VersionCacheEntity>)

    @Query("DELETE FROM version_cache")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(versions: List<VersionCacheEntity>) {
        clear()
        insertAll(versions)
    }
}
