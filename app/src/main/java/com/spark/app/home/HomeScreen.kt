package com.spark.app.home

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
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
 * Everything shown comes from the local feed/session state. The small cover
 * art is drawn with Compose shapes, so Home remains useful offline.
 */

private val SparkBg = Color(0xFF121110)
private val SparkCard = Color(0xFF1E1C1B)
private val SparkCardBorder = Color(0xFF312D2A)
private val SparkTrack = Color(0xFF4C4540)

private val SparkGold = Color(0xFFF5A844)

private val TextPrimary = Color(0xFFECE6E0)
private val TextSecondary = Color(0xFFC3BBB1)
private val TextTertiary = Color(0xFF9C9289)

private val Gutter = 20.dp
private val CardCornerRadius = 20.dp

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

private val ReadItStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    lineHeight = 18.sp
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

private val DoneTitleStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 21.sp,
    lineHeight = 27.sp,
    letterSpacing = (-0.3).sp
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
            .background(SparkBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Gutter)
    ) {
        Spacer(modifier = Modifier.height(22.dp))

        DatelineHeader(dayName = dayName, dateLabel = dateLabel)

        Spacer(modifier = Modifier.height(26.dp))

        Greeting(greeting = greeting)

        when {
            nextCard != null -> {
                Spacer(modifier = Modifier.height(22.dp))
                NextCard(card = nextCard, onClick = onOpenReels)
            }

            else -> {
                Spacer(modifier = Modifier.height(28.dp))
                DoneBlock(
                    total = sessionTotal,
                    complete = complete,
                    hasCards = hasCards,
                    savedCount = savedCount,
                    ctaLabel = ctaLabel,
                    onCta = ctaAction
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

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
            text = "Make a little room for a better idea.",
            fontSize = 23.sp,
            lineHeight = 29.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.45).sp,
            color = TextPrimary,
            modifier = Modifier.semantics { heading() }
        )
    }
}

/** The featured card pairs a real deck item with an offline-drawn cover. */
@Composable
private fun NextCard(card: Card, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(CardCornerRadius)

    Surface(
        shape = shape,
        color = SparkCard,
        border = BorderStroke(1.dp, SparkCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "YOUR NEXT IDEA", style = LabelStyle, color = SparkGold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = card.title,
                        fontSize = 20.sp,
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.25).sp,
                        color = TextPrimary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TopicChip(topic = card.topic, type = card.type.name)
                }
                Spacer(modifier = Modifier.width(14.dp))
                BookCoverArt(title = card.bookLine ?: card.title, topic = card.topic)
            }

            Spacer(modifier = Modifier.height(15.dp))

            Text(
                text = card.body.displayBody().asQuote(),
                style = QuoteStyle,
                color = TextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (card.actionLine.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = actionText(card.actionLine),
                    style = ActionStyle,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val source = card.bookLine ?: card.source
                Text(
                    text = source,
                    style = LabelStyle,
                    color = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
                ReadItAffordance()
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

/** A tiny editorial cover made from Compose shapes, so it works offline. */
@Composable
private fun BookCoverArt(title: String, topic: String) {
    Box(
        modifier = Modifier
            .size(width = 84.dp, height = 118.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF60411F), Color(0xFF2C2119))
                )
            )
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
                .size(36.dp)
                .background(Color(0x26FFD59A), CircleShape)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(62.dp)
                .align(Alignment.CenterStart)
                .background(SparkGold.copy(alpha = 0.75f))
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 11.dp, end = 8.dp, bottom = 10.dp)
        ) {
            Text(
                text = topic.uppercase(Locale.ROOT),
                fontSize = 8.sp,
                lineHeight = 10.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFFD7A6),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = title,
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFFF2DF),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReadItAffordance() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "Read it", style = ReadItStyle, color = TextPrimary)
        Spacer(modifier = Modifier.width(5.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = SparkGold,
            modifier = Modifier.size(13.dp)
        )
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
        shape = RoundedCornerShape(18.dp),
        color = SparkCard,
        border = BorderStroke(1.dp, SparkCardBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "TODAY'S RHYTHM", style = LabelStyle, color = TextTertiary)
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Color(0xFF30271E)
                ) {
                    Text(
                        text = "$minuteLimit MIN",
                        style = LabelStyle.copy(fontSize = 10.sp),
                        color = SparkGold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$seenCount",
                    fontSize = 29.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.6).sp,
                    color = TextPrimary
                )
                Text(
                    text = if (total > 0) " / $total cards" else " cards",
                    style = CaptionStyle,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 3.dp, bottom = 3.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = when {
                        isComplete -> "Session complete"
                        isCaughtUp -> "All caught up"
                        else -> "${(total - seenCount).coerceAtLeast(0)} left"
                    },
                    style = CaptionStyle.copy(fontSize = 11.5.sp),
                    color = if (isComplete) SparkGold else TextTertiary,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(SparkTrack)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(SparkGold)
                )
            }
        }
    }
}

