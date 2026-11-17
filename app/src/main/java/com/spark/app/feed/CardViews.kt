package com.spark.app.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spark.app.BuildConfig
import com.spark.app.R
import com.spark.app.audio.AudioPlayerBox
import com.spark.app.audio.TtsHelper
import com.spark.app.audio.formatMinSec
import com.spark.app.data.Card
import com.spark.app.data.CardType
import com.spark.app.data.remote.DataResponse
import com.spark.app.data.remote.SubtitleCueDto
import com.spark.app.sync.InteractionSync
import com.spark.app.sync.StateEvents
import com.spark.app.ui.OfflineConnectivityButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

// Paper-and-forest palette for the reel cards (same tokens as Home).
private val Paper = Color(0xFFF4EFE5)
private val PaperSoft = Color(0xFFFAF5EA)
private val Ember = Color(0xFFFFB454)
private val EmberSoft = Color(0xFFFFDDB8)
private val ForestTop = Color(0xFF1C4534)
private val ForestMid = Color(0xFF163528)
private val ForestDeep = Color(0xFF0D2118)
private val Display = FontFamily.Serif

@Composable
fun CardView(
    card: Card,
    counter: String,
    tts: TtsHelper,
    state: FeedState,
    sync: InteractionSync,
    player: AudioPlayerBox?,
    onOpenAudioPlayer: () -> Unit,
    showFinish: Boolean,
    onFinish: () -> Unit,
    offline: Boolean = false,
    onRetryOffline: (() -> Unit)? = null,
    /** Short notice shown while a saved card is opened in this deck. */
    openingLabel: String? = null,
    modifier: Modifier = Modifier
) {
    var showWhy by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val saved = state.saved.contains(card.id)
    val liked = state.liked.contains(card.id)
    val disliked = state.disliked.contains(card.id)

    // Center double-tap burst state
    val doubleTapBurst = remember { Animatable(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.verticalGradient(listOf(ForestTop, ForestMid, ForestDeep)))
            .pointerInput(card.id) {
                detectTapGestures(
                    onDoubleTap = {
                        // Double tap toggles Like (reels convention) with floating heart burst
                        val wasLiked = state.liked.contains(card.id)
                        state.toggleLike(card.id)
                        sync.record(StateEvents.rating(card.id, if (wasLiked) null else true))
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        coroutineScope.launch {
                            doubleTapBurst.snapTo(0f)
                            doubleTapBurst.animateTo(
                                1f,
                                animationSpec = tween(500, easing = FastOutSlowInEasing)
                            )
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            key(card.id) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Header row: topic chip, counter, and live listening status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${card.type.label()} · ${card.topic}",
                                fontSize = 12.sp,
                                letterSpacing = 0.2.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Paper.copy(alpha = 0.86f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(Paper.copy(alpha = 0.09f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Gentle live listening indicator badge
                            if (tts.isSpeaking) {
                                LiveListeningPill()
                                Spacer(Modifier.width(8.dp))
                            }

                            if (offline && onRetryOffline != null) {
                                OfflineConnectivityButton(
                                    onRetry = onRetryOffline,
                                    modifier = Modifier.size(48.dp),
                                    description = "Offline library. Tap to retry the deck."
                                )
                                Spacer(Modifier.width(4.dp))
                            }

                            if (openingLabel != null) {
                                OpeningPill(openingLabel)
                                Spacer(Modifier.width(8.dp))
                            }

                            Text(
                                text = counter,
                                fontSize = 11.sp,
                                letterSpacing = 1.2.sp,
                                color = Paper.copy(alpha = 0.35f)
                            )
                        }
                    }

                    if (!card.grounded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Ember.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AI-written, unverified",
                                fontSize = 11.sp,
                                color = EmberSoft
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (card.type) {
                        CardType.QUOTE -> QuoteBody(card)
                        CardType.QUIZ -> QuizBody(
                            card = card,
                            onAnswered = { correct -> sync.record(StateEvents.quiz(card.id, correct)) }
                        )
                        CardType.AUDIO -> AudioBody(
                            card = card,
                            player = player,
                            onOpenAudioPlayer = onOpenAudioPlayer,
                            modifier = Modifier.weight(1f)
                        )
                        else -> StandardBody(card)
                    }

                    if (card.actionLine.isNotBlank() && card.type != CardType.AUDIO) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = card.actionLine,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = Paper.copy(alpha = 0.8f)
                        )
                    }

                    if (showFinish) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onFinish,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(100.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Ember,
                                contentColor = ForestDeep
                            )
                        ) {
                            Text("Finish session", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            val isAudioCard = card.type == CardType.AUDIO || card.audioUrl != null

            if (isAudioCard) {
                // Audiobook Card: Player controls pinned at the bottom!
                // Per user instruction: Like, Dislike, Heart (Save), and Eye (Info) buttons removed.
                Column(modifier = Modifier.fillMaxWidth()) {
                    AudioPlayerBottomBar(
                        card = card,
                        player = player,
                        onOpenAudioPlayer = onOpenAudioPlayer
                    )
                    Spacer(Modifier.height(76.dp))
                }
            } else {
                Column {
                    // Tactile action row with bouncy spring responses and spark bursts
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
                            accentColor = Ember,
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
                            accentColor = Ember,
                            onClick = {
                                state.toggleLike(card.id)
                                sync.record(StateEvents.rating(card.id, if (liked) null else true))
                            }
                        )
                        CardAction(
                            icon = if (disliked) {
                                ImageVector.vectorResource(R.drawable.ic_thumb_down_filled_24)
                            } else {
                                ImageVector.vectorResource(R.drawable.ic_thumb_down_24)
                            },
                            contentDescription = "Dislike card",
                            stateDescription = if (disliked) "Disliked" else "Not disliked",
                            active = disliked,
                            accentColor = EmberSoft,
                            onClick = {
                                state.toggleDislike(card.id)
                                sync.record(StateEvents.rating(card.id, if (disliked) null else false))
                            }
                        )
                        val isReadingThis = tts.isSpeaking
                        CardAction(
                            icon = Icons.Filled.PlayArrow,
                            contentDescription = if (isReadingThis) "Stop listening" else "Listen to this card",
                            stateDescription = if (isReadingThis) "Playing voice" else "Not playing",
                            active = isReadingThis,
                            customContent = {
                                AudioPulseIndicator(active = isReadingThis)
                            },
                            onClick = {
                                tts.toggle("${card.title}. ${card.body}")
                            }
                        )
                        CardAction(
                            icon = Icons.Outlined.Info,
                            contentDescription = "Why am I seeing this?",
                            onClick = { showWhy = true }
                        )
                    }

                    if (!tts.isReady) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Voice loading… try Listen again in a second.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = Paper.copy(alpha = 0.78f)
                        )
                    }

                    // Breathing room above the floating glass navigation bar
                    Spacer(Modifier.height(76.dp))
                }
            }
        }

        // Floating Double-Tap Heart Burst Overlay
        if (doubleTapBurst.value > 0f && doubleTapBurst.value < 1f) {
            val progress = doubleTapBurst.value
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 0.6f + progress * 0.9f
                        scaleY = 0.6f + progress * 0.9f
                        alpha = (1f - progress).coerceIn(0f, 1f)
                        translationY = -progress * 80.dp.toPx()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Ember,
                    modifier = Modifier.size(72.dp)
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
                    Text("Type: ${card.type.label()} · Topic: ${card.topic}")
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

/** A restrained action control with a clear saved/liked state and a short press response. */
@Composable
private fun CardAction(
    icon: ImageVector,
    contentDescription: String,
    stateDescription: String? = null,
    active: Boolean = false,
    accentColor: Color = Ember,
    customContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    val pressScale = animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "actionPress"
    )
    val selectedScale = animateFloatAsState(
        targetValue = if (active) 1.04f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "actionSelectedScale"
    )
    val iconTint by animateColorAsState(
        targetValue = if (active) accentColor else Paper.copy(alpha = 0.75f),
        animationSpec = tween(durationMillis = 140),
        label = "actionTint"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer {
                val scale = pressScale.value * selectedScale.value
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            }
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                stateDescription?.let { this.stateDescription = it }
            },
        contentAlignment = Alignment.Center
    ) {
        if (customContent != null) {
            customContent()
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint
            )
        }
    }
}

