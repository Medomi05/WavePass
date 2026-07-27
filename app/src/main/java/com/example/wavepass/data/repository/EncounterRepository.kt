package com.example.wavepass.data.repository

import com.example.wavepass.data.local.Encounter
import com.example.wavepass.data.local.EncounterDao
import kotlinx.coroutines.flow.Flow

class EncounterRepository(private val encounterDao: EncounterDao) {

    fun getAllEncounters(): Flow<List<Encounter>> = encounterDao.getAllEncounters()

    // Saves a newly received profile as an encounter.
    // If we've already met this anonymousId before, we increment the existing
    // encounter count instead of creating a duplicate row.
    suspend fun recordEncounter(newEncounter: Encounter) {
        val existing = encounterDao.findByAnonymousId(newEncounter.remoteAnonymousId)

        if (existing != null) {
            val updated = existing.copy(
                encounterCount = existing.encounterCount + 1,
                receivedAtTimestamp = newEncounter.receivedAtTimestamp
            )
            encounterDao.update(updated)
        } else {
            encounterDao.insert(newEncounter)
        }
    }
}