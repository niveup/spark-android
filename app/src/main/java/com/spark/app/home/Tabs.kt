package com.spark.app.home
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextAlign

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.spark.app.data.Card
import com.spark.app.data.CardType
import com.spark.app.data.SavedExportFormat
import com.spark.app.data.SettingsStore
import com.spark.app.feed.DeckRefreshState
import com.spark.app.feed.FeedUiState
import com.spark.app.ui.OfflineConnectivityButton
import com.spark.app.ui.theme.SparkSpacing
import com.spark.app.ui.theme.SparkType
import java.util.Locale

// ---------------------------------------------------------------------------
// Shared charcoal surfaces, warm text, forest accents, and amber actions.
// ---------------------------------------------------------------------------
private val Page = Color(0xFF121110)
private val Paper = Color(0xFFECE6E0)
private val PaperDeep = Color(0xFF242220)
private val CardBg = Color(0xFF1A1817)
private val ReadingSurface = Color(0xFF211B15)
private val Ink = Color(0xFFECE6E0)
private val InkSoft = Color(0xFFCFC7BE)
private val Line = Color(0xFF49433D)
private val Ember = Color(0xFFFFB454)
private val Forest = Color(0xFF214A36)
private val SageSoft = Color(0xFF1E4F42)
private val SageAccent = Color(0xFFA6D8C4)

private val Display = FontFamily.Serif
private val Sans = FontFamily.SansSerif

private val Kicker = SparkType.kicker
private val PageTitle = SparkType.pageTitle
private val DisplaySmall = SparkType.displaySmall
private val Body = SparkType.body
private val Meta = SparkType.meta

// ---------------------------------------------------------------------------
// Saved
// ---------------------------------------------------------------------------

