package com.example.wavepass.ui.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wavepass.data.repository.PlaylistRepository

class PlaylistViewModelFactory(private val playlistRepository: PlaylistRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return PlaylistViewModel(playlistRepository) as T
    }
}