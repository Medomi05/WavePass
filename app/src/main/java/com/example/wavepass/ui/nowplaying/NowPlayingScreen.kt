package com.example.wavepass.ui.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.wavepass.playback.AudioPlayerManager
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Composable
fun NowPlayingScreen() {
    val context = LocalContext.current
    val audioPlayerManager = remember { AudioPlayerManager.getInstance(context) }

    val currentSong by audioPlayerManager.currentSong.collectAsState()
    val isPlaying by audioPlayerManager.isPlaying.collectAsState()
    val isShuffleEnabled by audioPlayerManager.isShuffleEnabled.collectAsState()
    val repeatMode by audioPlayerManager.repeatMode.collectAsState()
    val actualPositionMs by audioPlayerManager.currentPositionMs.collectAsState()
    val durationMs by audioPlayerManager.durationMs.collectAsState()

    var isDragging by remember { mutableStateOf(false) }
    var draggedPositionMs by remember { mutableStateOf(0f) }

    val displayedPositionMs = if (isDragging) draggedPositionMs.toLong() else actualPositionMs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            AlbumArt(albumArtPath = currentSong?.albumArtPath)
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (currentSong == null) {
            Text(
                "Nothing is playing yet",
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            Text(
                currentSong!!.title,
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                currentSong!!.artist,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        CustomProgressBar(
            positionMs = displayedPositionMs,
            durationMs = durationMs,
            onDrag = { newPositionMs ->
                isDragging = true
                draggedPositionMs = newPositionMs.toFloat()
            },
            onDragFinished = {
                audioPlayerManager.seekTo(draggedPositionMs.toLong())
                isDragging = false
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatMillis(displayedPositionMs),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                formatMillis(durationMs),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = { audioPlayerManager.toggleShuffle() }) {
                Icon(
                    imageVector = Icons.Filled.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffleEnabled)
                        MaterialTheme.colorScheme.primary
                    else
                        Color.Gray
                )
            }

            IconButton(onClick = { audioPlayerManager.previous() }) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = "Previous"
                )
            }

            IconButton(onClick = { audioPlayerManager.playPause() }) {
                Icon(
                    imageVector = if (isPlaying)
                        Icons.Filled.Pause
                    else
                        Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }

            IconButton(onClick = { audioPlayerManager.next() }) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = "Next"
                )
            }

            IconButton(onClick = { audioPlayerManager.cycleRepeatMode() }) {
                Icon(
                    imageVector = if (repeatMode == Player.REPEAT_MODE_ONE)
                        Icons.Filled.RepeatOne
                    else
                        Icons.Filled.Repeat,
                    contentDescription = "Repeat",
                    tint = if (repeatMode != Player.REPEAT_MODE_OFF)
                        MaterialTheme.colorScheme.primary
                    else
                        Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun formatMillis(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

@Composable
private fun CustomProgressBar(
    positionMs: Long,
    durationMs: Long,
    onDrag: (Long) -> Unit,
    onDragFinished: () -> Unit
) {
    var trackWidthPx by remember { mutableStateOf(0f) }

    val progress =
        if (durationMs > 0)
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        else
            0f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(horizontal = 4.dp)
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    if (trackWidthPx > 0 && durationMs > 0) {
                        val tappedFraction =
                            (offset.x / trackWidthPx).coerceIn(0f, 1f)

                        onDrag((tappedFraction * durationMs).toLong())
                        onDragFinished()
                    }
                }
            }
            .pointerInput(durationMs) {
                detectDragGestures(
                    onDragEnd = { onDragFinished() },
                    onDrag = { change, _ ->
                        if (trackWidthPx > 0 && durationMs > 0) {
                            val draggedFraction =
                                (change.position.x / trackWidthPx).coerceIn(0f, 1f)

                            onDrag((draggedFraction * durationMs).toLong())
                        }
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .onGloballyPositioned {
                    trackWidthPx = it.size.width.toFloat()
                }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )

        val thumbOffsetPx = (trackWidthPx * progress).roundToInt()

        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffsetPx - 28, 0) }
                .size(14.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun AlbumArt(albumArtPath: String?) {
    Box(
        modifier = Modifier
            .fillMaxHeight(0.9f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (albumArtPath != null) {
            AsyncImage(
                model = albumArtPath,
                contentDescription = "Album cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = "No cover art",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}