package com.example.wavepass.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WaveProfileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: WaveProfile)

    @Update
    suspend fun update(profile: WaveProfile)

    // There's only ever one profile (id = 1), so we observe it directly.
    @Query("SELECT * FROM wave_profile WHERE id = 1")
    fun getProfile(): Flow<WaveProfile?>
}