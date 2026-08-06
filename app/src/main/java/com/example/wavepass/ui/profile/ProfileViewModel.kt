package com.example.wavepass.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.wavepass.data.local.Song
import com.example.wavepass.data.local.WavePassDatabase
import com.example.wavepass.data.local.WaveProfile
import com.example.wavepass.data.repository.SongRepository
import com.example.wavepass.data.repository.WaveProfileRepository
import com.example.wavepass.wavepass.DeviceIdentity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val database = WavePassDatabase.getInstance(application)
    private val songRepository = SongRepository(database.songDao())
    private val waveProfileRepository = WaveProfileRepository(database.waveProfileDao())
    private val anonymousId = DeviceIdentity.getOrCreateAnonymousId(application)

    val songs: StateFlow<List<Song>> = songRepository.getAllSongs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val profile: StateFlow<WaveProfile?> = waveProfileRepository.getProfile()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Automatically computed: the artist with the most imported songs in the library.
    val computedFavoriteArtist: StateFlow<String> = songRepository.getAllSongs()
        .combine(songRepository.getAllSongs()) { list, _ -> list }
        .let { flow ->
            kotlinx.coroutines.flow.MutableStateFlow("Not enough songs yet").also { result ->
                viewModelScope.launch {
                    flow.collect { list ->
                        result.value = computeMostCommonArtist(list)
                    }
                }
            }
        }

    private fun computeMostCommonArtist(songs: List<Song>): String {
        if (songs.isEmpty()) return "Import some songs first"
        return songs.groupingBy { it.artist }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key ?: "Unknown"
    }

    fun saveProfile(alias: String, selectedSongTitles: List<String>) {
        viewModelScope.launch {
            val artist = computeMostCommonArtist(songs.value)
            waveProfileRepository.saveProfile(
                WaveProfile(
                    anonymousId = anonymousId,
                    displayAlias = alias.ifBlank { "Anonymous Phantom" },
                    favoriteArtist = artist,
                    favoriteSongTitles = selectedSongTitles
                )
            )
        }
    }
}