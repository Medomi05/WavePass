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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

    // Local, mutable copy of the song order, so dragging feels instant
    // instead of waiting for a round-trip to the database on every move.
    var orderedSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    // Re-sync local order whenever the underlying data changes for real
    // (e.g. a song was added/removed), but don't fight the user mid-drag.
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
    // Height of a single row, measured at runtime, used to figure out
    // how many positions a drag has crossed.
    var rowHeightPx by remember { mutableFloatStateOf(0f) }

    // Index of the item currently being dragged, and how far it has been dragged (in px).
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }

    Column(modifier = Modifier.fillMaxSize()) {
        songs.forEachIndexed { index, song ->
            val isBeingDragged = index == draggedIndex

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
                            .pointerInput(songs) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedIndex = index
                                        dragOffsetPx = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetPx += dragAmount.y

                                        if (rowHeightPx > 0f) {
                                            val currentDragged = draggedIndex
                                            if (currentDragged == -1) return@detectDragGesturesAfterLongPress

                                            val positionsMoved = (dragOffsetPx / rowHeightPx).let {
                                                if (it >= 0) kotlin.math.floor(it).toInt()
                                                else kotlin.math.ceil(it).toInt()
                                            }

                                            val targetIndex = (currentDragged + positionsMoved)
                                                .coerceIn(0, songs.lastIndex)

                                            if (targetIndex != currentDragged) {
                                                val mutable = songs.toMutableList()
                                                val moved = mutable.removeAt(currentDragged)
                                                mutable.add(targetIndex, moved)

                                                // Adjust offset so the item doesn't visually "jump"
                                                // now that its index reference point has changed.
                                                dragOffsetPx -= (targetIndex - currentDragged) * rowHeightPx
                                                draggedIndex = targetIndex

                                                onOrderChanged(mutable)
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        draggedIndex = -1
                                        dragOffsetPx = 0f
                                    },
                                    onDragCancel = {
                                        draggedIndex = -1
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