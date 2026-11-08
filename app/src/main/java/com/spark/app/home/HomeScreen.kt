package com.spark.app.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import kotlin.math.roundToInt

/*
 * Home is the front page of a finite daily edition, not a dashboard.
 *
 * The idea: the twenty cards of a session become a row of book spines, and a
 * real photograph of someone reading becomes the cover. The only colour is
 * amber, and it only marks data (read cards, card type, completion) — never
 * buttons, navigation or decoration.
 *
 * This screen deliberately has no gradients, glows, borders or shadows. A
 * surface is one lightness step above the page; a card is separated by tone,
 * not by a line.
 */

private val SparkBg = Color(0xFF0E0D0C)
private val SparkRaise = Color(0xFF181614)
private val SparkRaise2 = Color(0xFF242220)
private val SparkGold = Color(0xFFFFB454)
private val SparkText = Color(0xFFECE6E0)
private val SparkMuted = Color(0xFFB8ADA0)
private val SparkFaint = Color(0xFF8F8274)

private val Gutter = 20.dp

private val DisplayStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Bold,
    fontSize = 34.sp,
    lineHeight = 39.sp,
    letterSpacing = (-0.6).sp
)

private val CardTitleStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = FontWeight.Bold,
    fontSize = 21.sp,
    lineHeight = 27.sp,
    letterSpacing = (-0.3).sp
)

private val QuoteStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 22.sp,
    lineHeight = 30.sp,
    letterSpacing = (-0.2).sp
)

private val BodyStyle = TextStyle(fontSize = 14.sp, lineHeight = 21.sp)
private val SmallStyle = TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
private val MetaStyle = TextStyle(
    fontSize = 12.sp,
    lineHeight = 16.sp,
    fontFeatureSettings = "tnum"
)
private val LabelStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Medium
)
private val SectionStyle = TextStyle(
    fontSize = 15.sp,
    lineHeight = 20.sp,
    fontWeight = FontWeight.SemiBold
)

@Composable
fun HomeScreen(
    seenCount: Int,
    savedCount: Int,
    cardLimit: Int,
    minuteLimit: Int,
    deckSize: Int? = null,
    nextCard: Card? = null,
    dailyQuote: Card? = null,
    onOpenReels: () -> Unit,
    onOpenSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            in 17..21 -> "Good evening,"
            else -> "Late night,"
        }
    }
    val dateLabel = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
    }

    // The deck is the real session: a 21-card allowance with one card left in
    // the pool is a one-card session, not twenty-one. Fall back to the setting
    // while the deck is still loading.
    val sessionTotal = when {
        deckSize == null -> cardLimit
        deckSize <= 0 -> 0
        else -> deckSize.coerceAtMost(cardLimit)
    }
    val hasCards = sessionTotal > 0
    val complete = hasCards && seenCount >= sessionTotal

    val ctaLabel = when {
        !hasCards -> "Open the feed"
        complete && savedCount > 0 -> "Review what you kept"
        complete -> "Open the feed"
        seenCount > 0 -> "Continue session"
        else -> "Start today's session"
    }
    val ctaIcon = if (complete && savedCount > 0) {
        Icons.Outlined.FavoriteBorder
    } else {
        Icons.AutoMirrored.Filled.ArrowForward
    }
    val ctaAction = if (complete && savedCount > 0) onOpenSaved else onOpenReels

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SparkBg)
            .verticalScroll(rememberScrollState())
    ) {
        BrandRow(dateLabel = dateLabel)

        Image(
            painter = painterResource(R.drawable.home_reading),
            contentDescription = "A man reading a book by a window",
            contentScale = ContentScale.Crop,
            alignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(222.dp)
        )
        Text(
            text = "Photo: Shixart1985 · CC BY 2.0 · cropped",
            style = SmallStyle.copy(fontSize = 11.sp),
            color = SparkFaint,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Gutter, vertical = 7.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Gutter)
                .padding(top = 22.dp)
        ) {
            Text(
                text = "$greeting\nSatyam.",
                style = DisplayStyle,
                color = SparkText,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "A finite feed of ideas worth keeping. Read your cards, then get on with the day.",
                style = BodyStyle,
                color = SparkMuted
            )
        }

        SessionBlock(
            seenCount = seenCount,
            total = sessionTotal,
            complete = complete
        )

        Button(
            onClick = ctaAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Gutter)
                .padding(top = 24.dp)
                .height(56.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = SparkText,
                contentColor = SparkBg
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            ),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) {
            Text(
                text = ctaLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = ctaIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Gutter)
                .padding(top = 6.dp)
        ) {
            if (!complete && savedCount > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Review saved cards",
                            onClick = onOpenSaved
                        )
                        .padding(vertical = 10.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = SparkMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "Review saved",
                        style = LabelStyle,
                        color = SparkMuted
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "· $savedCount",
                        style = MetaStyle,
                        color = SparkText
                    )
                }
            }
            if (hasCards && !complete) {
                Text(
                    text = "Up to $minuteLimit min",
                    style = SmallStyle,
                    color = SparkFaint,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
        }

        nextCard?.let { card ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Gutter)
                    .padding(top = 36.dp)
            ) {
                Text(
                    text = "Up next",
                    style = SectionStyle,
                    color = SparkText,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(12.dp))
                NextCard(card = card, onClick = onOpenReels)
            }
        }

        dailyQuote?.let { quote ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Gutter)
                    .padding(top = 36.dp)
            ) {
                Text(
                    text = "Today's line",
                    style = SectionStyle,
                    color = SparkText,
                    modifier = Modifier.semantics { heading() }
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = quote.body.asPullQuote(),
                    style = QuoteStyle,
                    color = SparkText,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
                quote.bookLine?.let { attribution ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = attribution,
                        style = SmallStyle.copy(fontSize = 11.sp),
                        color = SparkMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "SPARK · one reader, one feed",
            style = SmallStyle.copy(fontSize = 11.sp),
            color = SparkFaint,
            modifier = Modifier.padding(horizontal = Gutter)
        )
        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun BrandRow(dateLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Gutter, end = Gutter, top = 18.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SparkMark()
            Spacer(modifier = Modifier.width(9.dp))
            Text(
                text = "SPARK",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 2.4.sp,
                color = SparkText
            )
        }
        Text(
            text = dateLabel,
            style = MetaStyle,
            color = SparkFaint
        )
    }
}

