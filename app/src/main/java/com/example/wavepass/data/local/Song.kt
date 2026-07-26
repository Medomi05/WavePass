package com.example.wavepass.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val filePath: String,       // local path of the audio file
    val durationMs: Long,       // total length of the song (ms)
    val snippetStartMs: Long? = null
)