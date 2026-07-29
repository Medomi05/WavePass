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

                // Try to extract embedded cover art from the file itself.
                val embeddedArt = retriever.embeddedPicture
                val albumArtPath = embeddedArt?.let { saveAlbumArt(context, it) }

                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                songRepository.addSong(
                    Song(
                        title = title,
                        artist = artist,
                        filePath = filePath,
                        durationMs = durationMs,
                        albumArtPath = albumArtPath
                    )
                )
            } finally {
                retriever.release()
            }
        }
    }

    // Saves embedded album art bytes as a file in the app's private storage,
    // and returns the path to it. Returns null if writing fails.
    private fun saveAlbumArt(context: Context, imageBytes: ByteArray): String? {
        return try {
            val fileName = "album_art_${System.currentTimeMillis()}.jpg"
            val file = java.io.File(context.filesDir, fileName)
            file.writeBytes(imageBytes)
            file.absolutePath
        } catch (e: Exception) {
            null
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

    init {
        viewModelScope.launch {
            songs.collect { list ->
                if (list.isNotEmpty()) {
                    audioPlayerManager.restoreLastSessionIfNeeded(list)
                }
            }
        }
    }
}