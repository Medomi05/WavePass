package com.example.wavepass.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Represents a profile received from another nearby device.
// Each row is one encounter with another anonymous user.
@Entity(tableName = "encounters")
data class Encounter(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteAnonymousId: String, // the other device's anonymous id, used to detect repeat encounters
    val remoteDisplayAlias: String,
    val favoriteArtist: String,
    val favoriteSongTitles: List<String>,
    val receivedAtTimestamp: Long, // epoch millis, when the encounter happened
    val encounterCount: Int = 1 // how many times we've crossed paths with this same anonymousId
)