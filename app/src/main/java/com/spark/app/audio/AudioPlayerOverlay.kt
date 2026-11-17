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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
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
import com.spark.app.R

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
        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
        label = "mini audio progress"
    )
    val haptics = LocalHapticFeedback.current

    AnimatedVisibility(
        visible = state.url != null,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF13221A),
            border = BorderStroke(1.dp, Color(0xFF233B2E)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable { onOpen() }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Album / Audio Glyph
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A3024))
                            .border(1.dp, Color(0xFF2C4C3A), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_headphones_16),
                            contentDescription = null,
                            tint = if (state.playing) PlayerAccent else PlayerMuted,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(Modifier.width(11.dp))

                    // Title & Status
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (state.buffering) PlayerAccent
                                        else if (state.playing) Color(0xFF4EE49B)
                                        else PlayerMuted
                                    )
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = when {
                                    state.buffering -> "CONNECTING"
                                    state.playing -> "PLAYING"
                                    else -> "PAUSED"
                                },
                                color = PlayerAccent,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            )
                            if (state.autoplay) {
                                Text(
                                    text = " · AUTOPLAY",
                                    color = Color(0xFF8FA396),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = title,
                            color = Color(0xFFF7F2EC),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Play / Pause Action Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PlayerAccent)
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                if (state.failed) {
                                    state.url?.let { player.playUrl(it, title) }
                                } else {
                                    player.toggle()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.playing && !state.failed) {
                            Text(
                                text = "Ⅱ",
                                color = Color(0xFF141210),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play",
                                tint = Color(0xFF141210),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Sleek integrated progress bar at the bottom edge
                if (state.buffering) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = PlayerAccent,
                        trackColor = Color(0xFF1B2B22)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(Color(0xFF1B2B22))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .height(2.5.dp)
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

                Spacer(Modifier.height(14.dp))

                // Autoplay Next Chapter Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PlayerRaised,
                    border = BorderStroke(1.dp, PlayerLine.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { player.toggleAutoplay() }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (state.autoplay) PlayerAccent.copy(alpha = 0.18f) else PlayerPage),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_headphones_16),
                                    contentDescription = null,
                                    tint = if (state.autoplay) PlayerAccent else PlayerMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Autoplay Chapters",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (state.autoplay) PlayerInk else PlayerMuted
                                )
                                Text(
                                    text = if (state.autoplay) "Plays chapters one after another" else "Stops when current chapter ends",
                                    fontSize = 11.sp,
                                    color = PlayerMuted
                                )
                            }
                        }
                        Switch(
                            checked = state.autoplay,
                            onCheckedChange = { player.setAutoplay(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF141210),
                                checkedTrackColor = PlayerAccent,
                                uncheckedThumbColor = PlayerMuted,
                                uncheckedTrackColor = PlayerSurface
                            )
                        )
                    }
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
