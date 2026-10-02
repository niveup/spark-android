package com.spark.app.home

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.spark.app.audio.TtsHelper
import com.spark.app.data.sampleCards
import com.spark.app.feed.FeedScreen
import com.spark.app.feed.FeedState

private enum class Tab(val label: String) {
    HOME("Home"),
    REELS("Reels"),
    SAVED("Saved"),
    ME("Me")
}

@Composable
fun AppNav(onCloseApp: () -> Unit) {
    val cards = remember { sampleCards }
    val state = remember { FeedState() }
    var tab by remember { mutableStateOf(Tab.HOME) }
    val context = LocalContext.current
    val tts = remember { TtsHelper(context.applicationContext) }
    DisposableEffect(Unit) {
        onDispose { tts.shutdown() }
    }
    LaunchedEffect(tab) {
        if (tab != Tab.REELS) tts.stop()
    }
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
                Tab.REELS -> FeedScreen(
                    state = state,
                    cards = cards,
                    tts = tts,
                    onCloseApp = onCloseApp
                )
                Tab.SAVED -> SavedScreen(
                    savedCards = cards.filter { state.saved.contains(it.id) }
                )
                Tab.ME -> MeScreen(
                    seenCount = state.seenCount,
                    savedCount = state.saved.size
                )
            }
        }
    }
}
