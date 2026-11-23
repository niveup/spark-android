package com.spark.app.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spark.app.data.Card

private val CanvasBackground = Color(0xFF0B0B0A)
private val CardBackground = Color(0xFF1A1A18)
private val GoldAccent = Color(0xFFF2B632)
private val IvoryText = Color(0xFFF3F1EA)
private val MutedText = Color(0xFFA3A29B)
private val Paper = Color(0xFFF3F1EA)
private val PaperSoft = Color(0xFFD2D0C6)
private val Ember = Color(0xFFF2B632)
private val EmberGlow = Color(0xFFF2B632)
private val Display = FontFamily.Serif

@Composable
fun DoneScreen(
    reason: FinishReason?,
    seenCount: Int,
    savedCards: List<Card>,
    onCloseApp: () -> Unit,
    onReadAgain: () -> Unit = {},
    deckRefreshState: DeckRefreshState = DeckRefreshState.Idle,
    modifier: Modifier = Modifier
) {
    var showSaved by remember { mutableStateOf(false) }
    var actionDone by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    val topAction = remember(savedCards) {
        savedCards.firstOrNull { it.actionLine.isNotBlank() }?.actionLine
    }

    // A short entrance cue; no haptic plays just because the limit was reached.
    val starScale = remember { Animatable(0.3f) }
    val contentAlpha = remember { Animatable(0f) }
    val contentTranslateY = remember { Animatable(24f) }

    LaunchedEffect(Unit) {
        starScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    LaunchedEffect(Unit) {
        contentAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(Unit) {
        contentTranslateY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Celebratory Spark Starburst Emblem
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        scaleX = starScale.value
                        scaleY = starScale.value
                    },
                contentAlignment = Alignment.Center
            ) {
                CelebrationEmblem()
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(
                modifier = Modifier.graphicsLayer {
                    alpha = contentAlpha.value
                    translationY = contentTranslateY.value
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SESSION COMPLETE",
                    fontSize = 11.sp,
                    letterSpacing = 2.0.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ember
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "You're done.",
                    fontFamily = Display,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = PaperSoft,
                    modifier = Modifier.semantics { heading() }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = when (reason) {
                        FinishReason.CARD_LIMIT -> "You hit your planned reading limit. Clean stop."
                        FinishReason.TIME_LIMIT -> "Time's up for this focus session."
                        else -> "You digested the entire stack of ideas."
                    },
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Paper.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Stats Chips Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetricChip(
                        icon = "✓",
                        label = "$seenCount read",
                        accent = false
                    )
                    MetricChip(
                        icon = "★",
                        label = "${savedCards.size} saved",
                        accent = savedCards.isNotEmpty()
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Action Callout Card (if present)
                if (topAction != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Paper.copy(alpha = 0.08f))
                            .border(1.dp, Paper.copy(alpha = 0.16f), RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Ember)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "YOUR EXPERIMENT TODAY",
                                    fontSize = 12.sp,
                                    letterSpacing = 0.8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Ember
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = topAction,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                color = PaperSoft
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (actionDone) Ember.copy(alpha = 0.2f) else Paper.copy(alpha = 0.12f))
                                    .toggleable(
                                        value = actionDone,
                                        role = Role.Checkbox
                                    ) { checked ->
                                        actionDone = checked
                                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = if (actionDone) "✓ Committed to do this today!" else "Commit to this action today",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (actionDone) EmberGlow else PaperSoft
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Saved Cards Accordion
                if (savedCards.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Paper.copy(alpha = 0.06f))
                            .clickable(role = Role.Button) {
                                showSaved = !showSaved
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            }
                            .semantics(mergeDescendants = true) {
                                stateDescription = if (showSaved) "Expanded" else "Collapsed"
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showSaved) "Hide saved ideas" else "Review saved ideas (${savedCards.size})",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = PaperSoft
                            )
                            Text(
                                text = if (showSaved) "▲" else "▼",
                                fontSize = 11.sp,
                                color = Paper.copy(alpha = 0.6f)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showSaved,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            savedCards.forEach { card ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "★",
                                        fontSize = 12.sp,
                                        color = Ember,
                                        modifier = Modifier.padding(top = 2.dp, end = 8.dp)
                                    )
                                    Column {
                                        Text(
                                            text = card.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PaperSoft
                                        )
                                        if (card.actionLine.isNotBlank()) {
                                            Text(
                                                text = card.actionLine,
                                                fontSize = 13.sp,
                                                lineHeight = 19.sp,
                                                color = Paper.copy(alpha = 0.65f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                OutlinedButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onReadAgain()
                    },
                    enabled = deckRefreshState !is DeckRefreshState.Refreshing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    if (deckRefreshState is DeckRefreshState.Refreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = PaperSoft
                        )
                        Spacer(Modifier.width(9.dp))
                    }
                    Text(
                        text = if (deckRefreshState is DeckRefreshState.Refreshing) {
                            "Refreshing your deck…"
                        } else {
                            "Read this deck again"
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }

                when (val refresh = deckRefreshState) {
                    is DeckRefreshState.Failed -> Text(
                        text = refresh.message,
                        fontSize = 12.sp,
                        color = EmberGlow,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    is DeckRefreshState.NoCards -> Text(
                        text = "No cards are available to read again right now.",
                        fontSize = 12.sp,
                        color = Paper.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    else -> Unit
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary Exit Button
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onCloseApp()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GoldAccent,
                        contentColor = CardBackground
                    )
                ) {
                    Text(
                        text = "Close app",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Finite by design · No infinite feed · You're in control",
                    fontSize = 12.sp,
                    letterSpacing = 0.3.sp,
                    color = Paper.copy(alpha = 0.45f)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MetricChip(
    icon: String,
    label: String,
    accent: Boolean
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(if (accent) Ember.copy(alpha = 0.18f) else Paper.copy(alpha = 0.08f))
            .border(
                1.dp,
                if (accent) Ember.copy(alpha = 0.5f) else Paper.copy(alpha = 0.18f),
                RoundedCornerShape(100.dp)
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            fontSize = 12.sp,
            color = if (accent) Ember else Paper.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (accent) PaperSoft else Paper.copy(alpha = 0.8f)
        )
    }
}

/** Draws an authentic geometric golden Spark star with a radiant halo. */
@Composable
private fun CelebrationEmblem() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.width * 0.44f
        val innerRadius = size.width * 0.18f

        // Soft outer ambient halo
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Ember.copy(alpha = 0.25f), Color.Transparent),
                center = center,
                radius = size.width * 0.5f
            ),
            radius = size.width * 0.5f,
            center = center
        )

        // Delicate ring outline
        drawCircle(
            color = Ember.copy(alpha = 0.4f),
            radius = outerRadius * 0.95f,
            center = center,
            style = Stroke(width = 1.2.dp.toPx())
        )

        // 8-Point geometric Spark Star
        val path = Path()
        val points = 8
        val step = Math.PI / points
        for (i in 0 until (points * 2)) {
            val r = if (i % 2 == 0) outerRadius * 0.82f else innerRadius
            val angle = i * step - Math.PI / 2
            val x = (center.x + r * kotlin.math.cos(angle)).toFloat()
            val y = (center.y + r * kotlin.math.sin(angle)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        drawPath(
            path = path,
            color = Ember
        )

        // Inner glowing core
        drawCircle(
            color = PaperSoft,
            radius = innerRadius * 0.45f,
            center = center
        )
    }
}
