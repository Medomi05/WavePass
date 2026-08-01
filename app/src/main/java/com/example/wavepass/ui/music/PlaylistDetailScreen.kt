package com.example.wavepass.ui.music

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.wavepass.data.local.Song
import com.example.wavepass.playback.AudioPlayerManager

@Composable
fun PlaylistDetailScreen(
    playlistViewModel: PlaylistViewModel,
    allSongs: List<Song>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val audioPlayerManager = remember { AudioPlayerManager.getInstance(context) }

    val playlistWithSongs by playlistViewModel.selectedPlaylistWithSongs.collectAsState()
    val playlistId = playlistWithSongs?.playlist?.id

    var orderedSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    LaunchedEffect(playlistWithSongs?.songs) {
        orderedSongs = playlistWithSongs?.songs ?: emptyList()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = playlistWithSongs?.playlist?.name ?: "",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Button(
            onClick = {
                if (orderedSongs.isNotEmpty()) {
                    audioPlayerManager.setQueue(orderedSongs, startIndex = 0)
                }
            },
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Text(" Play playlist")
        }

        if (orderedSongs.isEmpty()) {
            Text(
                "This playlist is empty. Add songs from the Songs tab.",
                modifier = Modifier.padding(16.dp)
            )
        } else {
            ReorderableSongList(
                songs = orderedSongs,
                onOrderChanged = { newOrder ->
                    orderedSongs = newOrder
                    val id = playlistId ?: return@ReorderableSongList
                    playlistViewModel.reorderSongs(id, newOrder.map { it.id })
                },
                onRemove = { song ->
                    val id = playlistId ?: return@ReorderableSongList
                    playlistViewModel.removeSongFromPlaylist(id, song.id)
                },
                onPlay = { song ->
                    val index = orderedSongs.indexOf(song)
                    if (index != -1) audioPlayerManager.setQueue(orderedSongs, index)
                }
            )
        }
    }
}

@Composable
private fun ReorderableSongList(
    songs: List<Song>,
    onOrderChanged: (List<Song>) -> Unit,
    onRemove: (Song) -> Unit,
    onPlay: (Song) -> Unit
) {
    // Always holds the latest values, so the long-running drag gesture coroutine
    // (which is NOT restarted on every reorder, see pointerInput key below)
    // never operates on stale, captured-at-launch data.
    val latestSongs by rememberUpdatedState(songs)
    val latestOnOrderChanged by rememberUpdatedState(onOrderChanged)

    var rowHeightPx by remember { mutableFloatStateOf(0f) }

    // Identity (not index!) of the song currently being dragged, and how far
    // it has been dragged in px. Using the song's own id (not its position)
    // means this stays correct even as the list reorders mid-drag.
    var draggedSongId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    Column(modifier = Modifier.fillMaxSize()) {
        songs.forEachIndexed { index, song ->
            // key() tells Compose to track this composable by the song's identity,
            // not by its slot/position in the list. Without this, reordering the
            // list can make Compose reuse a row's remembered state for the wrong song.
            key(song.id) {
                val isBeingDragged = song.id == draggedSongId

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            if (rowHeightPx == 0f) rowHeightPx = coordinates.size.height.toFloat()
                        }
                        .graphicsLayer {
                            translationY = if (isBeingDragged) dragOffsetPx else 0f
                        }
                        .zIndex(if (isBeingDragged) 1f else 0f)
                        .background(
                            if (isBeingDragged) MaterialTheme.colorScheme.surfaceVariant
                            else MaterialTheme.colorScheme.background
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                // Keyed on the song's id, which never changes for this row
                                // across reorders — so this gesture detector coroutine
                                // keeps running uninterrupted for the whole drag session,
                                // no matter how many times the list order changes.
                                .pointerInput(song.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedSongId = song.id
                                            dragOffsetPx = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetPx += dragAmount.y

                                            if (rowHeightPx > 0f) {
                                                val currentSongs = latestSongs
                                                val currentDraggedId = draggedSongId ?: return@detectDragGesturesAfterLongPress
                                                val currentIndex = currentSongs.indexOfFirst { it.id == currentDraggedId }
                                                if (currentIndex == -1) return@detectDragGesturesAfterLongPress

                                                val positionsMoved = (dragOffsetPx / rowHeightPx).let {
                                                    if (it >= 0) kotlin.math.floor(it).toInt()
                                                    else kotlin.math.ceil(it).toInt()
                                                }

                                                val targetIndex = (currentIndex + positionsMoved)
                                                    .coerceIn(0, currentSongs.lastIndex)

                                                if (targetIndex != currentIndex) {
                                                    val mutable = currentSongs.toMutableList()
                                                    val moved = mutable.removeAt(currentIndex)
                                                    mutable.add(targetIndex, moved)

                                                    // Adjust offset so the item doesn't visually "jump"
                                                    // now that its index reference point has changed.
                                                    dragOffsetPx -= (targetIndex - currentIndex) * rowHeightPx

                                                    latestOnOrderChanged(mutable)
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            draggedSongId = null
                                            dragOffsetPx = 0f
                                        },
                                        onDragCancel = {
                                            draggedSongId = null
                                            dragOffsetPx = 0f
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.DragHandle, contentDescription = "Drag to reorder")
                        }

                        Column(
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(song.title, style = MaterialTheme.typography.bodyLarge)
                            Text(song.artist, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Row {
                        IconButton(onClick = { onPlay(song) }) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
                        }
                        IconButton(onClick = { onRemove(song) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove from playlist")
                        }
                    }
                }
            }
        }
    }
}