package com.spark.app.home

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
 * One surface on the screen: the card you are about to read. Everything else
 * is type on the ground. The screen answers one question first — "what next?" —
 * then how far into today's finite session you are, then what you kept.
 *
 * Hierarchy, in order: the wordmark (drawn), the greeting (grey + amber name),
 * the card, the session bar, saved, today's line.
 */

private val SparkBg = Color(0xFF0E0E11)
private val SparkCard = Color(0xFF191921)
private val SparkCardBorder = Color(0xFF2C2C38)
private val SparkTrack = Color(0xFF4A4A56)

private val SparkGold = Color(0xFFF5A844)

private val TextPrimary = Color(0xFFF4F4F6)
private val TextSecondary = Color(0xFFA1A1AA)
private val TextTertiary = Color(0xFF8C8C96)

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
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 28.sp,
    letterSpacing = (-0.15).sp
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
    fontFamily = FontFamily.SansSerif,
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
        Spacer(modifier = Modifier.height(26.dp))

        DatelineHeader(dayName = dayName, dateLabel = dateLabel)

        Spacer(modifier = Modifier.height(30.dp))

        Greeting(greeting = greeting)

        when {
            nextCard != null -> {
                Spacer(modifier = Modifier.height(26.dp))
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

        Spacer(modifier = Modifier.height(36.dp))

        SessionBar(seenCount = seenCount, total = sessionTotal)

        if (savedCount > 0) {
            Spacer(modifier = Modifier.height(18.dp))
            SavedRow(count = savedCount, onClick = onOpenSaved)
        }

        if (dailyQuote != null) {
            Spacer(modifier = Modifier.height(46.dp))
            TodayLine(card = dailyQuote)
        }

        Spacer(modifier = Modifier.height(32.dp))
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

/** Context in grey, the person in amber: the only warm thing above the card. */
@Composable
private fun Greeting(greeting: String) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = TextSecondary, fontWeight = FontWeight.Medium)) {
                append("$greeting, ")
            }
            withStyle(SpanStyle(color = SparkGold, fontWeight = FontWeight.SemiBold)) {
                append(OWNER_NAME)
            }
        },
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.1).sp
    )
}

/**
 * The screen's one block: the book's own words, carrying the accent rule, then
 * the instruction in the app's voice, then the citation and the way in.
 */
@Composable
private fun NextCard(card: Card, onClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(18.dp)

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
        Column(modifier = Modifier.padding(22.dp)) {
            Text(text = "From the book", style = LabelStyle, color = TextTertiary)

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(SparkGold)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = card.body.displayBody().asQuote(),
                    style = QuoteStyle,
                    color = TextPrimary
                )
            }

            if (card.actionLine.isNotBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = actionText(card.actionLine),
                    style = ActionStyle,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
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
                Spacer(modifier = Modifier.width(12.dp))
                ReadItAffordance()
            }
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

/** One 2px bar is the whole progress story: read against the session's size. */
@Composable
private fun SessionBar(seenCount: Int, total: Int) {
    val fraction = if (total <= 0) 0f else (seenCount.toFloat() / total).coerceIn(0f, 1f)

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .clip(RoundedCornerShape(1.dp))
                .background(SparkTrack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(SparkGold)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "$seenCount of $total read",
            style = CaptionStyle,
            color = TextSecondary
        )
    }
}

/** A quiet line, not a second card: the box count stays at one. */
@Composable
private fun SavedRow(count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Saved", style = BodyStyle, color = TextSecondary)
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = count.toString(),
            style = BodyStyle.copy(fontWeight = FontWeight.SemiBold),
            color = TextPrimary
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(14.dp)
        )
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

    val title = when {
        complete -> "That\u2019s today\u2019s $total."
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
        else -> "Open the feed to load today\u2019s cards."
    }

    Column {
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

        if (complete) {
            TextButton(
                onClick = { (context as? Activity)?.finishAffinity() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Close app", style = CiteStyle.copy(fontSize = 13.sp), color = TextTertiary)
            }
        }
    }
}

/** The closing beat: one line to keep, and where it came from. */
@Composable
private fun TodayLine(card: Card) {
    Column {
        Text(text = "Today\u2019s line", style = LabelStyle, color = TextTertiary)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = card.body.displayBody().asQuote(),
            style = DailyQuoteStyle,
            color = TextPrimary
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
                Text(text = attribution, style = CiteStyle, color = TextTertiary)
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