/**
 * Animated 3-bar Audio Equalizer Pulse Indicator.
 * Renders vertical dancing rounded bars when TTS/audio is active.
 */
@Composable
private fun AudioPulseIndicator(active: Boolean) {
    if (!active) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Paper.copy(alpha = 0.75f)
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "audioEqualizer")
    val b1 by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(380, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b1"
    )
    val b2 by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(280, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b2"
    )
    val b3 by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(440, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "b3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.size(24.dp)
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(6.dp + 12.dp * b1)
                .clip(RoundedCornerShape(50))
                .background(Ember)
        )
        Box(
            Modifier
                .weight(1f)
                .height(6.dp + 12.dp * b2)
                .clip(RoundedCornerShape(50))
                .background(Ember)
        )
        Box(
            Modifier
                .weight(1f)
                .height(6.dp + 12.dp * b3)
                .clip(RoundedCornerShape(50))
                .background(Ember)
        )
    }
}

/**
 * Small header pill with a pulsing dot: a saved card is being opened in this
 * deck (or could not be found and the offline deck took its place).
 */
@Composable
private fun OpeningPill(label: String) {
    val transition = rememberInfiniteTransition(label = "openingPulse")
    val dotAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "openingDotPulse"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Ember.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics { contentDescription = label }
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .graphicsLayer { alpha = dotAlpha }
                .clip(CircleShape)
                .background(Ember)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            letterSpacing = 0.4.sp,
            fontWeight = FontWeight.Bold,
            color = Ember,
            lineHeight = 14.sp
        )
    }
}

