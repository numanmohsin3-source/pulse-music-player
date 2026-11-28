package com.pulsemusic.player.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulsemusic.player.data.AudioFolder
import com.pulsemusic.player.data.AudioTrack
import com.pulsemusic.player.ui.theme.PulseCyan
import com.pulsemusic.player.ui.theme.PulsePink
import com.pulsemusic.player.ui.theme.PulseSurface
import com.pulsemusic.player.ui.theme.PulseSurfaceAlt
import kotlinx.coroutines.launch
import kotlin.math.sin

enum class TrackSortOrder(val label: String) {
    NAME_AZ("Name (A → Z)"),
    NAME_ZA("Name (Z → A)"),
    DATE_NEWEST("Recently Added (Newest)"),
    DATE_OLDEST("Date Added (Oldest)"),
    DURATION_DESC("Duration (Longest)"),
    DURATION_ASC("Duration (Shortest)"),
    ARTIST_AZ("Artist (A → Z)")
}

enum class FolderSortOrder(val label: String) {
    NAME_AZ("Folder Name (A → Z)"),
    NAME_ZA("Folder Name (Z → A)"),
    SONGS_DESC("Most Songs First"),
    SONGS_ASC("Least Songs First")
}

fun List<AudioFolder>.sortedByFolderOrder(order: FolderSortOrder): List<AudioFolder> {
    return when (order) {
        FolderSortOrder.NAME_AZ -> sortedBy { it.folderName.lowercase() }
        FolderSortOrder.NAME_ZA -> sortedByDescending { it.folderName.lowercase() }
        FolderSortOrder.SONGS_DESC -> sortedByDescending { it.songCount }
        FolderSortOrder.SONGS_ASC -> sortedBy { it.songCount }
    }
}

fun List<AudioTrack>.sortedByOrder(order: TrackSortOrder): List<AudioTrack> {
    return when (order) {
        TrackSortOrder.NAME_AZ -> sortedBy { it.title.lowercase() }
        TrackSortOrder.NAME_ZA -> sortedByDescending { it.title.lowercase() }
        TrackSortOrder.DATE_NEWEST -> sortedByDescending { it.dateAddedSec }
        TrackSortOrder.DATE_OLDEST -> sortedBy { it.dateAddedSec }
        TrackSortOrder.DURATION_DESC -> sortedByDescending { it.durationMs }
        TrackSortOrder.DURATION_ASC -> sortedBy { it.durationMs }
        TrackSortOrder.ARTIST_AZ -> sortedBy { it.artist.lowercase() }
    }
}

/**
 * Vertical A-Z indexer (# to Z) on the right edge of track lists.
 * Tapping or dragging on any letter scrolls the list smoothly to that section.
 */
