package com.example.wavepass.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

// Represents a playlist together with its ordered list of songs.
// Room resolves this relation automatically via the cross-ref table.
data class PlaylistWithSongs(
    @Embedded val playlist: Playlist,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = PlaylistSongCrossRef::class,
            parentColumn = "playlistId",
            entityColumn = "songId"
        )
    )
    val songs: List<Song>
)