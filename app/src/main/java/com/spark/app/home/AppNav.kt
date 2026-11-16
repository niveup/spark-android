package com.spark.app.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spark.app.audio.AudioPlayerBox
import com.spark.app.audio.AudioPlayerOverlay
import com.spark.app.audio.MiniAudioPlayer
import com.spark.app.audio.TtsHelper
import com.spark.app.data.CardType
import com.spark.app.data.SavedExportFormat
import com.spark.app.data.SavedExporter
import com.spark.app.data.SettingsStore
import com.spark.app.data.TokenStore
import com.spark.app.data.local.OfflineCache
import com.spark.app.data.remote.SparkApi
import com.spark.app.feed.FeedScreen
import com.spark.app.feed.FeedState
import com.spark.app.feed.FeedUiState
import com.spark.app.feed.FeedViewModel
import com.spark.app.sync.InteractionSync
import com.spark.app.ui.EmptyState
import com.spark.app.ui.ErrorState
import com.spark.app.ui.LoadingState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private val Paper = Color(0xFF121110)
private val PaperDeep = Color(0xFF2F2C2A)
private val CardBg = Color(0xFF1A1817)
private val InkSoft = Color(0xFFCFC7BE)
private val Line = Color(0xFF49433D)
private val Forest = Color(0xFFECE6E0)

private enum class Tab(val label: String) {
    HOME("Home"),
    REELS("Reels"),
    SAVED("Saved"),
    ME("Me")
}

/**
 * Material 3 convention: the selected destination gets the solid icon, the
 * others the outline. Selection therefore reads from shape as well as colour,
 * which the old text glyphs could not express.
 */