/** A tappable preview of the real saved-card list. */
@Composable
private fun SavedRow(count: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Surface(
        shape = shape,
        color = SparkCard,
        border = BorderStroke(1.dp, SparkCardBorder),
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
                    .background(Color(0xFF30271E), CircleShape),
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
                color = Color(0xFF30271E)
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

/** The end of a session, on the ground: a statement, a line, one action. */
@Composable
private fun DoneBlock(
    total: Int,
    complete: Boolean,
    hasCards: Boolean,
    savedCount: Int,
    ctaLabel: String,
    onCta: () -> Unit
) {
    val context = LocalContext.current

    val isCaughtUp = total <= 0
    val isDone = complete || isCaughtUp
    val title = when {
        complete -> "That\u2019s today\u2019s $total."
        isCaughtUp -> "You\u2019re all caught up."
        hasCards -> "Nothing to show yet."
        else -> "No cards in the deck."
    }
    val body = when {
        complete && savedCount > 0 ->
            "Nothing left to scroll. That\u2019s the point. Your $savedCount saved " +
                "${if (savedCount == 1) "card is" else "cards are"} waiting, and tomorrow\u2019s " +
                "session starts fresh."
        complete ->
            "Nothing left to scroll. That\u2019s the point. Tomorrow\u2019s session starts fresh."
        isCaughtUp ->
            "No endless queue, no doomscrolling. You\u2019ve read today\u2019s batch \u2014 take a breath and step away, or revisit your saved notes."
        else -> "Open the feed to load today\u2019s cards."
    }

    Surface(
        shape = RoundedCornerShape(CardCornerRadius),
        color = SparkCard,
        border = BorderStroke(1.dp, SparkCardBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = if (isDone) "TODAY'S SESSION" else "GET STARTED",
                style = LabelStyle,
                color = SparkGold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = DoneTitleStyle,
                color = TextPrimary,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = body,
                style = BodyStyle,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onCta,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TextPrimary,
                    contentColor = SparkBg
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
                        fontSize = 14.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
            }

            if (isDone) {
                TextButton(
                    onClick = { (context as? Activity)?.finishAffinity() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Close app", style = CiteStyle.copy(fontSize = 13.sp), color = TextTertiary)
                }
            }
        }
    }
}

/** A highlighted quote from the real deck, with its source kept in view. */
@Composable
private fun TodayLine(card: Card) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF211E1A),
        border = BorderStroke(1.dp, SparkCardBorder)
    ) {
        Box {
            Text(
                text = "“",
                fontFamily = FontFamily.Serif,
                fontSize = 76.sp,
                lineHeight = 76.sp,
                color = SparkGold.copy(alpha = 0.12f),
                modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp)
            )
            Column(modifier = Modifier.padding(18.dp)) {
                Text(text = "A THOUGHT TO KEEP", style = LabelStyle, color = SparkGold)
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
