package com.example.wavepass.ui.music

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.wavepass.data.repository.SongRepository
import com.example.wavepass.playback.AudioPlayerManager

class MusicViewModelFactory(
    private val songRepository: SongRepository,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MusicViewModel(songRepository, audioPlayerManager) as T
    }
}