private fun Tab.icon(selected: Boolean): ImageVector = when (this) {
    Tab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    Tab.REELS -> if (selected) Icons.AutoMirrored.Filled.List else Icons.AutoMirrored.Outlined.List
    Tab.SAVED -> if (selected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder
    Tab.ME -> if (selected) Icons.Filled.Person else Icons.Outlined.Person
}

@Composable
fun AppNav(
    api: SparkApi,
    tokens: TokenStore,
    startOnReels: Boolean,
    onStartOnReelsHandled: () -> Unit,
    onCloseApp: () -> Unit,
    onLogout: () -> Unit,
    isOffline: Boolean,
    onRetryConnection: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { SettingsStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    // Phase 7: Room-backed offline cache — deck/saved snapshots + pending
    // interaction events. It doubles as InteractionSync's persistent store.
    val offline = remember { OfflineCache(context.applicationContext) }
    val sync = remember(api, tokens, scope) { InteractionSync(api, tokens, scope, store = offline) }

    val feedVm: FeedViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FeedViewModel(api, tokens, settings, offline) as T
        }
    })
    val savedVm: SavedViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SavedViewModel(api, tokens, offline) as T
        }
    })
    val meVm: MeViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MeViewModel(api, tokens) as T
        }
    })
    val feed by feedVm.state.collectAsStateWithLifecycle()
    val saved by savedVm.state.collectAsStateWithLifecycle()
    val stats by meVm.state.collectAsStateWithLifecycle()
    val deckRefresh by feedVm.deckRefreshState.collectAsStateWithLifecycle()
    val pendingSync by sync.pending.collectAsStateWithLifecycle()
    val state = remember { FeedState() }
    LaunchedEffect(Unit) {
        feedVm.load(onDeckReplaced = state::resetSession)
        meVm.refresh()
    }

    // The server is the source of truth for saved/rated state — seed each fresh load.
    LaunchedEffect(feed) {
        (feed as? FeedUiState.Content)?.let { state.seedFrom(it.cards) }
    }

    // Home shows the card the reader will meet next, plus a real grounded
    // quote from the same deck. Both come from data the app already holds;
    // nothing on Home is a sample or an invented statistic.
    val currentFeed = feed
    val feedContent = currentFeed as? FeedUiState.Content
    val feedCards = feedContent?.cards.orEmpty()
    val sessionTotal = feedContent?.let { minOf(it.cards.size, it.cardLimit) } ?: 0
    val nextCard = feedCards.getOrNull(state.seenCount.takeIf { it < sessionTotal } ?: -1)
    val dailyQuote = feedCards.firstOrNull { card ->
        card.type == CardType.QUOTE &&
            card.grounded &&
            card.id != nextCard?.id
    }
    var tab by rememberSaveable { mutableStateOf(if (startOnReels) Tab.REELS else Tab.HOME) }
    var contentTab by rememberSaveable { mutableStateOf(tab) }
    val transitionCover = remember { Animatable(0f) }
    val laidOutTab = remember { MutableStateFlow<Tab?>(contentTab) }
    var awaitingLayoutTab by remember { mutableStateOf<Tab?>(null) }
    LaunchedEffect(tab) {
        val targetTab = tab
        if (targetTab != contentTab) {
            // Fade the page into the app's charcoal surface before switching.
            // Drawing a translucent surface avoids an offscreen buffer for the
            // full 1080p page while preserving the same quiet fade.
            transitionCover.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing)
            )
            laidOutTab.value = null
            awaitingLayoutTab = targetTab
            contentTab = targetTab
            // Keep the charcoal surface in place until the destination has
            // completed its first layout, so a slow screen entry never flashes
            // a partially measured page during the reveal.
            try {
                laidOutTab.first { it == targetTab }
            } finally {
                awaitingLayoutTab = null
            }
        }
        transitionCover.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing)
        )
    }
    LaunchedEffect(startOnReels) {
        if (startOnReels) {
            tab = Tab.REELS
            onStartOnReelsHandled()
        }
    }
    val tts = remember { TtsHelper(context.applicationContext) }
    DisposableEffect(Unit) {
        onDispose { tts.shutdown() }
    }
    // Phase 6: one audiobook player for the whole app — audio keeps playing
    // when you leave the Reels tab or the app entirely (service + lock screen).
    val player = remember { AudioPlayerBox(context.applicationContext) }
    // The player emits position updates twice a second. The tab shell only
    // needs to know whether a player surface exists; the player surfaces
    // collect detailed playback state themselves.
    val hasAudioFlow = remember(player) {
        player.state.map { it.url != null }.distinctUntilChanged()
    }
    val hasAudio by hasAudioFlow.collectAsStateWithLifecycle(initialValue = false)
    var showAudioPlayer by rememberSaveable { mutableStateOf(false) }
    DisposableEffect(Unit) {
        player.start()
        onDispose { player.release() }
    }
    var previousTab by remember { mutableStateOf(tab) }
    LaunchedEffect(tab) {
        val exitedReels = previousTab == Tab.REELS && tab != Tab.REELS
        previousTab = tab
        if (exitedReels) {
            tts.stop()
            sync.flush() // leaving Reels is a natural checkpoint for seen/dwell events
        }
    }

    // Session limits: local state drives the steppers, the store persists them.
    var cardLimit by remember { mutableStateOf(settings.cardLimit) }
    var minuteLimit by remember { mutableStateOf(settings.minuteLimit) }

    // Saved tab: load on open + debounced while typing in the search box.
    // Upload a just-recorded save first, but never block the tab forever on a
    // dead network: after 2s, show the server/cache result. Keying on
    // pendingSync refreshes once more when the queued save reaches the server,
    // so a slow upload cannot leave the just-saved card missing.
    var savedQuery by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(tab, savedQuery, pendingSync) {
        if (tab == Tab.SAVED) {
            savedVm.showCached(savedQuery, force = pendingSync > 0)
            if (savedQuery.isNotBlank()) delay(300)
            withTimeoutOrNull(2_000) { sync.awaitFlush() }
            if (sync.pending.value > 0) sync.flush()
            savedVm.load(savedQuery)
        }
    }
    // Offline cache row on the Me tab (+ refreshed after clearing).
    var cachedCount by remember { mutableIntStateOf(-1) }
    LaunchedEffect(tab) {
        if (tab == Tab.ME) {
            meVm.refresh()
            offline.observeCachedCount().collect { cachedCount = it }
        }
    }

    val tabStateHolder = rememberSaveableStateHolder()

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Paper
        ) {
            tabStateHolder.SaveableStateProvider(contentTab.name) {
                key(contentTab) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (awaitingLayoutTab == contentTab) {
                                    Modifier.onGloballyPositioned {
                                        laidOutTab.value = contentTab
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .drawWithContent {
                                if (transitionCover.value < 1f) drawContent()
                                drawRect(Paper.copy(alpha = transitionCover.value))
                            }
                    ) {
                    when (contentTab) {
                        Tab.HOME -> HomeScreen(
                            seenCount = state.seenCount,
                            savedCount = state.saved.size,
                            // The loaded session owns its limits; the Me-tab values are
                            // only a fallback while the deck is still loading.
                            cardLimit = feedContent?.cardLimit ?: cardLimit,
                            minuteLimit = feedContent?.minuteLimit ?: minuteLimit,
                            deckSize = when (currentFeed) {
                                is FeedUiState.Loading -> null
                                is FeedUiState.Content -> currentFeed.cards.size
                                is FeedUiState.Error, is FeedUiState.Empty -> 0
                            },
                            deckErrorMessage = (currentFeed as? FeedUiState.Error)?.message,
                            offline = isOffline,
                            onRetryConnection = {
                                onRetryConnection()
                                feedVm.load(onDeckReplaced = state::resetSession)
                            },
                            onRetryDeck = { feedVm.load(onDeckReplaced = state::resetSession) },
                            nextCard = nextCard,
                            dailyQuote = dailyQuote,
                            bottomClearance = if (tab != Tab.REELS && hasAudio) 180.dp else 100.dp,
                            onOpenReels = { tab = Tab.REELS },
                            onOpenSaved = { tab = Tab.SAVED },
                            onOpenMe = { tab = Tab.ME }
                        )
                        Tab.REELS -> when (val f = feed) {
                            is FeedUiState.Loading -> CenteredFullScreen {
                                LoadingState("Fetching today's cards…")
                            }
                            is FeedUiState.Empty -> CenteredFullScreen {
                                EmptyState(
                                    text = "Nothing new right now — you're all caught up.",
                                    actionLabel = "Read them again",
                                    onAction = {
                                        feedVm.load(
                                            refill = true,
                                            onDeckReplaced = state::resetSession
                                        )
                                    }
                                )
                            }
                            is FeedUiState.Error -> CenteredFullScreen {
                                ErrorState(
                                    f.message,
                                    onRetry = { feedVm.load(onDeckReplaced = state::resetSession) }
                                )
                            }
                            is FeedUiState.Content -> key(f.deckGeneration) {
                                FeedScreen(
                                    state = state,
                                    cards = f.cards,
                                    cardLimit = f.cardLimit,
                                    minuteLimit = f.minuteLimit,
                                    startedAtElapsedMs = f.startedAtElapsedMs,
                                    offline = f.offline || isOffline,
                                    deckRefreshState = deckRefresh,
                                    onReadDeckAgain = {
                                        feedVm.refreshDeck(
                                            onDeckReplaced = state::resetSession,
                                            offlineOnly = f.offline || isOffline
                                        )
                                    },
                                    onRetry = if (f.offline || isOffline) ({
                                        onRetryConnection()
                                        feedVm.load(
                                            refill = true,
                                            onDeckReplaced = state::resetSession
                                        )
                                    }) else null,
                                    tts = tts,
                                    sync = sync,
                                    player = player,
                                    onOpenAudioPlayer = { showAudioPlayer = true },
                                    onSessionFinished = { reason ->
                                        feedVm.closeSession(reason, state.seenCount)
                                        sync.flush()
                                    },
                                    onCloseApp = onCloseApp
                                )
                            }
                        }
                        Tab.SAVED -> SavedScreen(
                            state = saved,
                            query = savedQuery,
                            onQueryChange = { savedQuery = it },
                            onRetry = { savedVm.load(savedQuery) },
                            offline = isOffline,
                            onRetryConnection = {
                                onRetryConnection()
                                savedVm.load(savedQuery)
                            },
                            onOpenReels = { tab = Tab.REELS },
                            bottomClearance = if (hasAudio) 180.dp else 100.dp,
                            onExport = { format ->
                                val cards = (saved as? SavedUiState.Content)?.cards.orEmpty()
                                SavedExporter.share(context, cards, format)
                            }
                        )
                        Tab.ME -> MeScreen(
                            stats = stats,
                            deckRefreshState = deckRefresh,
                            offline = isOffline,
                            onRetryConnection = onRetryConnection,
                            cardLimit = cardLimit,
                            minuteLimit = minuteLimit,
                            pendingSync = pendingSync,
                            onCardLimitDelta = { delta ->
                                val next = SettingsStore.clampCards(cardLimit + delta)
                                cardLimit = next
                                settings.cardLimit = next
                            },
                            onMinuteLimitDelta = { delta ->
                                val next = SettingsStore.clampMinutes(minuteLimit + delta)
                                minuteLimit = next
                                settings.minuteLimit = next
                            },
                            onRetryStats = { meVm.refresh() },
                            deckState = currentFeed,
                            // Refill mode: saved cards first, then previously read, so a
                            // deck that has been fully read still comes back with cards.
                            onRefreshDeck = {
                                feedVm.refreshDeck(
                                    onDeckReplaced = state::resetSession,
                                    offlineOnly = isOffline
                                )
                            },
                            bottomClearance = if (hasAudio) 180.dp else 96.dp,
                            cachedCount = cachedCount,
                            onClearCache = {
                                scope.launch {
                                    offline.clearCachedCards()
                                }
                            },
                            onLogout = onLogout
                        )
                    }
                }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (tab != Tab.REELS && hasAudio) {
                MiniAudioPlayer(
                    player = player,
                    onOpen = { showAudioPlayer = true },
                    modifier = Modifier.widthIn(max = 400.dp)
                )
            }
            SparkBottomBar(
                selectedTab = tab,
                onTabSelected = { tab = it },
                modifier = Modifier.widthIn(max = 400.dp)
            )
        }

        if (showAudioPlayer && hasAudio) {
            AudioPlayerOverlay(
                player = player,
                onDismiss = { showAudioPlayer = false }
            )
        }
    }
}