/** Small header pill showing live listening status with a pulsing dot beacon. */
@Composable
private fun LiveListeningPill() {
    val transition = rememberInfiniteTransition(label = "listeningPulse")
    val dotAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotPulse"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Ember.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .graphicsLayer { alpha = dotAlpha }
                .clip(CircleShape)
                .background(Ember)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "LISTENING",
            fontSize = 11.sp,
            letterSpacing = 0.6.sp,
            fontWeight = FontWeight.Bold,
            color = Ember,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun StandardBody(card: Card) {
    Text(
        text = card.title,
        fontFamily = Display,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.6).sp,
        color = PaperSoft,
        modifier = Modifier.semantics { heading() }
    )
    Spacer(modifier = Modifier.height(10.dp))
    val cleanBody = remember(card.body) {
        card.body
            .replace(Regex("^###[^\n\r]+", RegexOption.MULTILINE), "")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("\\*([^*]+)\\*"), "$1")
            .trim()
    }
    Text(
        text = cleanBody,
        fontSize = 15.sp,
        lineHeight = 23.sp,
        color = Paper.copy(alpha = 0.82f)
    )
    if (card.durationLabel != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${card.durationLabel} · tap Listen to hear it",
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = Paper.copy(alpha = 0.78f)
        )
    }
}

/** Audiobook chapter card body: displays title, book pill, and live subtitles synced with narration. */
@Composable
private fun AudioBody(
    card: Card,
    player: AudioPlayerBox?,
    onOpenAudioPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ui = player?.state?.collectAsStateWithLifecycle()?.value
    val url = card.audioUrl
    val resolvedUrl = if (url != null) {
        if (url.startsWith("http://") || url.startsWith("https://")) url
        else "${BuildConfig.API_BASE_URL.removeSuffix("/")}/${url.removePrefix("/")}"
    } else null
    val isThis = url != null && (ui?.url == url || ui?.url == resolvedUrl)
    val positionMs = if (isThis) (ui?.positionMs ?: 0L) else 0L
    val isPlaying = isThis && (ui?.playing == true)

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = card.title,
            fontFamily = Display,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            color = PaperSoft,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )

        if (card.bookLine != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Ember.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_book_filled_24),
                    contentDescription = null,
                    tint = Ember,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = card.bookLine,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmberSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Live Subtitle Viewer taking the available vertical space
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LiveSubtitleViewer(
                card = card,
                positionMs = positionMs,
                isPlaying = isPlaying,
                onSeekTo = { targetMs ->
                    if (!isThis && url != null) {
                        player?.playUrl(url, card.title)
                    }
                    player?.seekTo(targetMs)
                }
            )
        }
    }
}

