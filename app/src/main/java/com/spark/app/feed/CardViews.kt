package com.spark.app.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.spark.app.BuildConfig
import com.spark.app.R
import com.spark.app.audio.AudioPlayerBox
import com.spark.app.audio.TtsHelper
import com.spark.app.audio.formatMinSec
import com.spark.app.data.Card
import com.spark.app.data.CardType
import com.spark.app.sync.InteractionSync
import com.spark.app.sync.StateEvents
import com.spark.app.ui.OfflineConnectivityButton
import kotlinx.coroutines.launch

// Exact design palette tokens from Main.dc.html
private val CanvasBackground = Color(0xFF0B0B0A)
private val CardBackground = Color(0xFF1A1A18)
private val BrutalistShadow = Color(0xFF3A3A36)
private val GoldAccent = Color(0xFFF2B632)
private val IvoryText = Color(0xFFF3F1EA)
private val MutedText = Color(0xFFA3A29B)
private val BodyTextColor = Color(0xFFD2D0C6)
private val WaveformUnplayed = Color(0xFF6B6A64)
private val SageFallbackCover = Color(0xFF5B9467)
private val TrackLineColor = Color(0xFF2A2A27)

/**
 * Bookmark ribbon shape from Main.dc.html:
 * SVG path: M0 0h34v56l-17-12L0 56z
 */
private val RibbonShape = GenericShape { size, _ ->
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height)
    val notchDepth = 12f * (size.height / 56f)
    lineTo(size.width / 2f, size.height - notchDepth)
    lineTo(0f, size.height)
    close()
}

/**
 * 51-bar waveform coordinates extracted exactly from Main.dc.html:
 * viewBox: 0 0 308 44
 * Each element is Triple(x, y1, y2).
 */
private val WAVEFORM_BARS = listOf(
    Triple(3f, 17f, 27f),
    Triple(9f, 16f, 28f),
    Triple(15f, 13f, 31f),
    Triple(21f, 16f, 28f),
    Triple(27f, 16f, 28f),
    Triple(33f, 16f, 28f),
    Triple(39f, 16f, 28f),
    Triple(45f, 14f, 30f),
    Triple(51f, 17f, 27f),
    Triple(57f, 15f, 29f),
    Triple(63f, 14f, 30f),
    Triple(69f, 11f, 33f),
    Triple(75f, 8f, 36f),
    Triple(81f, 5f, 39f),
    Triple(87f, 8f, 36f),
    Triple(93f, 10f, 34f),
    Triple(99f, 12f, 32f),
    Triple(105f, 15f, 29f),
    Triple(111f, 12f, 32f),
    Triple(117f, 10f, 34f),
    Triple(123f, 6f, 38f),
    Triple(129f, 9f, 35f),
    Triple(135f, 7f, 37f),
    Triple(141f, 11f, 33f),
    Triple(147f, 14f, 30f),
    Triple(153f, 17f, 27f),
    Triple(159f, 16f, 28f),
    Triple(165f, 14f, 30f),
    Triple(171f, 17f, 27f),
    Triple(177f, 15f, 29f),
    Triple(183f, 14f, 30f),
    Triple(189f, 14f, 30f),
    Triple(195f, 13f, 31f),
    Triple(201f, 16f, 28f),
    Triple(207f, 18f, 26f),
    Triple(213f, 14f, 30f),
    Triple(219f, 9f, 35f),
    Triple(225f, 8f, 36f),
    Triple(231f, 7f, 37f),
    Triple(237f, 6f, 38f),
    Triple(243f, 8f, 36f),
    Triple(249f, 11f, 33f),
    Triple(255f, 13f, 31f),
    Triple(261f, 13f, 31f),
    Triple(267f, 12f, 32f),
    Triple(273f, 8f, 36f),
    Triple(279f, 8f, 36f),
    Triple(285f, 8f, 36f),
    Triple(291f, 10f, 34f),
    Triple(297f, 14f, 30f),
    Triple(303f, 13f, 31f)
)