/**
 * A quiet floating navigation bar with an in-place fading selection surface.
 * The page transitions separately, so the selected surface never leaves a trail.
 */
@Composable
private fun SparkBottomBar(
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 400.dp)
            .height(64.dp)
            .clip(shape)
            .background(CardBg)
            .border(1.dp, Line, shape)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Tab.entries.forEach { entry ->
                SparkBottomBarItem(
                    entry = entry,
                    selected = selectedTab == entry,
                    onClick = { onTabSelected(entry) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.SparkBottomBarItem(
    entry: Tab,
    selected: Boolean,
    onClick: () -> Unit
) {
    val activeColor = Forest
    val inactiveColor = InkSoft
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current

    val iconTint = if (selected) activeColor else inactiveColor
    val textColor = if (selected) activeColor else inactiveColor
    val selectionProgress = animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "tabSelectionSurface"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "tabItemScale"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .drawBehind {
                val alpha = selectionProgress.value
                if (alpha > 0f) {
                    val radius = 20.dp.toPx()
                    drawRoundRect(
                        color = PaperDeep.copy(alpha = alpha),
                        cornerRadius = CornerRadius(radius, radius)
                    )
                }
            }
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = {
                    if (!selected) {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onClick()
                    }
                }
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = entry.icon(selected),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = entry.label,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = textColor
        )
    }
}
@Composable
private fun CenteredFullScreen(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        content()
    }
}