@Composable
fun SavedScreen(
    state: SavedUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onExport: (SavedExportFormat) -> Unit,
    onOpenReels: () -> Unit = {},
    offline: Boolean = false,
    onRetryConnection: () -> Unit = {},
    /** Opens a saved card in the Reels tab. */
    onOpenCard: (Card) -> Unit = {},
    /** Id of the card that is currently being opened, if any. */
    openingItemId: String? = null,
    modifier: Modifier = Modifier,
    bottomClearance: Dp = 100.dp
) {
    val haptics = LocalHapticFeedback.current
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val cards = (state as? SavedUiState.Content)?.cards.orEmpty()
    val shown = when (filter) {
        1 -> cards.filter { it.type == CardType.QUOTE }
        2 -> cards.filter { it.type != CardType.QUOTE }
        else -> cards
    }
    val savedCount = (state as? SavedUiState.Content)?.let { shown.size }
    val countLabel = if (query.isBlank() && filter == 0) "saved" else "results"
    val canExport = state is SavedUiState.Content && cards.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Page)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = SparkSpacing.pageGutter)) {
            // Lifted a few pixels higher to align cleanly across all tabs
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Kept close".uppercase(Locale.ROOT), style = Kicker, color = InkSoft)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Your library",
                        style = PageTitle,
                        color = Ink,
                        modifier = Modifier.semantics { heading() }
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (offline) {
                        OfflineConnectivityButton(
                            onRetry = onRetryConnection,
                            modifier = Modifier.size(48.dp),
                            description = "Offline. Tap to retry saved cards and check the connection."
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = CardBg,
                        border = BorderStroke(1.dp, Line),
                        modifier = Modifier.semantics {
                            contentDescription = savedCount?.let { "$it $countLabel" } ?: "Loading saved cards"
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            AnimatedContent(
                                targetState = savedCount,
                                transitionSpec = {
                                    if ((targetState ?: -1) > (initialState ?: -1)) {
                                        (slideInVertically { it / 2 } + fadeIn()) togetherWith
                                            (slideOutVertically { -it / 2 } + fadeOut())
                                    } else {
                                        (slideInVertically { -it / 2 } + fadeIn()) togetherWith
                                            (slideOutVertically { it / 2 } + fadeOut())
                                    }
                                },
                                label = "savedCounterRoll"
                            ) { count ->
                                Text(
                                    text = count?.toString() ?: "—",
                                    style = Meta.copy(fontWeight = FontWeight.SemiBold, color = Ember)
                                )
                            }
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = countLabel,
                                style = Meta,
                                color = InkSoft
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) { contentDescription = "Search saved cards" },
                singleLine = true,
                shape = RoundedCornerShape(100.dp),
                textStyle = Body.copy(fontSize = 14.sp, color = Ink),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = InkSoft.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    AnimatedVisibility(
                        visible = query.isNotEmpty(),
                        enter = fadeIn(tween(150)) + scaleIn(spring(stiffness = Spring.StiffnessMediumLow)),
                        exit = fadeOut(tween(150)) + scaleOut(spring(stiffness = Spring.StiffnessMediumLow))
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable(role = Role.Button) {
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    onQueryChange("")
                                }
                                .semantics(mergeDescendants = true) { contentDescription = "Clear search query" },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(PaperDeep),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "×",
                                    style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                                    color = InkSoft
                                )
                            }
                        }
                    }
                },
                placeholder = { Text("Search saved cards…", style = Body.copy(fontSize = 14.sp), color = InkSoft) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardBg,
                    unfocusedContainerColor = CardBg,
                    focusedBorderColor = Ember,
                    unfocusedBorderColor = Line,
                    cursorColor = Ember,
                    focusedTextColor = Ink,
                    unfocusedTextColor = Ink
                )
            )

            Spacer(Modifier.height(8.dp))
            if (canExport) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportLink("Export .json", enabled = true) { onExport(SavedExportFormat.JSON) }
                    ExportLink("Export .md", enabled = true) { onExport(SavedExportFormat.MARKDOWN) }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (cards.isNotEmpty()) BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(PaperDeep.copy(alpha = 0.6f))
                    .border(1.dp, Line.copy(alpha = 0.8f), RoundedCornerShape(100.dp))
            ) {
                val third = maxWidth / 3
                val density = LocalDensity.current
                val stepPx = with(density) { third.toPx() }

                val pillX by animateFloatAsState(
                    targetValue = stepPx * filter,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "segmentPillX"
                )

                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .width(third - 6.dp)
                        .height(42.dp)
                        .graphicsLayer { translationX = pillX }
                        .clip(RoundedCornerShape(100.dp))
                        .background(Forest)
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("All", "Quotes", "Ideas").forEachIndexed { index, label ->
                        val selected = filter == index
                        val textColor by animateColorAsState(
                            targetValue = if (selected) Paper else InkSoft,
                            animationSpec = tween(durationMillis = 180),
                            label = "segmentTextColor_$index"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(100.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    role = Role.Tab
                                ) {
                                    if (filter != index) {
                                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                        filter = index
                                    }
                                }
                                .semantics(mergeDescendants = true) {
                                    role = Role.Tab
                                    this.selected = selected
                                    stateDescription = if (selected) "Selected" else "Not selected"
                                    contentDescription = "$label filter tab, ${if (selected) "selected" else "not selected"}"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = TextStyle(
                                    fontFamily = Sans,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp
                                ),
                                color = textColor
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
        }

        when (state) {
            is SavedUiState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SparkSpacing.pageGutter)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Loading saved cards…", style = Body, color = InkSoft)
                    }
                    Spacer(Modifier.height(12.dp))
                    repeat(2) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = CardBg,
                            border = BorderStroke(1.dp, Line.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Line.copy(alpha = 0.7f))
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        Modifier
                                            .width(90.dp)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Line.copy(alpha = 0.5f))
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Box(
                                    Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Line.copy(alpha = 0.5f))
                                )
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    Modifier
                                        .fillMaxWidth(0.5f)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Line.copy(alpha = 0.35f))
                                )
                            }
                        }
                    }
                }
            }

            is SavedUiState.Error -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SparkSpacing.pageGutter)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, Ember.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .background(CardBg)
                    .padding(18.dp)
            ) {
                Text(state.message, style = Body, color = Ink)
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onRetry,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Forest,
                        contentColor = Paper
                    )
                ) {
                    Text("Try again")
                }
            }

            is SavedUiState.Content -> {
                if (shown.isEmpty()) {
                    EmptySaved(
                        hasQuery = query.isNotBlank(),
                        onClearQuery = if (query.isNotBlank()) ({ onQueryChange("") }) else null,
                        onBrowse = if (query.isBlank()) {
                            {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onOpenReels()
                            }
                        } else null
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = SparkSpacing.pageGutter,
                            end = SparkSpacing.pageGutter,
                            bottom = bottomClearance
                        ),
                        verticalArrangement = Arrangement.spacedBy(SparkSpacing.rowGap)
                    ) {
                        items(shown, key = { it.id }) { card ->
                            SavedCard(
                                card = card,
                                opening = openingItemId == card.id,
                                onOpen = { onOpenCard(card) },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    ),
                                    fadeOutSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    placementSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExportLink(label: String, enabled: Boolean, onClick: () -> Unit) {
    val accessibleDescription = if (label.contains("json", ignoreCase = true)) "Export JSON" else "Export Markdown"

    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) { contentDescription = accessibleDescription }
    ) {
        Text(text = label, style = Body.copy(fontSize = 13.sp))
    }
}

/**
 * One saved card. Tapping it opens the card in the Reels tab (the saved id
 * is mapped back to the card); "Preview" expands the card here instead.
 */
@Composable
private fun SavedCard(
    card: Card,
    opening: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable(card.id) { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val contextLabel = card.bookLine?.let { "${card.type.label()} · ${card.topic}" } ?: card.topic
    val isQuote = card.type == CardType.QUOTE
    val quoteText = if (isQuote) "“${card.body.trim('"', '“', '”')}”" else card.title

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardBg,
        border = BorderStroke(1.dp, Line.copy(alpha = 0.9f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button
            ) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onOpen()
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = "Open in Reels: $quoteText"
                stateDescription = if (opening) "Opening" else "Opens the card in Reels"
            }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        Column(modifier = Modifier.padding(SparkSpacing.cardInset)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.bookLine?.let { authorOf(it) } ?: card.type.label(),
                        style = Body.copy(fontWeight = FontWeight.SemiBold, color = Ink),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(card.type.savedAccent())
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = contextLabel,
                            style = Meta,
                            color = InkSoft,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (card.grounded) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(SageSoft.copy(alpha = 0.6f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Grounded",
                            style = Meta.copy(fontSize = 11.sp),
                            color = SageAccent
                        )
                    }
                }
            }

            Spacer(Modifier.height(SparkSpacing.contentGap))


            Text(
                text = quoteText,
                style = DisplaySmall,
                color = Ink,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            // When expanded, reveal full body for non-quote cards as well
            if (expanded && !isQuote && card.body.isNotBlank()) {
                Spacer(Modifier.height(SparkSpacing.contentGap))
                Text(
                    text = card.body,
                    style = Body.copy(color = Ink),
                    lineHeight = 21.sp
                )
            }

            // Action line when expanded
            if (expanded && !card.actionLine.isNullOrBlank()) {
                Spacer(Modifier.height(SparkSpacing.rowGap))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SageSoft.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, Line.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Ember)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = card.actionLine,
                            style = Body.copy(fontStyle = FontStyle.Italic),
                            color = Ink
                        )
                    }
                }
            }

            Spacer(Modifier.height(SparkSpacing.rowGap))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = card.bookLine?.let { titleOf(it) } ?: card.sourceLabel(),
                    style = Meta,
                    color = InkSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (opening) {
                    Text(
                        text = "Opening…",
                        style = Meta,
                        color = Ember
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Button
                            ) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                expanded = !expanded
                            }
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 10.dp)
                            .semantics(mergeDescendants = true) {
                                role = Role.Button
                                contentDescription = if (expanded) {
                                    "Hide the preview of this card"
                                } else {
                                    "Preview this card here"
                                }
                                stateDescription = if (expanded) "Preview shown" else "Preview hidden"
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (expanded) "Hide preview" else "Preview",
                            style = Meta,
                            color = InkSoft
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySaved(
    hasQuery: Boolean,
    onClearQuery: (() -> Unit)? = null,
    onBrowse: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .padding(horizontal = SparkSpacing.pageGutter)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Line, RoundedCornerShape(20.dp))
            .background(CardBg.copy(alpha = 0.7f))
            .padding(24.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = if (hasQuery) {
                    "Nothing matches. Try a different word."
                } else {
                    "Nothing saved yet. Tap ♡ Save on any card in Reels."
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (hasQuery) PaperDeep.copy(alpha = 0.8f) else SageSoft.copy(alpha = 0.7f))
                .border(1.dp, Line.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (hasQuery) "⌕" else "✦",
                style = TextStyle(
                    fontFamily = Display,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = if (hasQuery) InkSoft else Ember
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (hasQuery) "Nothing matches" else "Nothing saved yet",
            style = DisplaySmall.copy(fontSize = 17.sp, fontWeight = FontWeight.Medium),
            color = Ink
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = if (hasQuery) {
                "Try searching for another word or author."
            } else {
                "Tap the save action on a card in Reels and it will live here."
            },
            style = Body,
            color = InkSoft,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (hasQuery && onClearQuery != null) {
            Spacer(Modifier.height(14.dp))
            TextButton(
                onClick = onClearQuery,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(text = "Clear search", style = Body.copy(fontSize = 13.sp))
            }
        } else if (!hasQuery && onBrowse != null) {
            Spacer(Modifier.height(14.dp))
            TextButton(
                onClick = onBrowse,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .semantics(mergeDescendants = true) { contentDescription = "Browse ideas in Reels" }
            ) {
                Text(text = "Browse Reels", style = Body.copy(fontSize = 13.sp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Me
// ---------------------------------------------------------------------------

@Composable
fun MeScreen(
    stats: StatsUiState,
    deckRefreshState: DeckRefreshState = DeckRefreshState.Idle,
    offline: Boolean = false,
    onRetryConnection: () -> Unit = {},
    cardLimit: Int,
    minuteLimit: Int,
    pendingSync: Int,
    onCardLimitDelta: (Int) -> Unit,
    onMinuteLimitDelta: (Int) -> Unit,
    onRetryStats: () -> Unit,
    cachedCount: Int,
    onClearCache: () -> Unit,
    onLogout: () -> Unit,
    deckState: FeedUiState,
    onRefreshDeck: () -> Unit,
    modifier: Modifier = Modifier,
    bottomClearance: Dp = 96.dp
) {
    val haptics = LocalHapticFeedback.current
    var showClearDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Page)
            .statusBarsPadding(),
        state = rememberLazyListState(),
        contentPadding = PaddingValues(
            start = SparkSpacing.pageGutter,
            top = SparkSpacing.screenTop,
            end = SparkSpacing.pageGutter,
            bottom = bottomClearance
        )
    ) {
        item(key = "me-header") {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Your space",
                        style = PageTitle,
                        color = Ink,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { heading() }
                    )
                    if (offline) {
                        OfflineConnectivityButton(
                            onRetry = onRetryConnection,
                            modifier = Modifier.size(48.dp),
                            description = "Offline. Tap to check the connection."
                        )
                    }
                }
                Spacer(Modifier.height(SparkSpacing.labelGap))
                Text(
                    text = "Your reading, session limits, and offline library",
                    style = Body.copy(fontSize = 14.sp),
                    color = InkSoft
                )
                Spacer(Modifier.height(20.dp))
            }
        }

        item(key = "reading-history") {
            Column {
                ReadingHistoryPanel(
                    state = stats,
                    onRetry = onRetryStats
                )
                Spacer(Modifier.height(SparkSpacing.sectionGap))
            }
        }

        item(key = "deck") {
            Column {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .padding(SparkSpacing.cardInset)
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your deck",
                        style = Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        color = Ink,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { heading() }
                    )
                    TextButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onRefreshDeck()
                        },
                        enabled = deckRefreshState !is DeckRefreshState.Refreshing,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .semantics(mergeDescendants = true) { contentDescription = "Refresh your deck" }
                    ) {
                        Text(
                            if (deckRefreshState is DeckRefreshState.Refreshing) {
                                "Refreshing…"
                            } else {
                                "Refresh deck"
                            }
                        )
                    }
                }
                when (val current = deckState) {
                    FeedUiState.Loading -> Row(
                        modifier = Modifier.heightIn(min = 48.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Checking your deck…", style = Body, color = InkSoft)
                    }

                    FeedUiState.Empty -> DeckStatusMessage(
                        message = "No cards are ready yet. Try refreshing the deck.",
                        onRefresh = onRefreshDeck
                    )

                    is FeedUiState.Error -> DeckStatusMessage(
                        message = current.message,
                        onRefresh = onRefreshDeck
                    )

                    is FeedUiState.Content -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${current.cards.size} cards ready",
                                style = DisplaySmall.copy(fontSize = 20.sp),
                                color = Ink
                            )
                            if (current.offline) {
                                Spacer(Modifier.height(SparkSpacing.labelGap))
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = SageSoft,
                                    contentColor = SageAccent
                                ) {
                                    Text(
                                        text = "Offline copy",
                                        style = Meta.copy(fontSize = 11.sp),
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                AnimatedContent(
                    targetState = deckRefreshState,
                    transitionSpec = {
                        (fadeIn(tween(150)) + slideInVertically(tween(170)) { it / 5 }) togetherWith
                            (fadeOut(tween(100)) + slideOutVertically(tween(100)) { -it / 6 })
                    },
                    label = "deckRefreshFeedback"
                ) { refresh ->
                    when (refresh) {
                        DeckRefreshState.Idle -> Unit
                        DeckRefreshState.Refreshing -> Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 40.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                "Checking for cards to read again…",
                                style = Meta.copy(fontSize = 12.sp),
                                color = InkSoft
                            )
                        }
                        is DeckRefreshState.Refreshed -> DeckRefreshMessage(
                            message = "Deck refreshed — ${refresh.cardsReady} " +
                                if (refresh.cardsReady == 1) "card ready to read again" else "cards ready to read again",
                            color = SageAccent,
                            showCheck = true
                        )
                        is DeckRefreshState.RestartedOffline -> DeckRefreshMessage(
                            message = "Offline deck restarted — ${refresh.cardsReady} local " +
                                if (refresh.cardsReady == 1) "card ready to read again" else "cards ready to read again",
                            color = SageAccent,
                            showCheck = true
                        )
                        is DeckRefreshState.NoCards -> DeckRefreshMessage(
                            message = "No cards are available to read again right now.",
                            color = InkSoft
                        )
                        is DeckRefreshState.Failed -> DeckRefreshMessage(
                            message = refresh.message,
                            color = Ember
                        )
                    }
                }
                }
                Spacer(Modifier.height(SparkSpacing.sectionGap))
            }
        }

        item(key = "session-limits") {
            Column {
                SectionCard(title = "Session limits") {
                    StepperRow(
                        label = "Cards",
                        value = cardLimit,
                        unit = "cards",
                        minimum = SettingsStore.MIN_CARD_LIMIT,
                        maximum = SettingsStore.MAX_CARD_LIMIT,
                        onDelta = onCardLimitDelta
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Line.copy(alpha = 0.75f))
                    )
                    StepperRow(
                        label = "Time",
                        value = minuteLimit,
                        unit = "min",
                        minimum = SettingsStore.MIN_MINUTE_LIMIT,
                        maximum = SettingsStore.MAX_MINUTE_LIMIT,
                        onDelta = onMinuteLimitDelta
                    )
                    Spacer(Modifier.height(SparkSpacing.labelGap))
                    Text(
                        text = "Changes start with your next session.",
                        style = Meta.copy(fontSize = 12.sp),
                        color = InkSoft
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        item(key = "device-data") {
            Column {
                SectionCard(title = "Device & data") {
                    val waitingText = if (pendingSync == 1) {
                        "1 change waiting to sync"
                    } else {
                        "$pendingSync changes waiting to sync"
                    }
                    val cachedText = when (cachedCount) {
                        -1 -> "Checking offline library…"
                        0 -> "No cards stored for offline reading"
                        1 -> "1 card stored for offline reading"
                        else -> "$cachedCount cards stored for offline reading"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .semantics {
                                stateDescription = if (pendingSync > 0) waitingText else "All changes synced"
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (pendingSync > 0) Ember else SageAccent)
                        )
                        Column {
                            Text("Sync", style = Meta, color = InkSoft)
                            Text(
                                text = if (pendingSync > 0) waitingText else "All changes synced",
                                style = Body,
                                color = Ink
                            )
                        }
                    }

                    Spacer(Modifier.height(SparkSpacing.rowGap))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Line.copy(alpha = 0.75f))
                    )
                    Spacer(Modifier.height(SparkSpacing.contentGap))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Offline library", style = Meta, color = InkSoft)
                            Text(text = cachedText, style = Body, color = Ink)
                        }
                        TextButton(
                            onClick = { showClearDialog = true },
                            enabled = cachedCount > 0,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Clear")
                        }
                    }
                }
                Spacer(Modifier.height(SparkSpacing.rowGap))
            }
        }

        item(key = "logout") {
            TextButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("Log out", color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear offline library?") },
            text = {
                Text("This removes cached cards from this phone. Changes waiting to sync are kept.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearCache()
                    }
                ) {
                    Text("Clear cards", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Keep cards")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = CardBg,
            titleContentColor = Ink,
            textContentColor = InkSoft
        )
    }
}

@Composable
private fun DeckRefreshMessage(message: String, color: Color, showCheck: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .padding(top = 8.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (showCheck) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = color
            )
        }
        Text(
            text = message,
            style = Meta.copy(fontSize = 12.sp),
            color = color
        )
    }
}

