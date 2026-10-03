package com.spark.app.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.spark.app.audio.TtsHelper
import com.spark.app.data.Card

@Composable
fun FeedScreen(
    state: FeedState,
    cards: List<Card>,
    tts: TtsHelper,
    onCloseApp: () -> Unit
) {
    if (state.done) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            DoneScreen(
                seenCount = state.seenCount,
                savedCards = cards.filter { state.saved.contains(it.id) },
                onCloseApp = onCloseApp
            )
        }
        return
    }
    val pagerState = rememberPagerState(pageCount = { cards.size })
    LaunchedEffect(pagerState.currentPage) {
        tts.stop()
        state.onPageSettled(pagerState.currentPage)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val card = cards[page]
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                CardView(
                    card = card,
                    counter = "${state.seenCount}/${cards.size}",
                    tts = tts,
                    state = state,
                    showFinish = page == cards.lastIndex
                )
            }
        }
    }
}
