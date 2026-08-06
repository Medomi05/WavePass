package com.example.wavepass.ui.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private const val MAX_FAVORITE_SONGS = 5

@Composable
fun ProfileScreen() {
    val viewModel: ProfileViewModel = viewModel()

    val songs by viewModel.songs.collectAsState()
    val profile by viewModel.profile.collectAsState()
    val computedArtist by viewModel.computedFavoriteArtist.collectAsState()

    var alias by remember { mutableStateOf("") }
    var selectedTitles by remember { mutableStateOf(setOf<String>()) }

    // Pre-fill the form once, when the existing profile first loads.
    LaunchedEffect(profile) {
        profile?.let {
            alias = it.displayAlias
            selectedTitles = it.favoriteSongTitles.toSet()
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Text("Your WavePass Profile", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = alias,
            onValueChange = { alias = it },
            label = { Text("Display alias") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("Favorite artist (automatic)", style = MaterialTheme.typography.labelMedium)
        Text(computedArtist, style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "Pick up to $MAX_FAVORITE_SONGS favorite songs (${selectedTitles.size}/$MAX_FAVORITE_SONGS)",
            style = MaterialTheme.typography.labelMedium
        )

        if (songs.isEmpty()) {
            Text("Import some songs first to build your profile.")
        } else {
            LazyColumn(modifier = Modifier.weight(1f, fill = false).fillMaxWidth()) {
                items(songs, key = { it.id }) { song ->
                    val isSelected = song.title in selectedTitles
                    val canSelectMore = selectedTitles.size < MAX_FAVORITE_SONGS

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            enabled = isSelected || canSelectMore,
                            onCheckedChange = { checked ->
                                selectedTitles = if (checked) {
                                    selectedTitles + song.title
                                } else {
                                    selectedTitles - song.title
                                }
                            }
                        )
                        Text("${song.title} — ${song.artist}")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.saveProfile(alias, selectedTitles.toList()) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save profile")
        }
    }
}