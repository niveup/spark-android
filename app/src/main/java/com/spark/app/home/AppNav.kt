package com.spark.app.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spark.app.audio.AudioPlayerBox
import com.spark.app.audio.TtsHelper
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
    onLogout: () -> Unit
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
    val pendingSync by sync.pending.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { feedVm.load() }

    val state = remember { FeedState() }
    // The server is the source of truth for saved/rated state — seed each fresh load.
    LaunchedEffect(feed) {
        (feed as? FeedUiState.Content)?.let { state.seedFrom(it.cards) }
    }
    var tab by remember { mutableStateOf(if (startOnReels) Tab.REELS else Tab.HOME) }
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
    DisposableEffect(Unit) {
        player.start()
        onDispose { player.release() }
    }
    LaunchedEffect(tab) {
        if (tab != Tab.REELS) {
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
    var savedQuery by remember { mutableStateOf("") }
    LaunchedEffect(tab, savedQuery, pendingSync) {
        if (tab == Tab.SAVED) {
            if (savedQuery.isNotBlank()) delay(300)
            withTimeoutOrNull(2_000) { sync.awaitFlush() }
            if (sync.pending.value > 0) sync.flush()
            savedVm.load(savedQuery)
        }
    }
    // Offline cache row on the Me tab (+ refreshed after clearing).
    var cachedCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(tab) {
        if (tab == Tab.ME) {
            meVm.refresh()
            cachedCount = offline.cachedCount()
        }
    }

    Scaffold(
        bottomBar = {
            // Quiet tone step (surfaceContainerLow) instead of a divider line:
            // the bar separates from the page by luminance, not by a hairline.
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp
            ) {
                Tab.entries.forEach { entry ->
                    val selected = tab == entry
                    NavigationBarItem(
                        selected = selected,
                        onClick = { tab = entry },
                        icon = {
                            // Indicator, hand-drawn: Material 3's default is a
                            // 64x32 dp stadium filled with indicatorColor. At
                            // amber-on-dark that fill reads as muddy gold, and
                            // it is far wider than the 24 dp glyph it sits
                            // behind. A compact neutral key (tone step, not a
                            // tint) marks "where I am" quietly, and leaves the
                            // amber for the icon and label only.
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(28.dp)
                                    .background(
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.surfaceContainerHighest
                                        } else {
                                            Color.Transparent
                                        },
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = entry.icon(selected),
                                    // The label already names the destination,
                                    // so the icon must not repeat it in TalkBack.
                                    contentDescription = null
                                )
                            }
                        },
                        label = {
                            Text(
                                text = entry.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            // The built-in indicator is replaced above; keep its
                            // own fill out of the way.
                            indicatorColor = Color.Transparent,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            when (tab) {
                Tab.HOME -> HomeScreen(
                    seenCount = state.seenCount,
                    savedCount = state.saved.size,
                    cardLimit = cardLimit,
                    minuteLimit = minuteLimit,
                    onOpenReels = { tab = Tab.REELS },
                    onOpenSaved = { tab = Tab.SAVED }
                )
                Tab.REELS -> when (val f = feed) {
                    is FeedUiState.Loading -> CenteredFullScreen {
                        LoadingState("Fetching today's cards…")
                    }
                    is FeedUiState.Empty -> CenteredFullScreen {
                        EmptyState(
                            "Nothing new right now — you're all caught up."
                        )
                    }
                    is FeedUiState.Error -> CenteredFullScreen {
                        ErrorState(f.message, onRetry = { feedVm.load() })
                    }
                    is FeedUiState.Content -> FeedScreen(
                        state = state,
                        cards = f.cards,
                        cardLimit = f.cardLimit,
                        minuteLimit = f.minuteLimit,
                        startedAtElapsedMs = f.startedAtElapsedMs,
                        offline = f.offline,
                        tts = tts,
                        sync = sync,
                        player = player,
                        onSessionFinished = { reason ->
                            feedVm.closeSession(reason, state.seenCount)
                            sync.flush()
                        },
                        onCloseApp = onCloseApp
                    )
                }
                Tab.SAVED -> SavedScreen(
                    state = saved,
                    query = savedQuery,
                    onQueryChange = { savedQuery = it },
                    onRetry = { savedVm.load(savedQuery) },
                    onExport = { format ->
                        val cards = (saved as? SavedUiState.Content)?.cards.orEmpty()
                        SavedExporter.share(context, cards, format)
                    }
                )
                Tab.ME -> MeScreen(
                    stats = stats,
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
                    cachedCount = cachedCount,
                    onClearCache = {
                        scope.launch {
                            offline.clearAll()
                            cachedCount = 0
                        }
                    },
                    onLogout = onLogout
                )
            }
        }
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
