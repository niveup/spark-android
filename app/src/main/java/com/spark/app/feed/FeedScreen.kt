package com.spark.app.feed

import android.os.SystemClock
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.font.FontWeight
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

private val Paper = Color(0xFF121110)

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
    onOpenAudioPlayer: () -> Unit,
    onSessionFinished: (FinishReason) -> Unit,
    onCloseApp: () -> Unit,
    /** Phase 7: deck came from the Room cache (server unreachable). */
    offline: Boolean = false,
    /** Re-fetch from the server after an offline deck (tap on the pill). */
    onRetry: (() -> Unit)? = null,
    deckRefreshState: DeckRefreshState = DeckRefreshState.Idle,
    onReadDeckAgain: () -> Unit = {}
) {
    val haptics = LocalHapticFeedback.current
    // Room can hold cards from several online sessions. Keep offline reading
    // finite too by showing only the cards allowed in this session.
    val sessionCards = remember(cards, cardLimit) { cards.take(cardLimit) }

    Crossfade(
        targetState = state.done,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "feedScreenCrossfade"
    ) { isDone ->
        if (isDone) {
            val reason = state.finishReason
            LaunchedEffect(reason) {
                if (reason != null) onSessionFinished(reason)
            }
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                color = MaterialTheme.colorScheme.background
            ) {
                DoneScreen(
                    reason = reason,
                    seenCount = state.seenCount,
                    savedCards = cards.filter { state.saved.contains(it.id) },
                    onCloseApp = onCloseApp,
                    onReadAgain = onReadDeckAgain,
                    deckRefreshState = deckRefreshState
                )
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { sessionCards.size })
            var lastShown by remember(cards) { mutableStateOf<LastShown?>(null) }
            var previousSettledPage by remember { mutableIntStateOf(0) }

            // Tactile settle feedback on page snap
            LaunchedEffect(pagerState.settledPage) {
                if (pagerState.settledPage != previousSettledPage) {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    previousSettledPage = pagerState.settledPage
                }
            }

            // On each settled page: send dwell for the card we just left, mark the new
            // card seen. Events are batched by InteractionSync (no network per swipe).
            LaunchedEffect(pagerState.currentPage) {
                tts.stop()
                val page = pagerState.currentPage
                state.onPageSettled(page)
                val card = sessionCards.getOrNull(page) ?: return@LaunchedEffect
                val previous = lastShown
                if (previous != null && previous.cardId != card.id) {
                    val dwellMs = (SystemClock.elapsedRealtime() - previous.atMs).coerceAtMost(MAX_DWELL_MS)
                    if (dwellMs >= MIN_DWELL_MS) sync.record(StateEvents.dwell(previous.cardId, dwellMs))
                }
                sync.record(StateEvents.seen(card.id))
                lastShown = LastShown(card.id, SystemClock.elapsedRealtime())
            }

            // Session time limit: finish the session when the minutes run out.
            LaunchedEffect(sessionCards, minuteLimit, startedAtElapsedMs) {
                while (!state.done) {
                    delay(TIME_TICK_MS)
                    val elapsedMs = SystemClock.elapsedRealtime() - startedAtElapsedMs
                    if (elapsedMs >= minuteLimit * 60_000L) {
                        recordCurrentDwell(lastShown, sync)
                        state.finish(FinishReason.TIME_LIMIT)
                    }
                }
            }

            // Fluid snap fling physics
            val flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapAnimationSpec = spring(
                    dampingRatio = 0.82f,
                    stiffness = 420f
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                VerticalPager(
                    state = pagerState,
                    flingBehavior = flingBehavior,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val card = sessionCards[page]

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Paper)
                            .graphicsLayer {
                                // Read pager motion in the layer block. This updates transforms
                                // per frame without recomposing the full card content.
                                val pageOffset =
                                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                                val absOffset = kotlin.math.abs(pageOffset).coerceIn(0f, 1f)
                                val scale = 1f - (absOffset * 0.05f)
                                val isTopBound = page == 0 && pagerState.currentPage == 0 &&
                                    pagerState.currentPageOffsetFraction < 0f
                                val isBottomBound = page == cards.lastIndex &&
                                    pagerState.currentPage == cards.lastIndex &&
                                    pagerState.currentPageOffsetFraction > 0f
                                val overscrollDamping = if (isTopBound || isBottomBound) {
                                    val fraction = pagerState.currentPageOffsetFraction
                                    val sign = if (fraction > 0f) 1f else -1f
                                    sign * kotlin.math.sqrt(kotlin.math.abs(fraction)) * 0.32f
                                } else {
                                    0f
                                }

                                scaleX = scale
                                scaleY = if (isTopBound || isBottomBound) {
                                    scale * (1f - kotlin.math.abs(overscrollDamping) * 0.06f)
                                } else {
                                    scale
                                }
                                alpha = 1f - (absOffset * 0.28f)
                                rotationX = pageOffset.coerceIn(-1f, 1f) * 4.5f
                                cameraDistance = 14f * density
                                if (overscrollDamping != 0f) {
                                    translationY = -overscrollDamping * size.height * 0.18f
                                }
                            }
                    ) {
                        CardView(
                            card = card,
                            counter = "${page + 1}/${sessionCards.size}",
                            tts = tts,
                            state = state,
                            sync = sync,
                            player = player,
                            onOpenAudioPlayer = onOpenAudioPlayer,
                            showFinish = page == sessionCards.lastIndex,
                            onFinish = {
                                recordCurrentDwell(lastShown, sync)
                                state.finish(
                                    if (sessionCards.size >= cardLimit) FinishReason.CARD_LIMIT
                                    else FinishReason.STACK_DONE
                                )
                            },
                            offline = offline,
                            onRetryOffline = onRetry
                        )
                    }
                }
            }
        }
    }
}

private fun recordCurrentDwell(lastShown: LastShown?, sync: InteractionSync) {
    if (lastShown == null) return
    val dwellMs = (SystemClock.elapsedRealtime() - lastShown.atMs).coerceAtMost(MAX_DWELL_MS)
    if (dwellMs >= MIN_DWELL_MS) sync.record(StateEvents.dwell(lastShown.cardId, dwellMs))
}
