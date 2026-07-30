package com.example.wavepass.ui.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wavepass.data.local.Playlist
import com.example.wavepass.data.local.PlaylistWithSongs
import com.example.wavepass.data.local.Song
import com.example.wavepass.data.repository.PlaylistRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class PlaylistViewModel(private val playlistRepository: PlaylistRepository) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = playlistRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // The playlist currently being viewed in detail, or null if none selected.
    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedPlaylistWithSongs: StateFlow<PlaylistWithSongs?> = _selectedPlaylistId
        .flatMapLatest { id ->
            if (id == null) kotlinx.coroutines.flow.flowOf(null)
            else playlistRepository.getPlaylistWithOrderedSongs(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun openPlaylist(playlistId: Long) {
        _selectedPlaylistId.value = playlistId
    }

    fun closePlaylist() {
        _selectedPlaylistId.value = null
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { playlistRepository.createPlaylist(name.trim()) }
    }

    fun deletePlaylist(playlist: Playlist) {
        viewModelScope.launch {
            if (_selectedPlaylistId.value == playlist.id) _selectedPlaylistId.value = null
            playlistRepository.deletePlaylist(playlist)
        }
    }

    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch { playlistRepository.addSongToPlaylist(playlistId, song) }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { playlistRepository.removeSongFromPlaylist(playlistId, songId) }
    }

    fun reorderSongs(playlistId: Long, orderedSongIds: List<Long>) {
        viewModelScope.launch { playlistRepository.reorderSongs(playlistId, orderedSongIds) }
    }
}