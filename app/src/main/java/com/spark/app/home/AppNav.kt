package com.spark.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.spark.app.audio.TtsHelper
import com.spark.app.data.TokenStore
import com.spark.app.data.remote.SparkApi
import com.spark.app.feed.FeedScreen
import com.spark.app.feed.FeedState
import com.spark.app.feed.FeedUiState
import com.spark.app.feed.FeedViewModel
import com.spark.app.ui.EmptyState
import com.spark.app.ui.ErrorState
import com.spark.app.ui.LoadingState

private enum class Tab(val label: String) {
    HOME("Home"),
    REELS("Reels"),
    SAVED("Saved"),
    ME("Me")
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
    val feedVm: FeedViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FeedViewModel(api, tokens) as T
        }
    })
    val feed by feedVm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { feedVm.load() }

    val state = remember { FeedState() }
    var tab by remember { mutableStateOf(if (startOnReels) Tab.REELS else Tab.HOME) }
    LaunchedEffect(startOnReels) {
        if (startOnReels) {
            tab = Tab.REELS
            onStartOnReelsHandled()
        }
    }
    val context = LocalContext.current
    val tts = remember { TtsHelper(context.applicationContext) }
    DisposableEffect(Unit) {
        onDispose { tts.shutdown() }
    }
    LaunchedEffect(tab) {
        if (tab != Tab.REELS) tts.stop()
    }
    val feedCards = (feed as? FeedUiState.Content)?.cards.orEmpty()

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = {
                            Text(
                                when (entry) {
                                    Tab.HOME -> "⌂"
                                    Tab.REELS -> "▤"
                                    Tab.SAVED -> "♡"
                                    Tab.ME -> "☺"
                                }
                            )
                        },
                        label = { Text(entry.label) }
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
                    onOpenReels = { tab = Tab.REELS },
                    onOpenSaved = { tab = Tab.SAVED }
                )
                Tab.REELS -> when (val f = feed) {
                    is FeedUiState.Loading -> CenteredFullScreen {
                        LoadingState("Fetching today's cards…")
                    }
                    is FeedUiState.Empty -> CenteredFullScreen {
                        EmptyState(
                            "No cards published yet. The content pipeline will fill this soon."
                        )
                    }
                    is FeedUiState.Error -> CenteredFullScreen {
                        ErrorState(f.message, onRetry = { feedVm.load() })
                    }
                    is FeedUiState.Content -> FeedScreen(
                        state = state,
                        cards = f.cards,
                        tts = tts,
                        onCloseApp = onCloseApp
                    )
                }
                Tab.SAVED -> SavedScreen(
                    savedCards = feedCards.filter { state.saved.contains(it.id) }
                )
                Tab.ME -> MeScreen(
                    seenCount = state.seenCount,
                    savedCount = state.saved.size,
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
