package com.example.wavepass.ui.music

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wavepass.data.local.WavePassDatabase
import com.example.wavepass.data.repository.SongRepository
import com.example.wavepass.playback.AudioPlayerManager

@Composable
fun MusicScreen() {
    val context = LocalContext.current
    val database = WavePassDatabase.getInstance(context)
    val repository = remember { SongRepository(database.songDao()) }
    val audioPlayerManager = remember { AudioPlayerManager(context) }
    val viewModel: MusicViewModel = viewModel(
        factory = MusicViewModelFactory(repository, audioPlayerManager)
    )

    val songs by viewModel.songs.collectAsState()

    // Opens the system file picker, filtered to audio files.
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importSong(context, it) }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = { filePickerLauncher.launch(arrayOf("audio/*")) }) {
            Text("Import song")
        }

        if (songs.isEmpty()) {
            Text("No songs yet. Import one from your device.")
        } else {
            LazyColumn {
                items(songs) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${song.title} — ${song.artist}")
                        Button(onClick = { viewModel.playSong(song) }) {
                            Text("Play")
                        }
                    }
                }
            }
        }
    }
}