/**
 * Top Segmented Progress Bar:
 * Height 4dp, track #2A2A27, stroke #F3F1EA with dashes (15.05dp dash, 3dp gap).
 */
@Composable
fun TopSegmentedProgressBar(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    val progressFraction = remember(currentPage, totalPages) {
        if (totalPages <= 0) 0f
        else ((currentPage + 1).toFloat() / totalPages.toFloat()).coerceIn(0f, 1f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
    ) {
        val strokeWidthPx = 4.dp.toPx()
        val dashOnPx = 15.05.dp.toPx()
        val dashOffPx = 3.dp.toPx()
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashOnPx, dashOffPx), 0f)

        // Background track: #2A2A27
        drawLine(
            color = TrackLineColor,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = strokeWidthPx,
            pathEffect = pathEffect
        )

        // Progress line: #F3F1EA
        if (progressFraction > 0f) {
            drawLine(
                color = IvoryText,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width * progressFraction, size.height / 2f),
                strokeWidth = strokeWidthPx,
                pathEffect = pathEffect
            )
        }
    }
}

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
    val context = LocalContext.current

    val saved = state.saved.contains(card.id)
    val liked = state.liked.contains(card.id)

    // Center double-tap burst state
    val doubleTapBurst = remember { Animatable(0f) }

    // Parse book title and author
    val (bookTitle, bookAuthor) = remember(card) {
        if (!card.bookLine.isNullOrBlank()) {
            val line = card.bookLine
            val parts = when {
                line.contains(" — ") -> line.split(" — ", limit = 2)
                line.contains(" - ") -> line.split(" - ", limit = 2)
                line.contains(" by ") -> line.split(" by ", limit = 2)
                else -> listOf(line, card.source.ifBlank { "Author" })
            }
            Pair(parts[0].trim(), parts.getOrNull(1)?.trim() ?: card.source.ifBlank { "Author" })
        } else {
            Pair(card.source.ifBlank { card.topic }, "SPARK")
        }
    }

    // Card Index number for top-right bookmark ribbon tag (e.g., "11")
    val ribbonNumber = remember(counter) {
        counter.substringBefore("/").trim()
    }

    val isAudioCard = card.type == CardType.AUDIO || !card.audioUrl.isNullOrBlank()

    // Outer card container with brutalist shadow, inner dashed border, and ribbon tag
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 22.dp, top = 8.dp, bottom = 86.dp)
            .pointerInput(card.id) {
                detectTapGestures(
                    onDoubleTap = {
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
        // Brutalist shadow + card surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val cornerPx = 28.dp.toPx()
                    val shadowOffset = 6.dp.toPx()
                    // Brutalist solid shadow: 6px 6px 0 #3A3A36
                    drawRoundRect(
                        color = BrutalistShadow,
                        topLeft = Offset(shadowOffset, shadowOffset),
                        size = size,
                        cornerRadius = CornerRadius(cornerPx, cornerPx)
                    )
                }
                .clip(RoundedCornerShape(28.dp))
                .background(CardBackground)
                .drawWithContent {
                    drawContent()
                    // Inner dashed border: inset 8px, border 1.5px dashed rgba(243,241,234,0.2), radius 21px
                    val insetPx = 8.dp.toPx()
                    val cornerPx = 21.dp.toPx()
                    val strokePx = 1.5.dp.toPx()
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 6.dp.toPx()), 0f)
                    drawRoundRect(
                        color = IvoryText.copy(alpha = 0.20f),
                        topLeft = Offset(insetPx, insetPx),
                        size = Size(size.width - insetPx * 2, size.height - insetPx * 2),
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                        style = Stroke(width = strokePx, pathEffect = dashEffect)
                    )
                }
        ) {
            // Main card interior content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                ) {
                    // Top-Left Header: Tilted book thumbnail + Book Title & Author
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 40.dp), // clearance for top-right ribbon
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Tilted mini book cover: 28px × 40px, -5deg, rounded 3px
                        Box(
                            modifier = Modifier
                                .size(width = 28.dp, height = 40.dp)
                                .graphicsLayer { rotationZ = -5f }
                                .clip(RoundedCornerShape(3.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!card.coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(card.coverUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "$bookTitle cover",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // Sage green fallback cover with 3 horizontal lines (from Main.dc.html)
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(SageFallbackCover),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.size(18.dp, 22.dp)) {
                                        val sx = size.width / 18f
                                        val sy = size.height / 22f
                                        val strk = 1.6f * density
                                        drawLine(IvoryText, Offset(3f * sx, 6f * sy), Offset(15f * sx, 6f * sy), strk, StrokeCap.Round)
                                        drawLine(IvoryText, Offset(3f * sx, 10f * sy), Offset(15f * sx, 10f * sy), strk, StrokeCap.Round)
                                        drawLine(IvoryText, Offset(5f * sx, 16f * sy), Offset(13f * sx, 16f * sy), strk, StrokeCap.Round)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = bookTitle,
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium,
                                fontSize = 17.sp,
                                color = IvoryText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = bookAuthor,
                                fontSize = 13.sp,
                                color = MutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (offline && onRetryOffline != null) {
                            OfflineConnectivityButton(
                                onRetry = onRetryOffline,
                                modifier = Modifier.size(36.dp),
                                description = "Offline. Tap to retry."
                            )
                        }
                    }

                    // Sub-header Metadata: "APPENDIX · 46 SEC" / "CHAPTER 1 · 46 SEC"
                    val subHeaderMeta = remember(card) {
                        val chapterPart = when {
                            card.type == CardType.AUDIO -> "CHAPTER"
                            card.type == CardType.SUMMARY -> "SUMMARY"
                            card.type == CardType.QUOTE -> "QUOTE"
                            else -> card.topic.uppercase()
                        }
                        val durPart = card.durationLabel?.uppercase() ?: "46 SEC"
                        "$chapterPart · $durPart"
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = subHeaderMeta,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        color = MutedText
                    )

                    // Headline (Card Title): 34-36sp serif semi-bold #F3F1EA
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = card.title,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 32.sp,
                        lineHeight = 36.sp,
                        letterSpacing = (-0.5).sp,
                        color = IvoryText,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.semantics { heading() }
                    )

                    // Playful golden underline flourish under title
                    Canvas(
                        modifier = Modifier
                            .width(100.dp)
                            .height(6.dp)
                            .padding(top = 2.dp)
                    ) {
                        val path = Path().apply {
                            val w = size.width
                            val h = size.height
                            moveTo(0f, h * 0.6f)
                            cubicTo(w * 0.15f, 0f, w * 0.35f, h, w * 0.55f, h * 0.4f)
                            cubicTo(w * 0.75f, 0f, w * 0.9f, h * 0.8f, w, h * 0.5f)
                        }
                        drawPath(
                            path = path,
                            color = GoldAccent,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Body Text with Drop Cap
                    Spacer(Modifier.height(16.dp))
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(scrollState)
                    ) {
                        when (card.type) {
                            CardType.QUOTE -> QuoteBodyEditorial(card)
                            CardType.QUIZ -> QuizBodyEditorial(card, onAnswered = { correct ->
                                sync.record(StateEvents.quiz(card.id, correct))
                            })
                            else -> StandardBodyEditorial(card)
                        }
                    }

                    if (showFinish) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onFinish,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldAccent,
                                contentColor = CardBackground
                            )
                        ) {
                            Text("Finish session", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Bottom Player Controls or Non-Audio Action Row
                if (isAudioCard) {
                    AudioPlayerSection(
                        card = card,
                        player = player,
                        saved = saved,
                        liked = liked,
                        onToggleSave = {
                            state.toggleSave(card.id)
                            sync.record(StateEvents.saved(card.id, !saved))
                        },
                        onToggleLike = {
                            state.toggleLike(card.id)
                            sync.record(StateEvents.rating(card.id, if (liked) null else true))
                        }
                    )
                } else {
                    // Non-Audio Cards: Audio section removed entirely per user instruction
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, start = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SaveBookmarkButton(saved = saved, onClick = {
                            state.toggleSave(card.id)
                            sync.record(StateEvents.saved(card.id, !saved))
                        })

                        if (card.actionLine.isNotBlank()) {
                            Text(
                                text = card.actionLine,
                                fontSize = 12.sp,
                                color = MutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )
                        }

                        LikeHeartButton(liked = liked, onClick = {
                            state.toggleLike(card.id)
                            sync.record(StateEvents.rating(card.id, if (liked) null else true))
                        })
                    }
                }
            }
        }

        // Top-Right Bookmark Ribbon Tag: 34px × 56px, right: 28px, fill #F2B632, number in #1A1A18
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 28.dp)
                .size(width = 34.dp, height = 56.dp)
                .clip(RibbonShape)
                .background(GoldAccent),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = ribbonNumber,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = CardBackground,
                modifier = Modifier.padding(top = 10.dp)
            )
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
                Canvas(modifier = Modifier.size(72.dp)) {
                    val scale = size.width / 24f
                    val path = Path().apply {
                        moveTo(12f * scale, 21.35f * scale)
                        cubicTo(11.45f * scale, 20.85f * scale, 2f * scale, 12.28f * scale, 2f * scale, 7.5f * scale)
                        cubicTo(2f * scale, 4.42f * scale, 4.42f * scale, 2f * scale, 7.5f * scale, 2f * scale)
                        cubicTo(9.24f * scale, 2f * scale, 10.91f * scale, 2.81f * scale, 12f * scale, 4.09f * scale)
                        cubicTo(13.09f * scale, 2.81f * scale, 14.76f * scale, 2f * scale, 16.5f * scale, 2f * scale)
                        cubicTo(19.58f * scale, 2f * scale, 22f * scale, 4.42f * scale, 22f * scale, 7.5f * scale)
                        cubicTo(22f * scale, 12.28f * scale, 12.55f * scale, 20.85f * scale, 12f * scale, 21.35f * scale)
                        close()
                    }
                    drawPath(path, color = GoldAccent)
                }
            }
        }
    }

    if (showWhy) {
        AlertDialog(
            onDismissRequest = { showWhy = false },
            title = { Text("About this card", color = IvoryText) },
            text = {
                Column {
                    Text("Type: ${card.type.name} · Topic: ${card.topic}", color = BodyTextColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Source: ${card.source}", color = BodyTextColor)
                    if (card.bookLine != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Book: ${card.bookLine}", color = BodyTextColor)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (card.grounded) "Grounded in the source text ✓"
                        else "AI-written, unverified — treat as a lead, not a fact.",
                        color = if (card.grounded) GoldAccent else MutedText
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showWhy = false }) {
                    Text("Close", color = GoldAccent)
                }
            },
            containerColor = CardBackground
        )
    }
}