@Composable
private fun DeckStatusMessage(message: String, onRefresh: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = message,
            style = Body,
            color = InkSoft,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        TextButton(
            onClick = onRefresh,
            modifier = Modifier
                .align(Alignment.End)
                .heightIn(min = 48.dp)
        ) {
            Text("Refresh deck")
        }
    }
}

@Composable
private fun RollingStatNumber(
    value: Int,
    fontSize: androidx.compose.ui.unit.TextUnit = 18.sp,
    color: Color = Ink
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            (slideInVertically(tween(150)) { it / 4 } + fadeIn(tween(120))) togetherWith
                (slideOutVertically(tween(120)) { -it / 4 } + fadeOut(tween(90)))
        },
        label = "readingStat"
    ) { shownValue ->
        Text(
            text = shownValue.toString(),
            style = DisplaySmall.copy(
                fontSize = fontSize,
                lineHeight = fontSize * 1.12f,
                fontWeight = FontWeight.Medium
            ),
            color = color
        )
    }
}

@Composable
private fun StatCell(value: Int, label: String, modifier: Modifier = Modifier) {
    val numberColor = if (value == 0) InkSoft else Ink
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RollingStatNumber(value = value, fontSize = 17.sp, color = numberColor)
        Text(
            text = label,
            style = Meta.copy(fontSize = 11.sp),
            color = InkSoft
        )
    }
}

