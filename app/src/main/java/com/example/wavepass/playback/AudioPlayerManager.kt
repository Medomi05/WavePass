package com.example.wavepass.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.wavepass.data.local.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioPlayerManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    val player: ExoPlayer = ExoPlayer.Builder(appContext).build()

    private val prefs = appContext.getSharedPreferences("wavepass_playback_state", Context.MODE_PRIVATE)

    private val managerScope = CoroutineScope(Dispatchers.Main)
    private var positionUpdateJob: Job? = null
    private var ticksSinceLastSave = 0

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private var queueSongs: List<Song> = emptyList()
    private var hasRestoredSession = false

    private var pendingRestorePositionMs: Long? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startPositionUpdates() else {
                    stopPositionUpdates()
                    savePlaybackState()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = player.currentMediaItemIndex
                _currentSong.value = queueSongs.getOrNull(index)

                if (pendingRestorePositionMs == null) {
                    _currentPositionMs.value = 0L
                }

                savePlaybackState()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _durationMs.value = player.duration.coerceAtLeast(0L)

                    pendingRestorePositionMs?.let { restorePosition ->
                        player.seekTo(restorePosition)
                        _currentPositionMs.value = restorePosition
                        pendingRestorePositionMs = null
                    }
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _isShuffleEnabled.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }
        })
    }

    private fun startPositionUpdates() {
        stopPositionUpdates()
        positionUpdateJob = managerScope.launch {
            while (true) {
                _currentPositionMs.value = player.currentPosition.coerceAtLeast(0L)
                _durationMs.value = player.duration.coerceAtLeast(0L)

                // Persist roughly every 5 seconds (10 ticks * 500ms) instead of every tick,
                // to avoid hammering disk writes.
                ticksSinceLastSave++
                if (ticksSinceLastSave >= 10) {
                    savePlaybackState()
                    ticksSinceLastSave = 0
                }

                delay(500)
            }
        }
    }

    private fun stopPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    private fun savePlaybackState() {
        val song = _currentSong.value ?: return
        prefs.edit()
            .putLong(KEY_LAST_SONG_ID, song.id)
            .putLong(KEY_LAST_POSITION_MS, player.currentPosition)
            .apply()
    }

    // Call once, when the library has loaded, to resume the last session (paused, not auto-playing).
    // Returns true if a session was restored.
    fun restoreLastSessionIfNeeded(allSongs: List<Song>): Boolean {
        if (hasRestoredSession || allSongs.isEmpty()) return false
        hasRestoredSession = true

        val lastSongId = prefs.getLong(KEY_LAST_SONG_ID, -1L)
        if (lastSongId == -1L) return false

        val index = allSongs.indexOfFirst { it.id == lastSongId }
        if (index == -1) return false

        val lastPositionMs = prefs.getLong(KEY_LAST_POSITION_MS, 0L)
        pendingRestorePositionMs = lastPositionMs
        setQueue(allSongs, index, autoPlay = false)
        return true
    }

    fun setQueue(songs: List<Song>, startIndex: Int, autoPlay: Boolean = true) {
        queueSongs = songs
        val mediaItems = songs.map { MediaItem.fromUri(it.filePath) }
        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        if (autoPlay) player.play()
        _currentSong.value = songs.getOrNull(startIndex)
    }

    fun playPause() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun next() {
        if (player.hasNextMediaItem()) player.seekToNext()
    }

    fun previous() {
        val currentPosition = player.currentPosition
        if (currentPosition > 5000L) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPrevious()
        } else {
            player.seekTo(0L)
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun toggleShuffle() {
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        player.repeatMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun release() {
        savePlaybackState()
        stopPositionUpdates()
        player.release()
    }

    companion object {
        private const val KEY_LAST_SONG_ID = "last_song_id"
        private const val KEY_LAST_POSITION_MS = "last_position_ms"

        @Volatile
        private var INSTANCE: AudioPlayerManager? = null

        fun getInstance(context: Context): AudioPlayerManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioPlayerManager(context).also { INSTANCE = it }
            }
        }
    }

    fun saveStateNow() {
        savePlaybackState()
    }
}