/** Standard Body with 17sp serif typography and drop cap */
@Composable
private fun StandardBodyEditorial(card: Card) {
    val cleanBody = remember(card.body) {
        card.body
            .replace(Regex("^###[^\n\r]+", RegexOption.MULTILINE), "")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("\\*([^*]+)\\*"), "$1")
            .trim()
    }

    val paragraphs = remember(cleanBody) {
        cleanBody.split(Regex("\n+")).filter { it.isNotBlank() }
    }

    paragraphs.forEachIndexed { index, paragraph ->
        if (index > 0) {
            Spacer(Modifier.height(10.dp))
        }

        if (index == 0 && paragraph.isNotEmpty()) {
            val firstChar = paragraph.take(1)
            val restOfPara = paragraph.drop(1)
            val annotated = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = GoldAccent
                    )
                ) {
                    append(firstChar)
                }
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Serif,
                        fontSize = 17.sp,
                        color = BodyTextColor
                    )
                ) {
                    append(restOfPara)
                }
            }
            Text(
                text = annotated,
                lineHeight = 25.sp
            )
        } else {
            Text(
                text = paragraph,
                fontFamily = FontFamily.Serif,
                fontSize = 17.sp,
                lineHeight = 25.sp,
                color = BodyTextColor
            )
        }
    }
}