@Composable
private fun ReadingHistoryPanel(
    state: StatsUiState,
    onRetry: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ReadingSurface,
        border = BorderStroke(1.dp, Ember.copy(alpha = 0.16f)),
        modifier = Modifier
            .animateContentSize()
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "Reading history",
                style = Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                color = Ink,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(16.dp))
            when (state) {
                StatsUiState.Loading -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Loading your reading history…",
                        style = Body.copy(fontSize = 14.sp),
                        color = InkSoft
                    )
                }

                is StatsUiState.Error -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 72.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Your reading history isn’t available right now.",
                        style = Body.copy(fontSize = 14.sp),
                        color = Ink
                    )
                    Spacer(Modifier.height(SparkSpacing.labelGap))
                    Text(
                        text = state.message,
                        style = Meta.copy(fontSize = 12.sp),
                        color = InkSoft
                    )
                    TextButton(onClick = onRetry) {
                        Text("Try again")
                    }
                }

                is StatsUiState.Content -> {
                    val data = state.stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            RollingStatNumber(
                                value = data.seenTotal,
                                fontSize = 40.sp,
                                color = if (data.seenTotal > 0) Ember else InkSoft
                            )
                            Text(
                                text = if (data.seenTotal == 1) "card read" else "cards read",
                                style = Body.copy(fontSize = 13.sp),
                                color = InkSoft
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(42.dp)
                                .background(Line.copy(alpha = 0.8f))
                        )
                        Column(
                            modifier = Modifier.weight(0.8f),
                            horizontalAlignment = Alignment.End
                        ) {
                            RollingStatNumber(
                                value = data.sessionsTotal,
                                fontSize = 23.sp,
                                color = Ink
                            )
                            Text(
                                text = "sessions",
                                style = Meta.copy(fontSize = 11.sp),
                                color = InkSoft
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Line.copy(alpha = 0.65f))
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCell(
                            value = data.savedTotal,
                            label = "Saved",
                            modifier = Modifier.weight(1f)
                        )
                        StatCell(
                            value = data.likedTotal,
                            label = "Liked",
                            modifier = Modifier.weight(1f)
                        )
                        StatCell(
                            value = data.quizzesDue,
                            label = "Quizzes due",
                            modifier = Modifier.weight(1.25f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardBg,
        modifier = modifier
            .animateContentSize()
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(SparkSpacing.cardInset)) {
            Text(
                text = title,
                style = Body.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                color = Ink,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(SparkSpacing.contentGap))
            content()
        }
    }
}

@Composable
private fun StepperRow(
    label: String,
    value: Int,
    unit: String,
    minimum: Int,
    maximum: Int,
    onDelta: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = Body.copy(fontSize = 14.sp),
            color = Ink,
            modifier = Modifier.weight(1f)
        )
        StepButton(
            symbol = "−",
            description = "Decrease $label",
            enabled = value > minimum,
            onClick = { onDelta(-1) }
        )
        Text(
            text = "$value $unit",
            style = Body.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = Ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 62.dp)
        )
        StepButton(
            symbol = "+",
            description = "Increase $label",
            enabled = value < maximum,
            onClick = { onDelta(1) }
        )
    }
}

