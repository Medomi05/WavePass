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

    // Called when the user picks a file via the system file picker.
    // Reads basic metadata from the audio file and saves it into Room.
    fun importSong(context: Context, uri: Uri) {
        viewModelScope.launch {
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

                // Persist read/write permission for this file across app restarts.
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                songRepository.addSong(
                    Song(
                        title = title,
                        artist = artist,
                        filePath = uri.toString(),
                        durationMs = durationMs
                    )
                )
            } finally {
                retriever.release()
            }
        }
    }

    fun playSong(song: Song) {
        audioPlayerManager.play(song.filePath)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayerManager.release()
    }
}