package com.spark.app.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.spark.app.data.Card
import com.spark.app.ui.OfflineConnectivityButton
import com.spark.app.data.CardType
import com.spark.app.R
import com.spark.app.ui.theme.SparkSpacing
import com.spark.app.ui.theme.SparkType
import com.spark.app.ui.theme.SparkTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Home uses the shared charcoal surfaces, warm paper text, forest reading card,
 * and amber action color. Display text uses Android's platform serif.
 *
 * Interactions use short, quiet press feedback. The tab transition owns screen
 * movement; the Home content itself stays still so it is ready when selected.
 */

private val Page = Color(0xFF121110)
private val Paper = Color(0xFFECE6E0)
private val PaperDeep = Color(0xFF242220)
private val CardBg = Color(0xFF1A1817)
private val Ink = Color(0xFFECE6E0)
private val InkSoft = Color(0xFFCFC7BE)
private val Line = Color(0xFF49433D)
private val Ember = Color(0xFFFFB454)
private val Forest = Color(0xFF214A36)
private val ForestDeep = Color(0xFF0E0D0C)
private val SageSoft = Color(0xFF1E4F42)
private val SageAccent = Color(0xFFA6D8C4)

private val Display = FontFamily.Serif
private val Sans = FontFamily.SansSerif

private val Gutter = SparkSpacing.pageGutter

private val KickerStyle = SparkType.kicker
private val HeadlineStyle = SparkType.homeHeadline
private val CardTitleStyle = SparkType.cardTitle
private val QuoteStyle = SparkType.quote
private val QuoteSmallStyle = SparkType.quoteSmall
private val BodyStyle = SparkType.body
private val TinyStyle = SparkType.compactMeta

/**
 * A small, quiet press response for Home actions. The spring settles without
 * overshoot so a fast sequence of taps stays calm and predictable.
 */
@Composable
private fun Modifier.pressFeedback(
    scaleDown: Float = 0.98f,
    enabled: Boolean = true,
    role: Role = Role.Button,
    contentDescription: String? = null,
    onClick: () -> Unit
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "homePressScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            }
        )
        .then(
            if (contentDescription != null) {
                Modifier.semantics(mergeDescendants = true) {
                    this.contentDescription = contentDescription
                    this.role = role
                }
            } else Modifier
        )
}

@Composable
fun HomeScreen(
    seenCount: Int,
    savedCount: Int,
    cardLimit: Int,
    minuteLimit: Int,
    modifier: Modifier = Modifier,
    offline: Boolean = false,
    onRetryConnection: () -> Unit = {},
    deckSize: Int? = null,
    deckErrorMessage: String? = null,
    onRetryDeck: (() -> Unit)? = null,
    nextCard: Card? = null,
    dailyQuote: Card? = null,
    bottomClearance: Dp = 100.dp,
    onOpenReels: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenMe: () -> Unit = {}
) {
    val locale = LocalLocale.current.platformLocale
    val today = remember(locale) { LocalDate.now() }
    val dateLabel = remember(today, locale) {
        today.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", locale))
    }
    val sessionTotal = when {
        deckSize == null || deckSize <= 0 -> 0
        else -> deckSize.coerceAtMost(cardLimit)
    }
    val fraction = if (sessionTotal <= 0) 0f else (seenCount.toFloat() / sessionTotal).coerceIn(0f, 1f)

    // The session bar fills with a soft spring, so starting the day has a
    // little give instead of jumping straight to its width.
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "sessionProgress"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Page)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.padding(horizontal = Gutter)) {
            // Lifted a few pixels higher for a tighter, cleaner top header anchor
            Spacer(Modifier.height(6.dp))

            HomeHeader(
                savedCount = savedCount,
                offline = offline,
                onRetryConnection = onRetryConnection,
                onOpenMe = onOpenMe,
                onOpenSaved = onOpenSaved
            )

            Spacer(Modifier.height(20.dp))

            Lede(dateLabel = dateLabel)

            Spacer(Modifier.height(20.dp))

            if (deckSize == null) {
                HomeStateBlock(
                    title = "Loading your deck",
                    message = "Your reading deck will appear here as soon as it’s ready.",
                    loading = true
                )
            } else if (deckErrorMessage != null) {
                HomeStateBlock(
                    title = "Your deck couldn’t load",
                    message = deckErrorMessage,
                    actionLabel = if (onRetryDeck != null) "Try again" else null,
                    onAction = onRetryDeck
                )
            } else if (deckSize == 0) {
                HomeStateBlock(
                    title = "No cards in the deck",
                    message = "Refresh your deck from Me when you’re ready.",
                    actionLabel = "Open Me",
                    onAction = onOpenMe
                )
            } else if (nextCard != null) {
                SectionHead(
                    title = "Today’s pick",
                    action = "Your shelf",
                    onAction = onOpenSaved
                )
                Spacer(Modifier.height(SparkSpacing.rowGap))
                PickCard(
                    card = nextCard,
                    fraction = animatedFraction,
                    seenCount = seenCount,
                    total = sessionTotal,
                    minuteLimit = minuteLimit,
                    onClick = onOpenReels
                )
            } else {
                HomeStateBlock(
                    title = "You’re done for now",
                    message = "Your session limit is reached. Save an idea to revisit, or come back later.",
                    actionLabel = "Review saved ideas",
                    onAction = onOpenSaved
                )
            }

            if (dailyQuote != null && deckSize != null && deckErrorMessage == null) {
                Spacer(Modifier.height(SparkSpacing.sectionGap))
                SectionHead(title = "Saved review", action = "01 / 01", onAction = null)
                Spacer(Modifier.height(SparkSpacing.rowGap))
                QuoteCard(card = dailyQuote)
            }

            // Bottom clearance so content scrolls comfortably above the floating glass nav bar
            Spacer(Modifier.height(bottomClearance))
        }
    }
}