/** Displays synchronized subtitles that follow the narrator's voice in real time. */
@Composable
private fun LiveSubtitleViewer(
    card: Card,
    positionMs: Long,
    isPlaying: Boolean,
    onSeekTo: (Long) -> Unit
) {
    val subtitleKey = remember(card.audioUrl) {
        card.audioUrl?.substringAfterLast("/")?.substringBefore("?") ?: ""
    }

    var cues by remember(subtitleKey) { mutableStateOf<List<com.spark.app.data.remote.SubtitleCueDto>>(emptyList()) }
    var loaded by remember(subtitleKey) { mutableStateOf(false) }

    LaunchedEffect(subtitleKey) {
        cues = SubtitleRepository.getSubtitles(
            key = subtitleKey,
            bodyFallback = card.body,
            durationLabel = card.durationLabel
        )
        loaded = true
    }

    val activeIndex = remember(cues, positionMs) {
        if (cues.isEmpty()) -1
        else {
            // 200ms anticipatory lead so line highlights right as the narrator speaks the sentence
            val targetMs = positionMs + 200L
            val idx = cues.indexOfLast { it.startMs <= targetMs }
            if (idx != -1) idx.coerceAtMost(cues.lastIndex) else 0
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            listState.animateScrollToItem(
                index = (activeIndex - 1).coerceAtLeast(0),
                scrollOffset = -24
            )
        }
    }

    // Frameless, organic reel caption flow
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (!loaded) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Loading subtitles...",
                    fontSize = 14.sp,
                    color = Paper.copy(alpha = 0.5f)
                )
            }
        } else if (cues.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tap play below to start listening.",
                    fontSize = 14.sp,
                    color = Paper.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Spacer(Modifier.height(8.dp))
                }
                itemsIndexed(cues) { index, cue ->
                    val isActive = index == activeIndex
                    val isPast = activeIndex > index

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isActive) Ember.copy(alpha = 0.16f)
                                else Color.Transparent
                            )
                            .clickable { onSeekTo(cue.startMs) }
                            .padding(
                                horizontal = if (isActive) 12.dp else 4.dp,
                                vertical = if (isActive) 10.dp else 4.dp
                            )
                    ) {
                        Text(
                            text = cue.text,
                            fontSize = if (isActive) 20.sp else 16.sp,
                            lineHeight = if (isActive) 28.sp else 23.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = when {
                                isActive -> PaperSoft
                                isPast -> Paper.copy(alpha = 0.35f)
                                else -> Paper.copy(alpha = 0.65f)
                            }
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

/** Pinned bottom player controls for audiobook cards. */
@Composable
private fun AudioPlayerBottomBar(
    card: Card,
    player: AudioPlayerBox?,
    onOpenAudioPlayer: () -> Unit
) {
    val url = card.audioUrl ?: return
    val playerBox = player ?: return
    val ui by playerBox.state.collectAsStateWithLifecycle()
    val resolvedUrl = if (url.startsWith("http://") || url.startsWith("https://")) {
        url
    } else {
        "${BuildConfig.API_BASE_URL.removeSuffix("/")}/${url.removePrefix("/")}"
    }
    val isThis = ui.url == url || ui.url == resolvedUrl
    val isFailed = isThis && ui.failed
    val playing = isThis && ui.playing && !isFailed
    val duration = if (isThis && ui.durationMs > 0) ui.durationMs
    else (card.durationLabel?.let { parseMinSecPlusMs(it) } ?: 0L)
    val position = if (isThis) ui.positionMs else 0L

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF132B20).copy(alpha = 0.96f),
        border = BorderStroke(1.dp, Color(0xFF2E5E4A)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Ember)
                            .clickable {
                                if (isThis && !isFailed) {
                                    playerBox.toggle()
                                } else {
                                    playerBox.playUrl(url, card.title)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (playing) {
                            Text(
                                text = "Ⅱ",
                                color = Color(0xFF121110),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play audiobook",
                                tint = Color(0xFF121110),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (playing) "PLAYING CHAPTER" else "AUDIOBOOK CHAPTER",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = if (playing) Ember else Paper.copy(alpha = 0.65f)
                        )
                        Text(
                            text = if (duration > 0) formatMinSec(duration) else (card.durationLabel ?: "Audio"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PaperSoft
                        )
                    }
                }

                IconButton(
                    onClick = {
                        if (!isThis || isFailed) playerBox.playUrl(url, card.title)
                        onOpenAudioPlayer()
                    }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_headphones_16),
                        contentDescription = "Open full audio player",
                        tint = Ember,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            if (duration > 0) {
                Slider(
                    value = position.toFloat().coerceIn(0f, duration.toFloat().coerceAtLeast(1f)),
                    onValueChange = { playerBox.seekTo(it.toLong()) },
                    valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Ember,
                        activeTrackColor = Ember,
                        inactiveTrackColor = Paper.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .semantics { contentDescription = "Audiobook position" }
                )
            } else {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Ember,
                    trackColor = Paper.copy(alpha = 0.15f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMinSec(position),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Paper.copy(alpha = 0.7f)
                )
                Text(
                    text = if (duration > 0) formatMinSec(duration) else (card.durationLabel ?: "0:00"),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Paper.copy(alpha = 0.7f)
                )
            }
        }
    }

    if (isFailed) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                .clickable { playerBox.playUrl(url, card.title) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Connection lost · Tap to retry chapter",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
        }
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
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = card.title.uppercase(),
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.8.sp,
            fontWeight = FontWeight.SemiBold,
            color = Paper.copy(alpha = 0.78f),
            modifier = Modifier.semantics { heading() }
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Ember)) { append("“") }
                append(card.body.trim('"', '\u201C', '\u201D'))
                withStyle(SpanStyle(color = Ember)) { append("”") }
            },
            fontFamily = Display,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.4).sp,
            color = PaperSoft
        )
    }
}

