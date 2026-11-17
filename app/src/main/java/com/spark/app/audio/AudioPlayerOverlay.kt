package com.spark.app.audio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val PlayerPage = Color(0xFF121110)
private val PlayerSurface = Color(0xFF1A1817)
private val PlayerRaised = Color(0xFF242220)
private val PlayerInk = Color(0xFFECE6E0)
private val PlayerMuted = Color(0xFFCFC7BE)
private val PlayerLine = Color(0xFF49433D)
private val PlayerAccent = Color(0xFFFFB454)

@Composable
fun MiniAudioPlayer(
    player: AudioPlayerBox,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by player.state.collectAsStateWithLifecycle()
    val title = state.title ?: "Audio chapter"
    val duration = state.durationMs
    val progress = if (duration > 0) {
        (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
        label = "mini audio progress"
    )

    AnimatedVisibility(
        visible = state.url != null,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            color = PlayerSurface,
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PlayerLine),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onOpen)
                            .padding(vertical = 5.dp)
                            .semantics { contentDescription = "Open audio player: $title" }
                    ) {
                        Text(
                            text = if (state.buffering) "CONNECTING TO AUDIO" else "NOW PLAYING",
                            color = PlayerAccent,
                            fontSize = 10.sp,
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = title,
                            color = PlayerInk,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    FilledIconButton(
                        onClick = {
                            if (state.failed) {
                                state.url?.let { player.playUrl(it, title) }
                            } else {
                                player.toggle()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .semantics {
                                contentDescription = when {
                                    state.failed -> "Retry audio"
                                    state.playing -> "Pause audio"
                                    else -> "Resume audio"
                                }
                            }
                    ) {
                        PlaybackGlyph(playing = state.playing && !state.failed)
                    }
                }
                if (state.buffering) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = PlayerAccent,
                        trackColor = PlayerRaised
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(PlayerRaised)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .height(2.dp)
                                .background(PlayerAccent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioPlayerOverlay(
    player: AudioPlayerBox,
    onDismiss: () -> Unit
) {
    val state by player.state.collectAsStateWithLifecycle()
    val title = state.title ?: "Audio chapter"
    val duration = state.durationMs
    var scrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }
    val animatedPosition by animateFloatAsState(
        targetValue = state.positionMs.toFloat(),
        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
        label = "audio position"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(180)) +
                scaleIn(initialScale = 0.985f, animationSpec = tween(180)),
            modifier = Modifier.fillMaxSize()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PlayerPage)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                color = PlayerPage
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FilledIconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Minimize full audio player" }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = "LISTENING",
                        color = PlayerMuted,
                        fontSize = 11.sp,
                        letterSpacing = 1.8.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = {
                        player.stopAndClear()
                        onDismiss()
                    }) {
                        Text("Stop", color = PlayerMuted)
                    }
                }

                Spacer(Modifier.height(48.dp))
                Text(
                    text = title,
                    color = PlayerInk,
                    fontSize = 25.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { heading() }
                )
                Text(
                    text = "${if (state.buffering) "Connecting" else "Streaming"} · screen-off playback",
                    color = PlayerMuted,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )

                Spacer(Modifier.height(24.dp))
                if (duration > 0) {
                    Slider(
                        value = (if (scrubbing) scrubPosition else animatedPosition)
                            .coerceIn(0f, duration.toFloat()),
                        onValueChange = {
                            scrubbing = true
                            scrubPosition = it
                        },
                        onValueChangeFinished = {
                            player.seekTo(scrubPosition.toLong())
                            scrubbing = false
                        },
                        valueRange = 0f..duration.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = "Audio position" }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatMinSec(if (scrubbing) scrubPosition.toLong() else state.positionMs), color = PlayerMuted, fontSize = 12.sp)
                        Text(formatMinSec(duration), color = PlayerMuted, fontSize = 12.sp)
                    }
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        color = PlayerAccent
                    )
                }

                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { player.seekBy(-10_000L) },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Text("−10", color = PlayerInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(22.dp))
                    FilledIconButton(
                        onClick = {
                            if (state.failed) state.url?.let { player.playUrl(it, title) }
                            else player.toggle()
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .semantics {
                                contentDescription = when {
                                    state.failed -> "Retry audio"
                                    state.playing -> "Pause audio"
                                    else -> "Resume audio"
                                }
                            }
                    ) {
                        PlaybackGlyph(
                            playing = state.playing && !state.failed,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(Modifier.width(22.dp))
                    TextButton(
                        onClick = { player.seekBy(30_000L) },
                        modifier = Modifier.size(64.dp)
                    ) {
                        Text("+30", color = PlayerInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                if (state.failed) {
                    Text(
                        text = "Audio could not connect. Check your network and try again.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SpeedControl(
                        selectedSpeed = state.playbackSpeed,
                        onSelectSpeed = player::setPlaybackSpeed,
                        modifier = Modifier.weight(1f)
                    )
                    SleepTimerControl(
                        remainingMs = state.sleepTimerRemainingMs,
                        onSetTimer = player::setSleepTimer,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(28.dp))
            }
            }
        }
    }
}

@Composable
private fun PlaybackGlyph(playing: Boolean, modifier: Modifier = Modifier) {
    if (playing) {
        Text("Ⅱ", color = PlayerInk, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = modifier)
    } else {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = modifier)
    }
}

@Composable
private fun SpeedControl(
    selectedSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PlayerSurface)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SPEED", color = PlayerMuted, fontSize = 10.sp, letterSpacing = 1.sp)
                Text("${selectedSpeed}×", color = PlayerInk, fontWeight = FontWeight.SemiBold)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { speed ->
                DropdownMenuItem(
                    text = { Text("${speed}×") },
                    onClick = {
                        onSelectSpeed(speed)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SleepTimerControl(
    remainingMs: Long,
    onSetTimer: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (remainingMs > 0) formatMinSec(remainingMs) else "Off"
    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(PlayerSurface)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SLEEP TIMER", color = PlayerMuted, fontSize = 10.sp, letterSpacing = 1.sp)
                Text(label, color = PlayerInk, fontWeight = FontWeight.SemiBold)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(null to "Off", 5 to "5 minutes", 10 to "10 minutes", 15 to "15 minutes", 30 to "30 minutes", 45 to "45 minutes", 60 to "60 minutes").forEach { (minutes, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onSetTimer(minutes)
                        expanded = false
                    }
                )
            }
        }
    }
}