@Composable
fun AlphabetIndexerBar(
    tracks: List<AudioTrack>,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val alphabet = remember { listOf("#") + ('A'..'Z').map { it.toString() } }

    val letterIndices = remember(tracks) {
        val map = mutableMapOf<String, Int>()
        tracks.forEachIndexed { index, track ->
            val first = track.title.trim().firstOrNull()?.uppercaseChar() ?: '#'
            val key = if (first in 'A'..'Z') first.toString() else "#"
            if (!map.containsKey(key)) {
                map[key] = index
            }
        }
        map
    }

    var selectedLetter by remember { mutableStateOf<String?>(null) }

    fun jumpToLetter(letter: String) {
        selectedLetter = letter
        val targetIdx = letterIndices[letter]
        if (targetIdx != null) {
            coroutineScope.launch {
                listState.animateScrollToItem(targetIdx)
            }
        } else {
            // Find closest next letter
            val nextKey = alphabet.dropWhile { it != letter }.firstOrNull { letterIndices.containsKey(it) }
            if (nextKey != null && letterIndices[nextKey] != null) {
                coroutineScope.launch {
                    listState.animateScrollToItem(letterIndices[nextKey]!!)
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxHeight(), contentAlignment = Alignment.CenterEnd) {
        Column(
            modifier = Modifier
                .padding(end = 2.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x990F172A))
                .padding(vertical = 4.dp, horizontal = 2.dp)
                .pointerInput(tracks) {
                    detectTapGestures { offset ->
                        val itemHeight = size.height.toFloat() / alphabet.size
                        val index = (offset.y / itemHeight).toInt().coerceIn(0, alphabet.lastIndex)
                        jumpToLetter(alphabet[index])
                    }
                }
                .pointerInput(tracks) {
                    detectDragGestures(
                        onDragEnd = { selectedLetter = null },
                        onDragCancel = { selectedLetter = null }
                    ) { change, _ ->
                        val itemHeight = size.height.toFloat() / alphabet.size
                        val index = (change.position.y / itemHeight).toInt().coerceIn(0, alphabet.lastIndex)
                        jumpToLetter(alphabet[index])
                    }
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            alphabet.forEach { letter ->
                val hasSongs = letterIndices.containsKey(letter)
                Text(
                    text = letter,
                    fontSize = 8.5.sp,
                    fontWeight = if (hasSongs) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        selectedLetter == letter -> PulsePink
                        hasSongs -> PulseCyan
                        else -> Color(0xFF475569)
                    },
                    modifier = Modifier.padding(vertical = 0.5.dp)
                )
            }
        }

        // Floating letter bubble feedback
        if (selectedLetter != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(end = 40.dp)
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(PulsePink, Color(0xFF1E293B))))
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedLetter!!,
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/**
 * Sort selector dialog with modern option rows.
 */
@Composable
fun SortSelectionDialog(
    currentOrder: TrackSortOrder,
    onSelectOrder: (TrackSortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Sort, contentDescription = null, tint = PulseCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sort Songs By", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TrackSortOrder.values().forEach { order ->
                    val isSelected = order == currentOrder
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PulseSurfaceAlt else Color.Transparent)
                            .clickable {
                                onSelectOrder(order)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = order.label,
                            color = if (isSelected) PulseCyan else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PulseCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PulsePink)
            }
        },
        containerColor = Color(0xFF0F172A)
    )
}

/**
 * Sort selector dialog for folders.
 */
@Composable
fun FolderSortSelectionDialog(
    currentOrder: FolderSortOrder,
    onSelectOrder: (FolderSortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Sort, contentDescription = null, tint = PulseCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sort Folders By", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FolderSortOrder.values().forEach { order ->
                    val isSelected = order == currentOrder
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PulseSurfaceAlt else Color.Transparent)
                            .clickable {
                                onSelectOrder(order)
                                onDismiss()
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = order.label,
                            color = if (isSelected) PulseCyan else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = PulseCyan, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = PulsePink)
            }
        },
        containerColor = Color(0xFF0F172A)
    )
}

/**
 * Premium animated equalizer spectrum wave: 18 gradient bars dancing to rhythm when playing.
 */
@Composable
fun AnimatedEqualizerWave(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_bars")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    val barCount = 18
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        for (i in 0 until barCount) {
            val baseHeight = if (isPlaying) {
                val wave = (sin(animPhase + (i * 0.45f)) + 1f) / 2f
                val wave2 = (sin(animPhase * 1.8f - (i * 0.35f)) + 1f) / 2f
                ((wave * 0.6f + wave2 * 0.4f) * 24f + 4f).dp
            } else {
                3.dp
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(baseHeight)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                PulsePink,
                                PulseCyan
                            )
                        )
                    )
            )
        }
    }
}

/**
 * 4-bar mini equalizer indicator for currently playing track.
 */
@Composable
fun MiniEqualizerIndicator(
    isPlaying: Boolean,
    tint: Color = PulsePink,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mini_eq")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mini_phase"
    )

    Row(
        modifier = modifier.size(width = 18.dp, height = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val multipliers = listOf(0.3f, 0.7f, 0.5f, 0.9f)
        for (i in 0..3) {
            val h = if (isPlaying) {
                val v = (sin(phase + i * 1.2f) + 1f) / 2f
                (v * 10f * multipliers[i] + 3f).dp
            } else {
                3.dp
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(h)
                    .clip(RoundedCornerShape(1.dp))
                    .background(tint)
            )
        }
    }
}