@Composable
private fun QuizBody(card: Card, onAnswered: (Boolean) -> Unit) {
    val quiz = card.quiz
    Text(
        text = card.title,
        fontFamily = Display,
        fontSize = 24.sp,
        lineHeight = 29.sp,
        color = PaperSoft,
        modifier = Modifier.semantics { heading() }
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = card.body, fontSize = 14.sp, lineHeight = 21.sp, color = Paper.copy(alpha = 0.82f))
    Spacer(modifier = Modifier.height(12.dp))
    if (quiz == null) {
        Text("Quiz data missing.", color = Paper.copy(alpha = 0.7f))
        return
    }
    var selected by remember { mutableStateOf<Int?>(null) }
    quiz.options.forEachIndexed { index, option ->
        val label = ('A' + index).toString()
        Surface(
            onClick = {
                if (selected == null) {
                    selected = index
                    onAnswered(index == quiz.correctIndex)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
                .semantics {
                    if (selected != null) {
                        when {
                            index == quiz.correctIndex -> stateDescription = "Correct answer"
                            index == selected -> stateDescription = "Your answer, incorrect"
                        }
                    }
                },
            shape = RoundedCornerShape(100.dp),
            color = Paper.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Paper.copy(alpha = 0.22f))
        ) {
            val mark = when {
                selected == null -> ""
                index == quiz.correctIndex -> " ✓"
                index == selected -> " ✗"
                else -> ""
            }
            Text(
                text = "$label) $option$mark",
                fontSize = 13.sp,
                color = if (selected != null && index == quiz.correctIndex) Ember else PaperSoft,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
    if (selected != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = quiz.explanation,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = Paper.copy(alpha = 0.82f)
        )
    }
}

private fun CardType.label(): String = when (this) {
    CardType.BOOK_IDEA -> "Book idea"
    CardType.SUMMARY -> "Summary"
    CardType.QUOTE -> "Quote"
    CardType.CONCEPT -> "Concept"
    CardType.QUIZ -> "Quiz"
    CardType.AUDIO -> "Audio"
    CardType.CHALLENGE -> "Challenge"
}