/** Quote Card Body with elegant serif styling and golden quotes */
@Composable
private fun QuoteBodyEditorial(card: Card) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = GoldAccent, fontSize = 28.sp, fontWeight = FontWeight.Bold)) {
                append("“")
            }
            append(card.body.trim('"', '\u201C', '\u201D'))
            withStyle(SpanStyle(color = GoldAccent, fontSize = 28.sp, fontWeight = FontWeight.Bold)) {
                append("”")
            }
        },
        fontFamily = FontFamily.Serif,
        fontStyle = FontStyle.Italic,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        color = IvoryText
    )
}

/** Quiz Card Body with capsules in dark editorial theme */
@Composable
private fun QuizBodyEditorial(card: Card, onAnswered: (Boolean) -> Unit) {
    val quiz = card.quiz
    Text(
        text = card.body,
        fontFamily = FontFamily.Serif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        color = BodyTextColor
    )
    Spacer(Modifier.height(12.dp))
    if (quiz == null) return

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
                .padding(bottom = 8.dp),
            shape = RoundedCornerShape(100.dp),
            color = IvoryText.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, IvoryText.copy(alpha = 0.22f))
        ) {
            val mark = when {
                selected == null -> ""
                index == quiz.correctIndex -> " ✓"
                index == selected -> " ✗"
                else -> ""
            }
            Text(
                text = "$label) $option$mark",
                fontSize = 14.sp,
                color = if (selected != null && index == quiz.correctIndex) GoldAccent else IvoryText,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
    if (selected != null) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = quiz.explanation,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = BodyTextColor
        )
    }
}

