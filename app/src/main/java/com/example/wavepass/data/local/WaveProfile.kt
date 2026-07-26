package com.example.wavepass.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

// Represents the local user's own shared profile.
// This is the only profile that gets broadcast to nearby devices.
// There should only ever be one row in this table.
@Entity(tableName = "wave_profile")
data class WaveProfile(
    @PrimaryKey
    val id: Long = 1, // fixed id, since there's only ever one local profile
    val anonymousId: String, // randomly generated UUID, created once on first app launch
    val displayAlias: String, // optional user-chosen nickname, never tied to real identity
    val favoriteArtist: String,
    val favoriteSongTitles: List<String>, // up to 3-5 track titles, text only
    val snippetSongId: Long? = null // references Song.id, the track whose snippet gets shared
)