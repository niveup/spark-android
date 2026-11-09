package com.spark.app.home

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.spark.app.R
import com.spark.app.data.Card
import com.spark.app.data.CardType
import com.spark.app.ui.theme.SparkTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * SPARK Home.
 *
 * Home is a reading dashboard: a real next card, a finite-session snapshot,
 * and shortcuts to content the reader has already saved.
 *
 * Feed/session facts come from local state. The hero illustration is a native
 * Android vector drawable, so it stays crisp at every screen density.
 */


private val SparkGold = Color(0xFFF5A844)

private val TextPrimary = Color(0xFFECE6E0)
private val TextSecondary = Color(0xFFC3BBB1)
private val TextTertiary = Color(0xFF9C9289)

private val Gutter = 20.dp

/** The one person this app is for. */
private const val OWNER_NAME = "Satyam"

private val LabelStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Medium,
    fontSize = 11.5.sp,
    lineHeight = 15.sp,
    letterSpacing = 0.1.sp
)

private val QuoteStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Normal,
    fontSize = 17.5.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.1).sp
)

private val ActionStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 13.5.sp,
    lineHeight = 20.sp
)

private val CaptionStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 12.5.sp,
    lineHeight = 18.sp
)

private val DailyQuoteStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Normal,
    fontSize = 17.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.1).sp
)

private val CiteStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 16.sp
)

private val BodyStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 22.sp
)

private val DatelineDayStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Medium,
    fontSize = 11.5.sp,
    lineHeight = 14.sp
)

private val DatelineDateStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 14.sp,
    lineHeight = 18.sp,
    letterSpacing = (-0.1).sp
)

@Composable
fun HomeScreen(
    seenCount: Int,
    savedCount: Int,
    cardLimit: Int,
    minuteLimit: Int,
    modifier: Modifier = Modifier,
    deckSize: Int? = null,
    nextCard: Card? = null,
    dailyQuote: Card? = null,
    onOpenReels: () -> Unit,
    onOpenSaved: () -> Unit
) {
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..21 -> "Good evening"
            else -> "Late night"
        }
    }
    val today = remember { LocalDate.now() }
    val dayName = remember {
        today.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()))
    }
    val dateLabel = remember {
        today.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
    }

    val sessionTotal = when {
        deckSize == null -> cardLimit
        deckSize <= 0 -> 0
        else -> deckSize.coerceAtMost(cardLimit)
    }
    val hasCards = sessionTotal > 0
    val complete = hasCards && seenCount >= sessionTotal

    val ctaLabel = when {
        !hasCards -> "Open feed"
        complete && savedCount > 0 -> "Review saved cards"
        complete -> "Open feed"
        seenCount > 0 -> "Continue session"
        else -> "Start today's session"
    }
    val ctaAction = if (complete && savedCount > 0) onOpenSaved else onOpenReels

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF101614),
                        Color(0xFF12151A),
                        Color(0xFF171513)
                    )
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Gutter)
    ) {
        Spacer(modifier = Modifier.height(22.dp))

        DatelineHeader(dayName = dayName, dateLabel = dateLabel)

        Spacer(modifier = Modifier.height(26.dp))

        Greeting(greeting = greeting)
        Spacer(modifier = Modifier.height(18.dp))

        HomeHero(
            card = nextCard,
            seenCount = seenCount,
            total = sessionTotal,
            minuteLimit = minuteLimit,
            complete = complete,
            hasCards = hasCards,
            ctaLabel = ctaLabel,
            onCta = ctaAction
        )

        Spacer(modifier = Modifier.height(16.dp))

        SessionBar(seenCount = seenCount, total = sessionTotal, minuteLimit = minuteLimit)

        Spacer(modifier = Modifier.height(12.dp))
        SavedRow(count = savedCount, onClick = onOpenSaved)

        if (dailyQuote != null) {
            Spacer(modifier = Modifier.height(12.dp))
            TodayLine(card = dailyQuote)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** Wordmark on the left, a two-line dateline on the right, both on one baseline. */
@Composable
private fun DatelineHeader(dayName: String, dateLabel: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_spark_wordmark),
            contentDescription = "SPARK",
            modifier = Modifier.height(21.dp)
        )

        Column(horizontalAlignment = Alignment.End) {
            Text(text = dayName, style = DatelineDayStyle, color = TextTertiary)
            Spacer(modifier = Modifier.height(3.dp))
            Text(text = dateLabel, style = DatelineDateStyle, color = TextPrimary)
        }
    }
}