/**
 * Pinned Audio Section:
 * 1. 51-bar waveform with drag/tap scrubbing
 * 2. Left and right timestamps
 * 3. 5-button control row: Save, Rewind 15, Center 60dp Play/Pause with spinner, Forward 15, Like
 */
@Composable
private fun AudioPlayerSection(
    card: Card,
    player: AudioPlayerBox?,
    saved: Boolean,
    liked: Boolean,
    onToggleSave: () -> Unit,
    onToggleLike: () -> Unit
) {
    val url = card.audioUrl ?: return
    val playerBox = player ?: return
    val ui by playerBox.state.collectAsStateWithLifecycle()

    val resolvedUrl = remember(url) {
        if (url.startsWith("http://") || url.startsWith("https://")) url
        else "${BuildConfig.API_BASE_URL.removeSuffix("/")}/${url.removePrefix("/")}"
    }

    val isThis = ui.url == url || ui.url == resolvedUrl
    val isFailed = isThis && ui.failed
    val playing = isThis && ui.playing && !isFailed
    val isBuffering = isThis && ui.buffering && !isFailed
    val durationMs = if (isThis && ui.durationMs > 0) ui.durationMs
    else (card.durationLabel?.let { parseMinSecDuration(it) } ?: 46_000L)
    val positionMs = if (isThis) ui.positionMs else 0L

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
    ) {
        // 51-bar Waveform Scrubber
        AudioWaveformScrubber(
            durationMs = durationMs,
            positionMs = positionMs,
            onSeek = { targetMs -> playerBox.seekTo(targetMs) }
        )

        // Timestamps: left position, right duration (12sp, #A3A29B)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatMinSec(positionMs),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MutedText
            )
            Text(
                text = formatMinSec(durationMs),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MutedText
            )
        }

        Spacer(Modifier.height(8.dp))

        // Controls Row: Save (44dp) | Rewind 15 (44dp) | Play 60dp | Forward 15 (44dp) | Like (44dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Save Bookmark Button
            SaveBookmarkButton(saved = saved, onClick = onToggleSave)

            // Rewind 15s Button
            Rewind15Button(onClick = {
                val newPos = (positionMs - 15_000L).coerceAtLeast(0L)
                playerBox.seekTo(newPos)
            })

            // Center 60dp Play/Pause Button with double-ring and buffering spinner
            CenterPlayButton(
                isPlaying = playing,
                isBuffering = isBuffering,
                onPlayPause = {
                    if (isThis && !isFailed) {
                        playerBox.toggle()
                    } else {
                        playerBox.playUrl(url, card.title)
                    }
                }
            )

            // Forward 15s Button
            Forward15Button(onClick = {
                val newPos = (positionMs + 15_000L).coerceAtMost(durationMs)
                playerBox.seekTo(newPos)
            })

            // Like Heart Button
            LikeHeartButton(liked = liked, onClick = onToggleLike)
        }
    }
}

