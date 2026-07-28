package com.example.wavepass.ui.music

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wavepass.data.local.Song
import com.example.wavepass.data.repository.SongRepository
import com.example.wavepass.playback.AudioPlayerManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MusicViewModel(
    private val songRepository: SongRepository,
    private val audioPlayerManager: AudioPlayerManager
) : ViewModel() {

    val songs: StateFlow<List<Song>> = songRepository.getAllSongs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun importSong(context: Context, uri: Uri) {
        viewModelScope.launch {
            val filePath = uri.toString()
            if (songRepository.songExists(filePath)) return@launch

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, uri)
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: uri.lastPathSegment
                    ?: "Unknown title"
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: "Unknown artist"
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull() ?: 0L

                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                songRepository.addSong(
                    Song(title = title, artist = artist, filePath = filePath, durationMs = durationMs)
                )
            } finally {
                retriever.release()
            }
        }
    }

    // Plays the whole current library list as a queue, starting at the tapped song.
    fun playSong(song: Song) {
        val currentList = songs.value
        val index = currentList.indexOf(song)
        if (index != -1) {
            audioPlayerManager.setQueue(currentList, index)
        }
    }

    // No longer releases the player here — it's a shared singleton now,
    // released only when the whole app process dies (see Application class, next step).
}