@Composable
private fun StepButton(
    symbol: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    IconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            onClick()
        },
        enabled = enabled,
        modifier = Modifier
            .size(48.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = description
            }
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (enabled) PaperDeep else PaperDeep.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = symbol,
                style = Body.copy(fontSize = 19.sp, lineHeight = 22.sp),
                color = if (enabled) Ink else InkSoft.copy(alpha = 0.6f)
            )
        }
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

private fun CardType.savedAccent(): Color = when (this) {
    CardType.BOOK_IDEA, CardType.SUMMARY -> SageAccent
    CardType.QUOTE -> Ember
    CardType.CONCEPT -> Color(0xFF8FB7D1)
    CardType.QUIZ -> Color(0xFFE6A08F)
    CardType.AUDIO -> Color(0xFFB9A9D8)
    CardType.CHALLENGE -> Color(0xFFDCC7AE)
}

private fun Card.sourceLabel(): String = when {
    source.startsWith("Wikipedia", ignoreCase = true) -> "Wikipedia"
    source.startsWith("ebook", ignoreCase = true) -> "Project Gutenberg"
    source.startsWith("Bartlett", ignoreCase = true) -> "Bartlett’s Quotations"
    else -> source.take(30)
}

private fun titleOf(bookLine: String): String {
    val parts = bookLine.split("—", "·").map { it.trim() }.filter { it.isNotEmpty() }
    return parts.firstOrNull() ?: bookLine
}

private fun authorOf(bookLine: String): String {
    val parts = bookLine.split("—", "·").map { it.trim() }.filter { it.isNotEmpty() }
    return if (parts.size >= 2) parts.last() else bookLine
}
