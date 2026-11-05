package com.spark.app.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spark.app.R
import com.spark.app.audio.AudioPlayerBox
import com.spark.app.audio.AudioUi
import com.spark.app.audio.TtsHelper
import com.spark.app.audio.formatMinSec
import com.spark.app.data.Card
import com.spark.app.data.CardType
import com.spark.app.sync.InteractionSync
import com.spark.app.sync.StateEvents

@Composable
fun CardView(
    card: Card,
    counter: String,
    tts: TtsHelper,
    state: FeedState,
    sync: InteractionSync,
    player: AudioPlayerBox?,
    showFinish: Boolean,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showWhy by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "[${card.type.name}·${card.topic}]",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = counter, style = MaterialTheme.typography.labelLarge)
            }
            if (!card.grounded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "AI-written, unverified",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            key(card.id) {
                when (card.type) {
                    CardType.QUOTE -> QuoteBody(card)
                    CardType.QUIZ -> QuizBody(
                        card = card,
                        onAnswered = { correct -> sync.record(StateEvents.quiz(card.id, correct)) }
                    )
                    CardType.AUDIO -> AudioBody(card, player)
                    else -> StandardBody(card)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (card.actionLine.isNotBlank()) {
                Text(
                    text = "▸ ${card.actionLine}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (showFinish) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                    Text("Finish session")
                }
            }
        }
        Column {
            // One row of icon actions instead of two rows of emoji labels.
            // Emoji came out of the phone's own font at uneven sizes and read
            // as decoration, not controls. Each action is a 48 dp target that
            // sits in onSurfaceVariant at rest; the accent appears only when
            // the state it stands for is actually on.
            val saved = state.saved.contains(card.id)
            val liked = state.liked.contains(card.id)
            val disliked = state.disliked.contains(card.id)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CardAction(
                    icon = if (saved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Save card",
                    stateDescription = if (saved) "Saved" else "Not saved",
                    active = saved,
                    onClick = {
                        state.toggleSave(card.id)
                        sync.record(StateEvents.saved(card.id, !saved))
                    }
                )
                CardAction(
                    icon = if (liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = "Like card",
                    stateDescription = if (liked) "Liked" else "Not liked",
                    active = liked,
                    onClick = {
                        state.toggleLike(card.id)
                        sync.record(StateEvents.rating(card.id, if (liked) null else true))
                    }
                )
                CardAction(
                    // Core icons ship thumb_up but no thumb_down, so the
                    // thumb_down below is a local vector drawn from Google's
                    // own path data (see res/drawable).
                    icon = if (disliked) {
                        ImageVector.vectorResource(R.drawable.ic_thumb_down_filled_24)
                    } else {
                        ImageVector.vectorResource(R.drawable.ic_thumb_down_24)
                    },
                    contentDescription = "Dislike card",
                    stateDescription = if (disliked) "Disliked" else "Not disliked",
                    active = disliked,
                    onClick = {
                        state.toggleDislike(card.id)
                        sync.record(StateEvents.rating(card.id, if (disliked) null else false))
                    }
                )
                if (card.audioUrl == null) {
                    CardAction(
                        icon = Icons.Filled.PlayArrow,
                        contentDescription = "Listen to this card",
                        onClick = { tts.speak("${card.title}. ${card.body}") }
                    )
                }
                CardAction(
                    icon = Icons.Outlined.Info,
                    contentDescription = "Why am I seeing this?",
                    onClick = { showWhy = true }
                )
            }
            if (!tts.isReady) {
                Text(
                    text = "Voice loading… try Listen again in a second.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
    if (showWhy) {
        AlertDialog(
            onDismissRequest = { showWhy = false },
            title = { Text("Why am I seeing this?") },
            text = {
                Column {
                    Text("Type: ${card.type.name.lowercase()} · Topic: ${card.topic}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Source: ${card.source}")
                    if (card.bookLine != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Book: ${card.bookLine}")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (card.grounded) "Grounded in the source text ✓"
                        else "AI-written, unverified — treat as a lead, not a fact."
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showWhy = false }) { Text("Close") }
            }
        )
    }
}

/**
 * One card action: a 48 dp icon target.
 *
 * A plain clickable, not Material3's IconButton. In Material3 1.4.0, tapping an
 * action that then changes its own semantics and is disposed with the card can
 * detach a Surface's ChildSemanticsNode during traversal and crash the app
 * ("visitAncestors called on an unattached node"). This keeps the same role and
 * state for TalkBack without that semantics node.
 *
 * The light haptic tick matches the bottom bar, so every control in the app
 * answers a press the same way.
 */
@Composable
private fun CardAction(
    icon: ImageVector,
    contentDescription: String,
    stateDescription: String? = null,
    active: Boolean = false,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            }
            .semantics {
                this.contentDescription = contentDescription
                stateDescription?.let { this.stateDescription = it }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) colors.primary else colors.onSurfaceVariant
        )
    }
}

@Composable
private fun StandardBody(card: Card) {
    Text(
        text = card.title,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() }
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(text = card.body, fontSize = 18.sp, lineHeight = 26.sp)
    if (card.durationLabel != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${card.durationLabel} · tap Listen to hear it",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/** Audiobook chapter card body: stream player (Phase 6). */
@Composable
private fun AudioBody(card: Card, player: AudioPlayerBox?) {
    Text(
        text = card.title,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() }
    )
    if (card.bookLine != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "From: ${card.bookLine}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    Text(text = card.body, fontSize = 18.sp, lineHeight = 26.sp)
    Spacer(modifier = Modifier.height(20.dp))
    val url = card.audioUrl
    if (url == null) {
        Text("Audio link missing on this card.")
        return
    }
    if (player == null) {
        Text("Player not available right now.")
        return
    }
    val ui by player.state.collectAsStateWithLifecycle()
    val isThis = ui.url == url
    val playing = isThis && ui.playing
    val duration = if (isThis && ui.durationMs > 0) ui.durationMs
        else (card.durationLabel?.let { parseMinSecPlusMs(it) } ?: 0L)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilledIconButton(
            onClick = {
                if (isThis) {
                    player.toggle()
                } else {
                    player.playUrl(url, card.title)
                }
            },
            modifier = Modifier.semantics {
                contentDescription = if (playing) "Pause audiobook" else "Play audiobook"
            }
        ) {
            Text(text = if (playing) "⏸" else "▶", fontSize = 20.sp)
        }
        if (duration > 0) {
            Slider(
                value = (ui.positionMs.takeIf { isThis } ?: 0).toFloat().coerceIn(0f, duration.toFloat()),
                onValueChange = { player.seekTo(it.toLong()) },
                valueRange = 0f..duration.toFloat(),
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = "Audiobook position" }
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier.weight(1f)
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatMinSec(ui.positionMs.takeIf { isThis } ?: 0),
            style = MaterialTheme.typography.labelMedium
        )
        Text(
            text = if (duration > 0) formatMinSec(duration) else ("length " + (card.durationLabel ?: "…")),
            style = MaterialTheme.typography.labelMedium
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = if (playing) "Playing — keeps running with the screen off, controls on the lock screen."
        else "Streams online — nothing downloads to your phone.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (isThis && ui.failed) {
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Couldn't start this chapter — check the USB tunnel / network, then tap again.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/** Fallback duration parse of the server's "m:ss" label (no regex needed). */
private fun parseMinSecPlusMs(label: String): Long {
    val parts = label.split(":")
    val minutes = parts.getOrNull(parts.lastIndex - 1)?.toLongOrNull() ?: return 0
    val seconds = parts.lastOrNull()?.toLongOrNull() ?: return 0
    return (minutes * 60 + seconds) * 1000
}

@Composable
private fun QuoteBody(card: Card) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = card.title,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(modifier = Modifier.height(24.dp))
        // The server body usually carries its own quotation marks already —
        // strip edge quotes before adding ours so the card never shows two.
        Text(text = "\"${card.body.trim('"', '“', '”')}\"", fontSize = 26.sp, lineHeight = 34.sp)
    }
}

@Composable
private fun QuizBody(card: Card, onAnswered: (Boolean) -> Unit) {
    val quiz = card.quiz
    Text(
        text = card.title,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() }
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(text = card.body, fontSize = 18.sp)
    Spacer(modifier = Modifier.height(12.dp))
    if (quiz == null) {
        Text("Quiz data missing.")
        return
    }
    var selected by remember { mutableStateOf<Int?>(null) }
    quiz.options.forEachIndexed { index, option ->
        val label = ('A' + index).toString()
        OutlinedButton(
            onClick = {
                if (selected == null) {
                    selected = index
                    onAnswered(index == quiz.correctIndex) // feeds the SRS scheduler
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    if (selected != null) {
                        when {
                            index == quiz.correctIndex -> stateDescription = "Correct answer"
                            index == selected -> stateDescription = "Your answer, incorrect"
                        }
                    }
                }
        ) {
            val mark = when {
                selected == null -> ""
                index == quiz.correctIndex -> " ✓"
                index == selected -> " ✗"
                else -> ""
            }
            Text("$label) $option$mark")
        }
    }
    if (selected != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = quiz.explanation, style = MaterialTheme.typography.bodyMedium)
    }
}
