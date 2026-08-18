package com.frostbyte.launcher.core.storage.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    // NULLS LAST isn't guaranteed on the framework SQLite versions shipped by
    // older Android devices (min SDK 26) - CASE-based ordering achieves the
    // same "never-played profiles sort after recently-played ones" behavior
    // without depending on SQLite 3.30+.
    @Query(
        """
        SELECT * FROM profiles
        ORDER BY CASE WHEN lastPlayedEpochMillis IS NULL THEN 1 ELSE 0 END,
                 lastPlayedEpochMillis DESC,
                 name ASC
        """
    )
    fun observeAll(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getById(id: Long): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefault(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(profile: ProfileEntity): Long

    @Update
    suspend fun update(profile: ProfileEntity)

    @Delete
    suspend fun delete(profile: ProfileEntity)

    @Query("UPDATE profiles SET isDefault = 0")
    suspend fun clearDefaultFlag()

    @Query("UPDATE profiles SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultFlag(id: Long)

    @Query("UPDATE profiles SET lastPlayedEpochMillis = :timestamp WHERE id = :id")
    suspend fun updateLastPlayed(id: Long, timestamp: Long)
}
