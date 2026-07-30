package com.example.wavepass.ui.music

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.wavepass.data.local.Playlist
import com.example.wavepass.data.local.Song
import com.example.wavepass.data.local.WavePassDatabase
import com.example.wavepass.data.repository.PlaylistRepository
import com.example.wavepass.data.repository.SongRepository
import com.example.wavepass.playback.AudioPlayerManager

private val tabTitles = listOf("Songs", "Playlists")

@Composable
fun MusicScreen() {
    val context = LocalContext.current
    val database = WavePassDatabase.getInstance(context)
    val songRepository = remember { SongRepository(database.songDao()) }
    val playlistRepository = remember { PlaylistRepository(database.playlistDao()) }
    val audioPlayerManager = remember { AudioPlayerManager.getInstance(context) }

    val musicViewModel: MusicViewModel = viewModel(
        factory = MusicViewModelFactory(songRepository, audioPlayerManager)
    )
    val playlistViewModel: PlaylistViewModel = viewModel(
        factory = PlaylistViewModelFactory(playlistRepository)
    )

    var selectedTab by remember { mutableIntStateOf(0) }
    val openPlaylistId by playlistViewModel.selectedPlaylistId.collectAsState()

    // If a playlist is open, show its detail screen instead of the tabs.
    if (openPlaylistId != null) {
        PlaylistDetailScreen(
            playlistViewModel = playlistViewModel,
            allSongs = musicViewModel.songs.collectAsState().value,
            onBack = { playlistViewModel.closePlaylist() }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> SongsTab(musicViewModel, playlistViewModel)
            1 -> PlaylistsTab(playlistViewModel, onOpenPlaylist = { playlistViewModel.openPlaylist(it) })
        }
    }
}

@Composable
private fun SongsTab(musicViewModel: MusicViewModel, playlistViewModel: PlaylistViewModel) {
    val context = LocalContext.current
    val songs by musicViewModel.songs.collectAsState()
    val playlists by playlistViewModel.playlists.collectAsState()

    var songToAddToPlaylist by remember { mutableStateOf<Song?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { musicViewModel.importSong(context, it) } }

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
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${song.title} — ${song.artist}")
                        Row {
                            Button(onClick = { songToAddToPlaylist = song }) {
                                Text("Add to playlist")
                            }
                            Button(onClick = { musicViewModel.playSong(song) }) {
                                Text("Play")
                            }
                        }
                    }
                }
            }
        }
    }

    songToAddToPlaylist?.let { song ->
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { songToAddToPlaylist = null },
            onSelect = { playlist ->
                playlistViewModel.addSongToPlaylist(playlist.id, song)
                songToAddToPlaylist = null
            }
        )
    }
}

@Composable
private fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelect: (Playlist) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to playlist") },
        text = {
            if (playlists.isEmpty()) {
                Text("You don't have any playlists yet.")
            } else {
                Column {
                    playlists.forEach { playlist ->
                        Text(
                            text = playlist.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { onSelect(playlist) }
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}