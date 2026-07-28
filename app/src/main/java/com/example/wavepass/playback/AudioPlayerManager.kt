package com.example.wavepass.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.wavepass.data.local.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Wraps a single shared ExoPlayer instance for the whole app.
// Singleton pattern: every screen (Library, Now Playing) must reference
// the same player, otherwise each screen would control a different playback session.
class AudioPlayerManager private constructor(context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context.applicationContext).build()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private var queueSongs: List<Song> = emptyList()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player.currentMediaItemIndex
                _currentSong.value = queueSongs.getOrNull(index)
            }
        })
    }

    // Loads a full queue (e.g. a playlist or the whole library) and starts playing at startIndex.
    fun setQueue(songs: List<Song>, startIndex: Int) {
        queueSongs = songs
        val mediaItems = songs.map { MediaItem.fromUri(it.filePath) }
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.play()
        _currentSong.value = songs.getOrNull(startIndex)
    }

    fun playPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() {
        if (player.hasNextMediaItem()) player.seekToNext()
    }

    fun previous() {
        if (player.hasPreviousMediaItem()) player.seekToPrevious()
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun isShuffleEnabled(): Boolean = player.shuffleModeEnabled

    // Cycles: off -> repeat all -> repeat one -> off
    fun cycleRepeatMode() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun getRepeatMode(): Int = player.repeatMode

    fun release() {
        player.release()
    }

    companion object {
        @Volatile
        private var INSTANCE: AudioPlayerManager? = null

        fun getInstance(context: Context): AudioPlayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioPlayerManager(context).also { INSTANCE = it }
            }
        }
    }
}