/**
 * 51-bar waveform with exact SVG coordinates from Main.dc.html.
 * Interactive: tap or drag across the waveform to scrub audio.
 */
@Composable
private fun AudioWaveformScrubber(
    durationMs: Long,
    positionMs: Long,
    onSeek: (Long) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val progressFraction = remember(positionMs, durationMs) {
        if (durationMs > 0) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onSeek((fraction * durationMs).toLong())
                }
            }
            .pointerInput(durationMs) {
                detectHorizontalDragGestures { change, _ ->
                    val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeek((fraction * durationMs).toLong())
                }
            }
    ) {
        val scaleX = size.width / 308f
        val scaleY = size.height / 44f
        val currentProgressX = progressFraction * size.width
        val strokeWidthPx = 3f * scaleX

        // Draw 51 waveform bars
        for (bar in WAVEFORM_BARS) {
            val barX = bar.first * scaleX
            val barY1 = bar.second * scaleY
            val barY2 = bar.third * scaleY
            val isPlayed = barX <= currentProgressX
            val barColor = if (isPlayed) GoldAccent else WaveformUnplayed

            drawLine(
                color = barColor,
                start = Offset(barX, barY1),
                end = Offset(barX, barY2),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )
        }

        // Draw scrubber circle at current progress: radius 6.5dp, fill #F2B632, stroke 2.5dp #1A1A18
        val scrubberRadius = 6.5.dp.toPx()
        val scrubberBorderWidth = 2.5.dp.toPx()
        val scrubberCenter = Offset(
            x = currentProgressX.coerceIn(scrubberRadius, size.width - scrubberRadius),
            y = size.height / 2f
        )
        drawCircle(
            color = GoldAccent,
            radius = scrubberRadius,
            center = scrubberCenter
        )
        drawCircle(
            color = CardBackground,
            radius = scrubberRadius,
            center = scrubberCenter,
            style = Stroke(width = scrubberBorderWidth)
        )
    }
}

/**
 * Center 60dp Play/Pause Button:
 * background #F2B632, box-shadow: 0 0 0 4px #1A1A18, 0 0 0 6px #F2B632.
 * Shows circular spinning indicator while buffering.
 */
@Composable
private fun CenterPlayButton(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPause: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(68.dp)
            .drawBehind {
                // Outer ring: stroke 2dp, color #F2B632
                drawCircle(
                    color = GoldAccent,
                    radius = 33.dp.toPx(),
                    style = Stroke(width = 2.dp.toPx())
                )
                // Gap ring: stroke 4dp, color #1A1A18
                drawCircle(
                    color = CardBackground,
                    radius = 31.dp.toPx(),
                    style = Stroke(width = 4.dp.toPx())
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(GoldAccent)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onPlayPause()
                },
            contentAlignment = Alignment.Center
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = CardBackground,
                    strokeWidth = 2.5.dp
                )
            } else if (isPlaying) {
                // Pause icon: two vertical bars
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.5.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(CardBackground)
                    )
                    Box(
                        modifier = Modifier
                            .width(4.5.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(CardBackground)
                    )
                }
            } else {
                // Play triangle icon (SVG M7 4.5v13l11-6.5z)
                Canvas(modifier = Modifier.size(22.dp)) {
                    val scale = size.width / 22f
                    val path = Path().apply {
                        moveTo(7f * scale, 4.5f * scale)
                        lineTo(7f * scale, 17.5f * scale)
                        lineTo(18f * scale, 11f * scale)
                        close()
                    }
                    drawPath(path, color = CardBackground)
                }
            }
        }
    }
}