/**
 * A four-point spark, drawn rather than borrowed from an icon set. It is the
 * product's mark, not decoration: it appears once, beside the wordmark.
 */
@Composable
private fun SparkMark() {
    Canvas(modifier = Modifier.size(14.dp)) {
        val c = center
        val r = size.minDimension / 2f
        val inner = r * 0.30f
        val path = Path().apply {
            moveTo(c.x, c.y - r)
            lineTo(c.x + inner, c.y - inner)
            lineTo(c.x + r, c.y)
            lineTo(c.x + inner, c.y + inner)
            lineTo(c.x, c.y + r)
            lineTo(c.x - inner, c.y + inner)
            lineTo(c.x - r, c.y)
            lineTo(c.x - inner, c.y - inner)
            close()
        }
        drawPath(path = path, color = SparkGold)
    }
}

@Composable
private fun SessionBlock(
    seenCount: Int,
    total: Int,
    complete: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Gutter)
            .padding(top = 28.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "This session",
                style = SectionStyle,
                color = SparkText,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = if (total > 0) {
                    "$seenCount / $total ${if (total == 1) "card" else "cards"}"
                } else {
                    "No cards"
                },
                style = MetaStyle,
                color = SparkMuted
            )
        }
        if (total > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            SessionStrip(seenCount = seenCount, total = total)
            if (complete) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Session complete.",
                    style = SmallStyle,
                    color = SparkGold
                )
            }
        } else {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Nothing to read right now.",
                style = SmallStyle,
                color = SparkMuted
            )
        }
    }
}

/**
 * Twenty equal steps, always — a short deck still reads as the same strip
 * instead of one giant block. The exact count stays in the label above.
 * Filled segments are the only amber data mark on the screen.
 */
@Composable
private fun SessionStrip(seenCount: Int, total: Int) {
    val segments = 20
    val filled = if (total <= 0) {
        0
    } else {
        ((seenCount.toFloat() / total) * segments)
            .roundToInt()
            .coerceIn(0, segments)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "$seenCount of $total ${if (total == 1) "card" else "cards"} read in this session"
            },
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        repeat(segments) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(22.dp)
                    .background(if (index < filled) SparkGold else SparkRaise2)
            )
        }
    }
}

@Composable
private fun NextCard(card: Card, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SparkRaise)
            .clickable(
                role = Role.Button,
                onClickLabel = "Open card",
                onClick = onClick
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(SparkGold, CircleShape)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = card.type.readableName(),
                    style = SmallStyle,
                    color = SparkGold
                )
            }
            if (card.topic.isNotBlank()) {
                Text(
                    text = card.topic.asSentenceCase(),
                    style = SmallStyle,
                    color = SparkFaint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = card.title,
            style = CardTitleStyle,
            color = SparkText,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        card.bookLine?.let { bookLine ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = bookLine,
                style = SmallStyle,
                color = SparkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (card.body.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = card.body,
                style = BodyStyle,
                color = SparkMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Open card",
                style = LabelStyle,
                color = SparkText
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = SparkText,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

private fun CardType.readableName(): String = when (this) {
    CardType.BOOK_IDEA -> "Book idea"
    CardType.SUMMARY -> "Summary"
    CardType.QUOTE -> "Quote"
    CardType.CONCEPT -> "Concept"
    CardType.QUIZ -> "Quiz"
    CardType.AUDIO -> "Audio"
    CardType.CHALLENGE -> "Challenge"
}

private fun String.asSentenceCase(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

/**
 * The API body may or may not carry edge quotes (the Reels quote card strips
 * and re-adds its own). Always wrap the Home pull quote in one pair of curly
 * quotes, so an unquoted body still reads as a quotation.
 */
private fun String.asPullQuote(): String =
    "“" + trim('"', '“', '”') + "”"

@Preview(
    showBackground = true,
    widthDp = 390,
    heightDp = 844,
    backgroundColor = 0xFF0E0D0C
)
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
                type = CardType.CONCEPT,
                topic = "Focus",
                title = "Deep work is a skill you practise, not a mood you wait for",
                body = "Attention is trainable. Protect one block of time, remove the easy escape, and let the discomfort of boredom pass.",
                actionLine = "Block 25 minutes today.",
                source = "Preview",
                grounded = true
            ),
            dailyQuote = Card(
                id = "preview-quote",
                type = CardType.QUOTE,
                topic = "Mindset",
                title = "Today's line",
                body = "The happiness of your life depends upon the quality of your thoughts.",
                actionLine = "",
                source = "Meditations",
                grounded = true,
                bookLine = "Meditations — Marcus Aurelius"
            ),
            onOpenReels = {},
            onOpenSaved = {}
        )
    }
}
