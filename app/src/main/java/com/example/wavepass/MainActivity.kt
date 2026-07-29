package com.example.wavepass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.wavepass.data.local.WavePassDatabase
import com.example.wavepass.data.repository.SongRepository
import com.example.wavepass.playback.AudioPlayerManager
import com.example.wavepass.ui.navigation.MainScaffold
import com.example.wavepass.ui.theme.WavePassTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Restore the last playback session (song + position) once, at app startup,
        // regardless of which tab the user lands on first.
        val database = WavePassDatabase.getInstance(applicationContext)
        val songRepository = SongRepository(database.songDao())
        val audioPlayerManager = AudioPlayerManager.getInstance(applicationContext)

        lifecycleScope.launch {
            val allSongs = songRepository.getAllSongs().first()
            audioPlayerManager.restoreLastSessionIfNeeded(allSongs)
        }

        setContent {
            WavePassTheme {
                MainScaffold()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        AudioPlayerManager.getInstance(applicationContext).saveStateNow()
    }
}