/** A short welcome plus a clear statement of what this screen is for. */
@Composable
private fun Greeting(greeting: String) {
    Column {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = TextSecondary, fontWeight = FontWeight.Medium)) {
                    append("$greeting, ")
                }
                withStyle(SpanStyle(color = SparkGold, fontWeight = FontWeight.SemiBold)) {
                    append(OWNER_NAME)
                }
            },
            fontSize = 16.sp,
            lineHeight = 22.sp,
            letterSpacing = 0.sp
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = "Your reading space is ready.",
            fontSize = 15.sp,
            lineHeight = 21.sp,
            color = TextTertiary
        )
    }
}

/** A vivid lead panel, with a real deck item and an expandable reading peek. */
@Composable
private fun HomeHero(
    card: Card?,
    seenCount: Int,
    total: Int,
    minuteLimit: Int,
    complete: Boolean,
    hasCards: Boolean,
    ctaLabel: String,
    onCta: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var expanded by rememberSaveable(card?.id) { mutableStateOf(false) }
    val shape = RoundedCornerShape(26.dp)

    Surface(
        shape = shape,
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color(0x4D9BC6B4)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF183A34),
                            Color(0xFF244C45),
                            Color(0xFF3E4537)
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 24.dp)
                    .size(152.dp)
                    .background(Color(0x1EF4B84F), CircleShape)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 92.dp, bottom = 42.dp)
                    .size(100.dp)
                    .background(Color(0x1477C6AF), CircleShape)
            )

            Column(
                modifier = Modifier
                    .animateContentSize(
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(SparkGold, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (card == null) "YOUR DAILY SPARK" else "UP NEXT",
                        style = LabelStyle.copy(letterSpacing = 1.sp),
                        color = Color(0xFFFFD99B)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (total > 0) "$seenCount / $total · $minuteLimit MIN" else "READY WHEN YOU ARE",
                        style = LabelStyle.copy(fontSize = 10.sp),
                        color = Color(0xFFC8DDD0)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = card?.title ?: when {
                                complete -> "You did enough for today."
                                !hasCards -> "A new idea is on its way."
                                else -> "Your next idea is waiting."
                            },
                            fontSize = 23.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.45).sp,
                            color = Color(0xFFFFF5E8),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.semantics { heading() }
                        )
                        Spacer(modifier = Modifier.height(9.dp))
                        if (card != null) {
                            TopicChip(topic = card.topic, type = card.type.name)
                        } else {
                            Text(
                                text = if (complete) {
                                    "Your finite session is complete. Come back fresh tomorrow."
                                } else {
                                    "Open your feed for a short idea and one useful next step."
                                },
                                style = BodyStyle.copy(fontSize = 13.sp, lineHeight = 19.sp),
                                color = Color(0xFFD1E0D7),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    val bookLine = card?.bookLine.orEmpty()
                    val bookTitle = bookLine.substringBefore('\u2014').trim()
                        .ifBlank { card?.title ?: "A thought to keep" }
                    val bookAuthor = bookLine.substringAfter('\u2014', "").trim()
                    FloatingBookArtwork(title = bookTitle, author = bookAuthor)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCta()
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SparkGold,
                            contentColor = Color(0xFF241B0C)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Text(
                            text = ctaLabel,
                            style = TextStyle(
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.5.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    if (card != null) {
                        TextButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = if (expanded) "Hide peek" else "Peek inside",
                                style = LabelStyle.copy(fontSize = 11.sp),
                                color = Color(0xFFFFF5E8),
                                maxLines = 1
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = expanded && card != null,
                    enter = fadeIn(tween(180)) +
                        slideInVertically(
                            animationSpec = tween(240, easing = FastOutSlowInEasing),
                            initialOffsetY = { height -> height / 10 }
                        ) + expandVertically(tween(240, easing = FastOutSlowInEasing)),
                    exit = fadeOut(tween(140)) +
                        slideOutVertically(
                            animationSpec = tween(170, easing = FastOutSlowInEasing),
                            targetOffsetY = { height -> height / 12 }
                        ) + shrinkVertically(tween(170, easing = FastOutSlowInEasing))
                ) {
                    card?.let { preview ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0x33243C35))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = preview.body.displayBody().asQuote(),
                                style = QuoteStyle.copy(fontSize = 16.sp, lineHeight = 23.sp),
                                color = Color(0xFFFFF5E8),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (preview.actionLine.isNotBlank()) {
                                Spacer(modifier = Modifier.height(9.dp))
                                Text(
                                    text = actionText(preview.actionLine),
                                    style = ActionStyle.copy(fontSize = 12.5.sp),
                                    color = Color(0xFFD1E0D7),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = preview.bookLine ?: preview.source,
                                style = LabelStyle.copy(fontSize = 10.5.sp),
                                color = Color(0xFFFFD99B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (complete || !hasCards) {
                    TextButton(
                        onClick = { (context as? Activity)?.finishAffinity() },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                    ) {
                        Text(
                            text = "Close app",
                            style = CiteStyle.copy(fontSize = 13.sp),
                            color = Color(0xFFC8DDD0)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicChip(topic: String, type: String) {
    Surface(
        color = SparkGold.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50.dp)
    ) {
        Text(
            text = "${topic.uppercase(Locale.ROOT)}  ·  ${typeLabel(type)}",
            style = LabelStyle.copy(fontSize = 9.5.sp, letterSpacing = 0.65.sp),
            color = SparkGold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

private fun typeLabel(type: String): String =
    type.replace('_', ' ').uppercase(Locale.ROOT)

/** A small, typeset hardcover with visible binding, paper edges, and a quiet floating motion. */
@Composable
private fun FloatingBookArtwork(title: String, author: String) {
    val motion = rememberInfiniteTransition(label = "home-book-float")
    val verticalDrift by motion.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "book-vertical-drift"
    )
    val tilt by motion.animateFloat(
        initialValue = -1.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "book-tilt"
    )

    Box(
        modifier = Modifier
            .size(width = 138.dp, height = 122.dp)
            .graphicsLayer { rotationZ = tilt }
            .offset { IntOffset(x = 0, y = verticalDrift.dp.roundToPx()) },
        contentAlignment = Alignment.Center
    ) {
        val bookShape = RoundedCornerShape(3.dp)
        val pageShape = RoundedCornerShape(4.dp)

        // The offset cream block gives the cover a real page edge and a little thickness.
        Box(
            modifier = Modifier
                .offset(x = 5.dp, y = 3.dp)
                .size(width = 98.dp, height = 108.dp)
                .clip(pageShape)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFFD6C7A7), Color(0xFFFFF5DD), Color(0xFFD8C9A8))
                    )
                )
                .border(1.dp, Color(0xFFA99B80), pageShape)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(5.dp)
                    .fillMaxHeight()
                    .padding(vertical = 5.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                repeat(28) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(Color(0x557A6850))
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .size(width = 96.dp, height = 106.dp)
                .shadow(elevation = 8.dp, shape = bookShape)
                .clip(bookShape)
                .background(Color(0xFF17352F))
        ) {
            // The rounded spine and its fine bands make the object read as a bound volume.
            Column(
                modifier = Modifier
                    .width(13.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF102721), Color(0xFF244A40), Color(0xFF18372F))
                        )
                    )
                    .padding(vertical = 7.dp, horizontal = 3.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(2) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color(0xFFB99A60).copy(alpha = 0.7f))
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF315D50), Color(0xFF244A40), Color(0xFF19372F))
                        )
                    )
                    .border(1.dp, Color(0xFFB99A60).copy(alpha = 0.55f), bookShape)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 9.dp, end = 6.dp, top = 9.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SPARK  /  READING EDITION",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 5.5.sp,
                        letterSpacing = 0.55.sp,
                        color = Color(0xFFE0C78E),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )

                    Text(
                        text = title,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        letterSpacing = (-0.15).sp,
                        color = Color(0xFFFFF4DC),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(1.dp)
                                .background(Color(0xFFE0C78E))
                        )
                        Text(
                            text = author.ifBlank { "FIELD NOTES  ·  01" },
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 6.sp,
                            letterSpacing = 0.35.sp,
                            color = Color(0xFFE0C78E),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** A compact dashboard card for the session limit and actual reading progress. */
@Composable
private fun SessionBar(seenCount: Int, total: Int, minuteLimit: Int) {
    val isComplete = total > 0 && seenCount >= total
    val isCaughtUp = total <= 0
    val fraction = when {
        isComplete -> 1f
        isCaughtUp -> 0f
        else -> (seenCount.toFloat() / total).coerceIn(0f, 1f)
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFF202C2A),
        border = BorderStroke(1.dp, Color(0xFF344A42))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProgressRing(
                progress = fraction,
                value = seenCount,
                description = "$seenCount of $total cards read"
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Today's rhythm",
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (total > 0) "$seenCount of $total cards" else "All caught up",
                    style = CaptionStyle,
                    color = Color(0xFFC7D8D0)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when {
                        isComplete -> "Session complete · $minuteLimit min goal"
                        isCaughtUp -> "Take a breather. Your deck is clear."
                        else -> "${(total - seenCount).coerceAtLeast(0)} left · $minuteLimit min goal"
                    },
                    style = CaptionStyle.copy(fontSize = 11.5.sp),
                    color = if (isComplete) SparkGold else Color(0xFF9CB8AA),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = Color(0xFF394739)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$minuteLimit",
                        fontSize = 18.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFFD99B)
                    )
                    Text(
                        text = "MIN",
                        style = LabelStyle.copy(fontSize = 8.5.sp, letterSpacing = 0.8.sp),
                        color = Color(0xFFC7D8D0)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressRing(progress: Float, value: Int, description: String) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "home-session-progress"
    )
    val track = Color(0xFF52665D)

    Box(
        modifier = Modifier
            .size(76.dp)
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(animatedProgress, 0f..1f)
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 7.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = SparkGold,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = value.toString(),
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
    }
}

/** A tappable preview of the real saved-card list. */
@Composable
private fun SavedRow(count: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        shape = shape,
        color = Color(0xFF2A2531),
        border = BorderStroke(1.dp, Color(0xFF493C54)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(horizontal = 15.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFF45394F), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = SparkGold,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Your saved shelf",
                    style = BodyStyle.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (count == 0) "Keep the ideas you want to revisit" else "$count ${if (count == 1) "idea" else "ideas"} to revisit",
                    style = CaptionStyle,
                    color = TextTertiary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(50.dp),
                color = Color(0xFF45394F)
            ) {
                Text(
                    text = count.toString(),
                    style = BodyStyle.copy(fontWeight = FontWeight.SemiBold),
                    color = SparkGold,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/** A highlighted quote from the real deck, with its source kept in view. */
@Composable
private fun TodayLine(card: Card) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF242033),
        border = BorderStroke(1.dp, Color(0xFF453A58))
    ) {
        Box {
            Text(
                text = "“",
                fontFamily = FontFamily.Serif,
                fontSize = 76.sp,
                lineHeight = 76.sp,
                color = Color(0xFFD8B8F0).copy(alpha = 0.15f),
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp)
            )
            Column(modifier = Modifier.padding(18.dp)) {
                Text(text = "A THOUGHT TO KEEP", style = LabelStyle, color = Color(0xFFD8B8F0))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = card.body.displayBody().asQuote(),
                    style = DailyQuoteStyle,
                    color = TextPrimary,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
                val attribution = card.bookLine
                if (!attribution.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .height(2.dp)
                                .background(SparkGold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = attribution,
                            style = CiteStyle,
                            color = TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * The ingest stores a scaffolding line at the head of book cards; it is not
 * part of the reading surface, so it never reaches the screen.
 */
private fun String.displayBody(): String =
    removePrefix("Opening lines of this public-domain classic:")
        .trim()
        .ifBlank { this }

/** Curly quotes, exactly once. */
private fun String.asQuote(): String {
    val trimmed = trim().trim('"', '\u201C', '\u201D')
    return "\u201C$trimmed\u201D"
}

/** "Try this today:" in the app's voice, the instruction itself in the text tone. */
private fun actionText(line: String) = buildAnnotatedString {
    val split = line.indexOf(':')
    if (split in 1..40) {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = TextPrimary)) {
            append(line.substring(0, split + 1))
        }
        append(" " + line.substring(split + 1).trim())
    } else {
        append(line)
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, backgroundColor = 0xFF0E0E11)
@Composable
private fun HomeScreenPreview() {
    SparkTheme {
        HomeScreen(
            seenCount = 7,
            savedCount = 3,
            cardLimit = 20,
            minuteLimit = 15,
            deckSize = 20,
            nextCard = Card(
                id = "preview-next",
                type = CardType.BOOK_IDEA,
                topic = "mindset",
                title = "Why read As a Man Thinketh",
                body = "Opening lines of this public-domain classic:\n\nThe aphorism, \u201CAs a man " +
                    "thinketh in his heart so is he,\u201D not only embraces the whole of a man\u2019s " +
                    "being, but is so comprehensive as to reach out to every condition and " +
                    "circumstance of his life.",
                actionLine = "Try this today: pick the one thought you will refuse to repeat this week.",
                source = "ebook #4507",
                grounded = true,
                bookLine = "As a Man Thinketh \u2014 James Allen"
            ),
            dailyQuote = Card(
                id = "preview-quote",
                type = CardType.QUOTE,
                topic = "mindset",
                title = "Marcus Aurelius",
                body = "The happiness of your life depends upon the quality of your thoughts.",
                actionLine = "",
                source = "Meditations",
                grounded = true,
                bookLine = "Marcus Aurelius \u2014 Meditations"
            ),
            onOpenReels = {},
            onOpenSaved = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844, backgroundColor = 0xFF0E0E11)
@Composable
private fun HomeScreenCompletePreview() {
    SparkTheme {
        HomeScreen(
            seenCount = 20,
            savedCount = 3,
            cardLimit = 20,
            minuteLimit = 15,
            deckSize = 20,
            nextCard = null,
            dailyQuote = Card(
                id = "preview-quote",
                type = CardType.QUOTE,
                topic = "mindset",
                title = "Marcus Aurelius",
                body = "The happiness of your life depends upon the quality of your thoughts.",
                actionLine = "",
                source = "Meditations",
                grounded = true,
                bookLine = "Marcus Aurelius \u2014 Meditations"
            ),
            onOpenReels = {},
            onOpenSaved = {}
        )
    }
}