@Composable
private fun HomeHeader(
    savedCount: Int,
    offline: Boolean,
    onRetryConnection: () -> Unit,
    onOpenMe: () -> Unit = {},
    onOpenSaved: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Image(
            painter = painterResource(R.drawable.ic_spark_wordmark),
            contentDescription = "SPARK",
            colorFilter = ColorFilter.tint(Ink),
            modifier = Modifier.height(21.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (offline) {
                OfflineConnectivityButton(
                    onRetry = onRetryConnection,
                    modifier = Modifier.size(48.dp),
                    description = "Offline. Tap to retry Home and check the connection."
                )
                Spacer(Modifier.width(4.dp))
            }

            // The shortcut opens the saved library, where search lives.
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.dp, Line, CircleShape)
                    .background(CardBg.copy(alpha = 0.7f))
                    .pressFeedback(
                        scaleDown = 0.96f,
                        contentDescription = "Open saved library",
                        onClick = onOpenSaved
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = InkSoft,
                    modifier = Modifier.size(19.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Avatar button with saved-count badge & bouncy press
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Forest)
                    .pressFeedback(
                        scaleDown = 0.96f,
                        contentDescription = if (savedCount > 0) {
                            "Open profile, $savedCount saved items"
                        } else {
                            "Open profile"
                        },
                        onClick = onOpenMe
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "S",
                    style = TextStyle(
                        fontFamily = Display,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    ),
                    color = Paper
                )
            }
        }
    }
}

@Composable
private fun Lede(dateLabel: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = dateLabel,
            style = KickerStyle,
            color = InkSoft
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = Ink)) { append("Read one thing.\n") }
                withStyle(SpanStyle(color = Ember, fontStyle = FontStyle.Italic)) {
                    append("Try it today.")
                }
            },
            style = HeadlineStyle,
            modifier = Modifier.semantics { heading() }
        )
    }
}

@Composable
private fun SectionHead(title: String, action: String?, onAction: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
        text = title.uppercase(Locale.ROOT),
            style = KickerStyle,
            color = InkSoft,
            modifier = Modifier.semantics { heading() }
        )
        if (action != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .then(
                        if (onAction != null) {
                            Modifier.pressFeedback(
                                scaleDown = 0.92f,
                                contentDescription = action,
                                onClick = onAction
                            )
                        } else Modifier
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = action,
                    style = TinyStyle.copy(fontWeight = FontWeight.Medium),
                    color = if (onAction != null) Ember else InkSoft
                )
            }
        }
    }
}

