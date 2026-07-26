package com.example.wavepass.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EncounterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(encounter: Encounter): Long

    @Update
    suspend fun update(encounter: Encounter)

    @Query("SELECT * FROM encounters ORDER BY receivedAtTimestamp DESC")
    fun getAllEncounters(): Flow<List<Encounter>>

    // Used to check if we've already met this anonymous id before,
    // so we can increment encounterCount instead of creating a duplicate row.
    @Query("SELECT * FROM encounters WHERE remoteAnonymousId = :anonymousId LIMIT 1")
    suspend fun findByAnonymousId(anonymousId: String): Encounter?
}