package com.example.wavepass.data.repository

import com.example.wavepass.data.local.Song
import com.example.wavepass.data.local.SongDao
import kotlinx.coroutines.flow.Flow

// Mediates all the access to Song data.
// The UI/ViewModel layer talks to this class, never directly to SongDao.
class SongRepository(private val songDao: SongDao) {

    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

    suspend fun getSongById(songId: Long): Song? = songDao.getSongById(songId)

    suspend fun addSong(song: Song): Long = songDao.insert(song)

    suspend fun updateSong(song: Song) = songDao.update(song)

    suspend fun deleteSong(song: Song) = songDao.delete(song)
}