/** Rewind 15s Button (44×44dp): Circular counter-clockwise arrow with "15" in center */
@Composable
private fun Rewind15Button(onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            val scale = size.width / 24f
            val path = Path().apply {
                moveTo(5.5f * scale, 3.5f * scale)
                lineTo(5.5f * scale, 8f * scale)
                lineTo(10f * scale, 8f * scale)
            }
            drawPath(
                path = path,
                color = IvoryText,
                style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawArc(
                color = IvoryText,
                startAngle = 180f,
                sweepAngle = 310f,
                useCenter = false,
                topLeft = Offset(4f * scale, 4f * scale),
                size = Size(16f * scale, 16f * scale),
                style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "15",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = IvoryText,
            modifier = Modifier.offset(y = 1.dp)
        )
    }
}

/** Forward 15s Button (44×44dp): Circular clockwise arrow with "15" in center */
@Composable
private fun Forward15Button(onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(28.dp)
                .graphicsLayer { scaleX = -1f }
        ) {
            val scale = size.width / 24f
            val path = Path().apply {
                moveTo(5.5f * scale, 3.5f * scale)
                lineTo(5.5f * scale, 8f * scale)
                lineTo(10f * scale, 8f * scale)
            }
            drawPath(
                path = path,
                color = IvoryText,
                style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawArc(
                color = IvoryText,
                startAngle = 180f,
                sweepAngle = 310f,
                useCenter = false,
                topLeft = Offset(4f * scale, 4f * scale),
                size = Size(16f * scale, 16f * scale),
                style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = "15",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            color = IvoryText,
            modifier = Modifier.offset(y = 1.dp)
        )
    }
}

/** Bookmark / Save Button (44×44dp, vector path M7 4h10v16l-5-3.5L7 20z) */
@Composable
private fun SaveBookmarkButton(
    saved: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val scale = size.width / 24f
            val path = Path().apply {
                moveTo(7f * scale, 4f * scale)
                lineTo(17f * scale, 4f * scale)
                lineTo(17f * scale, 20f * scale)
                lineTo(12f * scale, 16.5f * scale)
                lineTo(7f * scale, 20f * scale)
                close()
            }
            if (saved) {
                drawPath(path, color = GoldAccent)
            } else {
                drawPath(
                    path = path,
                    color = IvoryText,
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

/** Like Heart Button (44×44dp) */
@Composable
private fun LikeHeartButton(
    liked: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current

    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val scale = size.width / 24f
            val path = Path().apply {
                moveTo(12f * scale, 21.35f * scale)
                cubicTo(11.45f * scale, 20.85f * scale, 2f * scale, 12.28f * scale, 2f * scale, 7.5f * scale)
                cubicTo(2f * scale, 4.42f * scale, 4.42f * scale, 2f * scale, 7.5f * scale, 2f * scale)
                cubicTo(9.24f * scale, 2f * scale, 10.91f * scale, 2.81f * scale, 12f * scale, 4.09f * scale)
                cubicTo(13.09f * scale, 2.81f * scale, 14.76f * scale, 2f * scale, 16.5f * scale, 2f * scale)
                cubicTo(19.58f * scale, 2f * scale, 22f * scale, 4.42f * scale, 22f * scale, 7.5f * scale)
                cubicTo(22f * scale, 12.28f * scale, 12.55f * scale, 20.85f * scale, 12f * scale, 21.35f * scale)
                close()
            }
            if (liked) {
                drawPath(path, color = GoldAccent)
            } else {
                drawPath(
                    path = path,
                    color = IvoryText,
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
    }
}

/** Parses duration label like "0:46" or "3:15" into milliseconds */
private fun parseMinSecDuration(label: String): Long {
    val clean = label.replace(Regex("[^0-9:]"), "")
    val parts = clean.split(":")
    val minutes = parts.getOrNull(parts.lastIndex - 1)?.toLongOrNull() ?: return 46_000L
    val seconds = parts.lastOrNull()?.toLongOrNull() ?: return 46_000L
    return (minutes * 60 + seconds) * 1000L
}