/** The day's card: a drawn cover, the type chip, peek preview, and forward navigation. */
@Composable
private fun PickCard(
    card: Card,
    fraction: Float,
    seenCount: Int,
    total: Int,
    minuteLimit: Int,
    onClick: () -> Unit
) {
    var isPeekExpanded by rememberSaveable(card.id) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val largeTextLayout = LocalDensity.current.fontScale >= 1.5f
    val cardInteraction = remember { MutableInteractionSource() }
    val isPressed by cardInteraction.collectIsPressedAsState()

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pickCardScale"
    )

    val arrowOffset by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "arrowOffset"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardBg,
        border = BorderStroke(
            1.dp,
            if (isPressed) Line.copy(alpha = 1f) else Line.copy(alpha = 0.9f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clickable(
                interactionSource = cardInteraction,
                indication = null,
                role = Role.Button,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onClick()
                }
            )
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("Today's pick: ${card.title}.")
                    if (!card.grounded) append(" AI-written, unverified.")
                    append(" Tap to read.")
                }
            }
    ) {
        Column(modifier = Modifier.padding(SparkSpacing.cardInset)) {
            Row {
                if (!largeTextLayout) {
                    PickArtwork(
                        card = card,
                        isPressed = isPressed,
                        modifier = Modifier.width(84.dp)
                    )
                    Spacer(Modifier.width(SparkSpacing.rowGap))
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (largeTextLayout) {
                        Column(verticalArrangement = Arrangement.spacedBy(SparkSpacing.labelGap)) {
                            Text(
                                text = card.type.label().uppercase(Locale.ROOT),
                                style = TinyStyle.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.1.sp,
                                    fontSize = 11.sp
                                ),
                                color = SageAccent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(SageSoft)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                            if (card.topic.isNotBlank()) {
                                Text(
                                    text = card.topic,
                                    style = TinyStyle,
                                    color = InkSoft,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = card.type.label().uppercase(Locale.ROOT),
                                style = TinyStyle.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 1.1.sp,
                                    fontSize = 11.sp
                                ),
                                color = SageAccent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(SageSoft)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                            if (card.topic.isNotBlank()) {
                                Text(text = card.topic, style = TinyStyle, color = InkSoft)
                            }
                        }
                    }

                    Spacer(Modifier.height(SparkSpacing.contentGap))

                    Text(
                        text = card.title,
                        style = CardTitleStyle,
                        color = Ink,
                        maxLines = if (largeTextLayout) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(SparkSpacing.labelGap))

                    Text(
                        text = if (!card.grounded) {
                            "AI-written, unverified"
                        } else {
                            card.bookLine?.let { bookTitle(it) } ?: cleanSource(card.source)
                        },
                        style = BodyStyle,
                        color = InkSoft,
                        maxLines = if (largeTextLayout) 3 else 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.height(SparkSpacing.rowGap))

                    if (largeTextLayout) {
                        Column(verticalArrangement = Arrangement.spacedBy(SparkSpacing.labelGap)) {
                            Text("$seenCount of $total read", style = TinyStyle, color = InkSoft)
                            Text("$minuteLimit min limit", style = TinyStyle, color = InkSoft)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("$seenCount of $total read", style = TinyStyle, color = InkSoft)
                            Text("$minuteLimit min limit", style = TinyStyle, color = InkSoft)
                        }
                    }

                    Spacer(Modifier.height(SparkSpacing.contentGap))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(PaperDeep)
                            .semantics {
                                contentDescription = "Reading session progress"
                                stateDescription = "$seenCount of $total cards read"
                                progressBarRangeInfo = ProgressBarRangeInfo(
                                    current = fraction.coerceIn(0f, 1f),
                                    range = 0f..1f
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                .fillMaxSize()
                                .clip(RoundedCornerShape(100.dp))
                                .background(Ember)
                        )
                    }

                    Spacer(Modifier.height(SparkSpacing.rowGap))

                    // Action row: Primary forward navigation + Peek inside toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .pressFeedback(onClick = onClick)
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Text(
                                text = if (seenCount > 0) "Continue session" else "Read it",
                                style = BodyStyle.copy(fontWeight = FontWeight.SemiBold),
                                color = Ember
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Ember,
                                modifier = Modifier
                                    .size(12.dp)
                                    .graphicsLayer { translationX = arrowOffset.toPx() }
                            )
                        }

                        // Peek inside / Hide peek toggle
                        Text(
                            text = if (isPeekExpanded) "Hide peek" else "Peek inside",
                            style = TinyStyle.copy(fontWeight = FontWeight.Medium),
                            color = InkSoft,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .pressFeedback(
                                    scaleDown = 0.92f,
                                    contentDescription = if (isPeekExpanded) "Hide peek" else "Peek inside",
                                    onClick = { isPeekExpanded = !isPeekExpanded }
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Expandable peek inside: smooth spring entrance & collapse
            AnimatedVisibility(
                visible = isPeekExpanded,
                enter = fadeIn(tween(180)) +
                    slideInVertically(
                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                        initialOffsetY = { height -> height / 8 }
                    ) + expandVertically(tween(240, easing = FastOutSlowInEasing)),
                exit = fadeOut(tween(120)) +
                    shrinkVertically(tween(180, easing = FastOutSlowInEasing)) +
                    slideOutVertically(
                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                        targetOffsetY = { height -> height / 8 }
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = SparkSpacing.contentGap)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PaperDeep.copy(alpha = 0.55f))
                        .border(1.dp, Line.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(SparkSpacing.cardInset)
                ) {
                    Text(
                        text = card.body,
                        style = QuoteSmallStyle.copy(fontSize = 12.sp, lineHeight = 17.sp),
                        color = Ink,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (card.actionLine.isNotBlank()) {
                        Spacer(Modifier.height(SparkSpacing.contentGap))
                        Text(
                            text = card.actionLine,
                            style = TinyStyle.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Ember
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/** Use cover styling only for book-based cards; other cards get a simple type marker. */
@Composable
private fun PickArtwork(
    card: Card,
    modifier: Modifier = Modifier,
    isPressed: Boolean = false
) {
    val isBookCard = card.bookLine != null &&
        (card.type == CardType.BOOK_IDEA || card.type == CardType.SUMMARY)

    if (!isBookCard) {
        IdeaArtwork(type = card.type, modifier = modifier)
        return
    }

    val sunOffset by animateFloatAsState(
        targetValue = if (isPressed) 1.05f else 1.08f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "bookCoverSun"
    )

    Box(
        modifier = modifier
            .aspectRatio(2f / 2.9f)
            .clip(RoundedCornerShape(5.dp))
            .background(Forest)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                color = Ember,
                radius = w * 0.46f,
                center = Offset(w * sunOffset, h * 0.36f)
            )
            drawCircle(
                color = Paper.copy(alpha = 0.5f),
                radius = w * 0.37f,
                center = Offset(w * (sunOffset + 0.09f), h * 0.43f),
                style = Stroke(width = 1f)
            )
            drawCircle(
                color = Paper.copy(alpha = 0.3f),
                radius = w * 0.29f,
                center = Offset(w * (sunOffset + 0.13f), h * 0.48f),
                style = Stroke(width = 1f)
            )
            drawRect(
                color = ForestDeep,
                topLeft = Offset(0f, h * 0.66f),
                size = androidx.compose.ui.geometry.Size(w, h * 0.34f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            SparkStar(modifier = Modifier.size(10.dp), color = Paper.copy(alpha = 0.8f))
            Column {
                Text(
                    text = (card.bookLine?.let { parseAuthor(it) } ?: cleanSource(card.source))
                        .uppercase(Locale.ROOT),
                    style = TextStyle(
                        fontFamily = Sans,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        letterSpacing = 0.4.sp
                    ),
                    color = Paper.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = card.bookLine?.let(::bookTitle) ?: card.title,
                    style = TextStyle(
                        fontFamily = Display,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        lineHeight = 14.sp,
                        letterSpacing = (-0.25).sp
                    ),
                    color = Color(0xFFF7F2E8),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun IdeaArtwork(type: CardType, modifier: Modifier = Modifier) {
    val backgroundColor = when (type) {
        CardType.QUOTE -> Forest
        CardType.CHALLENGE -> ForestDeep
        else -> SageSoft
    }
    val markColor = when (type) {
        CardType.QUOTE -> Ember
        CardType.CHALLENGE -> Paper
        else -> SageAccent
    }
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .aspectRatio(2f / 2.9f)
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, markColor.copy(alpha = 0.18f), shape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                color = markColor.copy(alpha = 0.09f),
                radius = w * 0.52f,
                center = Offset(w * 0.94f, h * 0.38f)
            )
            drawCircle(
                color = markColor.copy(alpha = 0.2f),
                radius = w * 0.42f,
                center = Offset(w * 0.94f, h * 0.38f),
                style = Stroke(width = 1f)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(9.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "SPARK",
                style = TinyStyle.copy(fontSize = 8.sp, letterSpacing = 1.sp),
                color = Paper.copy(alpha = 0.76f)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(34.dp)
                    .clip(CircleShape)
                    .border(1.dp, markColor.copy(alpha = 0.48f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                SparkStar(modifier = Modifier.size(15.dp), color = markColor)
            }
            Text(
                text = type.label().uppercase(Locale.ROOT),
                style = TinyStyle.copy(fontSize = 8.sp, letterSpacing = 0.65.sp),
                color = Paper.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Forest card: the day's line, in the book's voice with engraved halos & interactive bookmark. */
@Composable
private fun QuoteCard(card: Card) {
    val author = card.bookLine?.let { parseAuthor(it) } ?: card.source

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Forest,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(SparkSpacing.cardInset)) {
            Text(
                text = "${card.type.label()} · ${card.topic}",
                style = TinyStyle.copy(letterSpacing = 0.4.sp),
                color = Paper.copy(alpha = 0.72f)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Ember)) { append("“") }
                    append(card.body.asQuoteBody())
                    withStyle(SpanStyle(color = Ember)) { append("”") }
                },
                style = QuoteStyle,
                color = Color(0xFFF7F1E5)
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Paper.copy(alpha = 0.16f))
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = author,
                style = BodyStyle.copy(fontWeight = FontWeight.SemiBold),
                color = Paper,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = card.bookLine?.let { bookTitle(it) } ?: cleanSource(card.source),
                style = TinyStyle,
                color = Paper.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeStateBlock(
    title: String,
    message: String,
    loading: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CardBg,
        border = BorderStroke(1.dp, Line)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Ember,
                    trackColor = PaperDeep,
                    strokeWidth = 2.dp
                )
            } else {
                SparkStar(modifier = Modifier.size(22.dp), color = Ember)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = CardTitleStyle.copy(fontSize = 19.sp),
                color = Ink
            )
            Spacer(Modifier.height(4.dp))
            Text(text = message, style = BodyStyle, color = InkSoft)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onAction,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(actionLabel, color = Ember, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SparkStar(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val p = Path().apply {
            moveTo(w * 0.5f, h * 0.02f)
            cubicTo(w * 0.53f, h * 0.27f, w * 0.73f, h * 0.47f, w * 0.98f, h * 0.5f)
            cubicTo(w * 0.73f, h * 0.53f, w * 0.53f, h * 0.73f, w * 0.5f, h * 0.98f)
            cubicTo(w * 0.47f, h * 0.73f, w * 0.27f, h * 0.53f, w * 0.02f, h * 0.5f)
            cubicTo(w * 0.27f, h * 0.47f, w * 0.47f, h * 0.27f, w * 0.5f, h * 0.02f)
            close()
        }
        drawPath(path = p, color = color)
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

/** "Title — Author" → "Title"; anything else is returned unchanged. */
private fun bookTitle(bookLine: String): String {
    val parts = bookLine.split("—", "·").map { it.trim() }.filter { it.isNotEmpty() }
    return parts.firstOrNull() ?: bookLine
}

/** A reader-facing source name: no URLs, no "Wikipedia: …" prefixes. */
private fun cleanSource(source: String): String = when {
    source.startsWith("Wikipedia", ignoreCase = true) -> "Wikipedia"
    source.startsWith("ebook", ignoreCase = true) -> "Project Gutenberg"
    source.startsWith("Bartlett", ignoreCase = true) -> "Bartlett’s Quotations"
    source.length > 30 -> source.take(29).trimEnd() + "…"
    else -> source
}

/** "Title — Author" → "Author"; anything else is returned unchanged. */
private fun parseAuthor(bookLine: String): String {
    val parts = bookLine.split("—", "·").map { it.trim() }.filter { it.isNotEmpty() }
    return if (parts.size >= 2) parts.last() else bookLine
}

private fun String.asQuoteBody(): String =
    trim().removePrefix("Opening lines of this public-domain classic:").trim()

@Preview(showBackground = true, widthDp = 390, heightDp = 844, backgroundColor = 0xFF121110)
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
                id = "p1",
                type = CardType.BOOK_IDEA,
                topic = "mindset",
                title = "Why read As a Man Thinketh",
                body = "The aphorism, “As a man thinketh in his heart so is he,” not only embraces " +
                    "the whole of a man’s being.",
                actionLine = "Try this today: pick the one thought you will refuse to repeat this week.",
                source = "ebook #4507",
                grounded = true,
                bookLine = "As a Man Thinketh — James Allen"
            ),
            dailyQuote = Card(
                id = "p2",
                type = CardType.QUOTE,
                topic = "mindset",
                title = "Marcus Aurelius",
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
