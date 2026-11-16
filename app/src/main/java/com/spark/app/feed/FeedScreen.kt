package com.spark.app.feed

import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spark.app.audio.AudioPlayerBox
import com.spark.app.audio.TtsHelper
import com.spark.app.data.Card
import com.spark.app.sync.InteractionSync
import com.spark.app.sync.StateEvents
import kotlinx.coroutines.delay

private const val MIN_DWELL_MS = 700L
private const val MAX_DWELL_MS = 10 * 60 * 1000L
private const val TIME_TICK_MS = 5_000L

private data class LastShown(val cardId: String, val atMs: Long)

@Composable
fun FeedScreen(
    state: FeedState,
    cards: List<Card>,
    cardLimit: Int,
    minuteLimit: Int,
    startedAtElapsedMs: Long,
    tts: TtsHelper,
    sync: InteractionSync,
    player: AudioPlayerBox?,
    onSessionFinished: (FinishReason) -> Unit,
    onCloseApp: () -> Unit,
    /** Phase 7: deck came from the Room cache (server unreachable). */
    offline: Boolean = false
) {
    if (state.done) {
        val reason = state.finishReason
        LaunchedEffect(Unit) {
            if (reason != null) onSessionFinished(reason)
        }
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            DoneScreen(
                reason = reason,
                seenCount = state.seenCount,
                savedCards = cards.filter { state.saved.contains(it.id) },
                onCloseApp = onCloseApp
            )
        }
        return
    }
    val pagerState = rememberPagerState(pageCount = { cards.size })
    var lastShown by remember(cards) { mutableStateOf<LastShown?>(null) }

    // On each settled page: send dwell for the card we just left, mark the new
    // card seen. Events are batched by InteractionSync (no network per swipe).
    LaunchedEffect(pagerState.currentPage) {
        tts.stop()
        val page = pagerState.currentPage
        state.onPageSettled(page)
        val card = cards.getOrNull(page) ?: return@LaunchedEffect
        val previous = lastShown
        if (previous != null && previous.cardId != card.id) {
            val dwellMs = (SystemClock.elapsedRealtime() - previous.atMs).coerceAtMost(MAX_DWELL_MS)
            if (dwellMs >= MIN_DWELL_MS) sync.record(StateEvents.dwell(previous.cardId, dwellMs))
        }
        sync.record(StateEvents.seen(card.id))
        lastShown = LastShown(card.id, SystemClock.elapsedRealtime())
    }

    // Session time limit: finish the session when the minutes run out.
    LaunchedEffect(cards, minuteLimit, startedAtElapsedMs) {
        while (!state.done) {
            delay(TIME_TICK_MS)
            val elapsedMs = SystemClock.elapsedRealtime() - startedAtElapsedMs
            if (elapsedMs >= minuteLimit * 60_000L) {
                recordCurrentDwell(lastShown, sync)
                state.finish(FinishReason.TIME_LIMIT)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val card = cards[page]
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                CardView(
                    card = card,
                    counter = "${page + 1}/${cards.size}",
                    tts = tts,
                    state = state,
                    sync = sync,
                    player = player,
                    showFinish = page == cards.lastIndex,
                    onFinish = {
                        recordCurrentDwell(lastShown, sync)
                        state.finish(
                            if (cards.size >= cardLimit) FinishReason.CARD_LIMIT
                            else FinishReason.STACK_DONE
                        )
                    }
                )
            }
        }
    }
}

private fun recordCurrentDwell(lastShown: LastShown?, sync: InteractionSync) {
    if (lastShown == null) return
    val dwellMs = (SystemClock.elapsedRealtime() - lastShown.atMs).coerceAtMost(MAX_DWELL_MS)
    if (dwellMs >= MIN_DWELL_MS) sync.record(StateEvents.dwell(lastShown.cardId, dwellMs))
}
