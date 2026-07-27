package com.example.wavepass.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

// Wraps a single ExoPlayer instance for the whole app.
// Keeping one shared instance avoids conflicts between screens
// trying to play audio at the same time.
class AudioPlayerManager(context: Context) {

    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    fun play(filePath: String) {
        val mediaItem = MediaItem.fromUri(filePath)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun release() {
        player.release()
    }
}