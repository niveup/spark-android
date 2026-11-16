package com.spark.app.feed

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spark.app.data.Card
import com.spark.app.data.SettingsStore
import com.spark.app.data.local.ItemCache
import com.spark.app.data.remote.SessionCloseRequest
import com.spark.app.data.remote.SessionRequest
import com.spark.app.data.remote.SparkApi
import com.spark.app.sync.TokenReader
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface DeckRefreshState {
    data object Idle : DeckRefreshState
    data object Refreshing : DeckRefreshState
    data class Refreshed(val cardsReady: Int, val attemptId: Long = 0L) : DeckRefreshState
    data class RestartedOffline(val cardsReady: Int, val attemptId: Long = 0L) : DeckRefreshState
    data class NoCards(val attemptId: Long = 0L) : DeckRefreshState
    data class Failed(val message: String, val attemptId: Long = 0L) : DeckRefreshState
}

sealed interface FeedUiState {
    data object Loading : FeedUiState
    data object Empty : FeedUiState
    data class Error(val message: String) : FeedUiState
    data class Content(
        val cards: List<Card>,
        val cardLimit: Int,
        val minuteLimit: Int,
        val startedAtElapsedMs: Long,
        /** True when the deck came from the Room cache (server unreachable). */
        val offline: Boolean = false,
        /** Changes only when a new server deck replaces the current one. */
        val deckGeneration: Long = 0L
    ) : FeedUiState
}

/** What happened when the reader opened a saved card from the Saved tab. */
sealed interface ItemOpenState {
    data object Idle : ItemOpenState

    /** The card is being resolved (server first, then this device's cache). */
    data class Opening(val itemId: String) : ItemOpenState

    /** The card is ready and is now the first card of the Reels deck. */
    data class Opened(val itemId: String, val fromCache: Boolean) : ItemOpenState

    /**
     * The card is neither on the server nor on this device — the offline
     * deck is shown instead.
     */
    data class Unavailable(val itemId: String) : ItemOpenState
}

/**
 * Loads the ranked feed from the API and owns the server-side session
 * (Phase 5b): POST /sessions on load, PATCH /sessions/:id on finish.
 * Session limits come from SettingsStore (Me tab).
 */
class FeedViewModel(
    private val api: SparkApi,
    private val tokens: TokenReader,
    private val settings: SettingsStore,
    private val offline: ItemCache
) : ViewModel() {

    private val _state = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    private val opener = SavedItemOpener(api, tokens, offline)

    private var openedSessionId: String? = null
    private var sessionJob: Job? = null
    private var loadJob: Job? = null
    private var sessionClosed = false
    private var deckGeneration = 0L
    private var refreshAttemptId = 0L

    private val _deckRefreshState = MutableStateFlow<DeckRefreshState>(DeckRefreshState.Idle)
    val deckRefreshState: StateFlow<DeckRefreshState> = _deckRefreshState.asStateFlow()

    private val _itemOpenState = MutableStateFlow<ItemOpenState>(ItemOpenState.Idle)
    val itemOpenState: StateFlow<ItemOpenState> = _itemOpenState.asStateFlow()

    private var openJob: Job? = null

    /**
     * Opens a saved card in the Reels deck (the Saved → Reels flow).
     *
     * The Saved tab only stores the card id, so opening it maps that id back
     * to a card: from the server when there is a connection, otherwise from
     * this device's cache. The card becomes the first card of the deck and
     * the session restarts on it. When the card is nowhere to be found, the
     * offline deck is served instead.
     */
    fun openSavedItem(
        itemId: String,
        offlineOnly: Boolean = false,
        onDeckReplaced: () -> Unit = {}
    ) {
        openJob?.cancel()
        openJob = viewModelScope.launch {
            _itemOpenState.value = ItemOpenState.Opening(itemId)
            when (val result = opener.open(itemId, offlineOnly)) {
                is SavedItemResult.Found -> {
                    val current = (_state.value as? FeedUiState.Content)?.cards.orEmpty()
                    // The opened card leads the deck; the rest of the session
                    // follows it, so the reader can keep swiping.
                    val deck = listOf(result.card) + current.filter { it.id != result.card.id }
                    onDeckReplaced()
                    _state.value = FeedUiState.Content(
                        cards = deck,
                        cardLimit = settings.cardLimit,
                        minuteLimit = settings.minuteLimit,
                        startedAtElapsedMs = SystemClock.elapsedRealtime(),
                        offline = result.fromCache,
                        deckGeneration = ++deckGeneration
                    )
                    _itemOpenState.value = ItemOpenState.Opened(itemId, result.fromCache)
                }

                SavedItemResult.Unavailable -> {
                    _itemOpenState.value = ItemOpenState.Unavailable(itemId)
                    // Nothing to open: show what this device has instead.
                    loadInternal(
                        refill = false,
                        isDeckRefresh = false,
                        onDeckReplaced = onDeckReplaced,
                        offlineOnly = true
                    )
                }
            }
        }
    }

    /** Resets the open state once the screen reacted to it. */
    fun clearItemOpen() {
        _itemOpenState.value = ItemOpenState.Idle
    }

    /**
     * [refill] = true re-requests the deck with seen cards included (server
     * "refill mode": saved first, then previously read). Used by the empty
     * state's button when the 30-day no-repeat rule has nothing left to serve.
     */
    fun load(refill: Boolean = false, onDeckReplaced: () -> Unit = {}) {
        loadInternal(
            refill = refill,
            isDeckRefresh = false,
            onDeckReplaced = onDeckReplaced
        )
    }

    /** Re-request the deck with seen cards eligible, then start the session from its first card. */
    fun refreshDeck(
        onDeckReplaced: () -> Unit = {},
        offlineOnly: Boolean = false
    ) {
        loadInternal(
            refill = true,
            isDeckRefresh = true,
            onDeckReplaced = onDeckReplaced,
            offlineOnly = offlineOnly
        )
    }

    private fun loadInternal(
        refill: Boolean,
        isDeckRefresh: Boolean,
        onDeckReplaced: () -> Unit,
        offlineOnly: Boolean = false
    ) {
        loadJob?.cancel()
        val attemptId = if (isDeckRefresh) ++refreshAttemptId else 0L
        _deckRefreshState.value = if (isDeckRefresh) {
            DeckRefreshState.Refreshing
        } else {
            DeckRefreshState.Idle
        }
        loadJob = viewModelScope.launch {
            val cardLimit = settings.cardLimit
            val minuteLimit = settings.minuteLimit
            val startedAt = SystemClock.elapsedRealtime()

            if (offlineOnly) {
                // An explicit offline refresh should use Room immediately instead
                // of waiting for the network connect timeout before falling back.
                val cachedCards = feedItemsToCards(offline.feedDtos())
                val currentCards = (_state.value as? FeedUiState.Content)?.cards.orEmpty()
                val restartCards = cachedCards.ifEmpty { currentCards }
                if (restartCards.isEmpty()) {
                    val message = "Couldn't connect, and there isn't a saved deck to restart yet."
                    if (_state.value !is FeedUiState.Content) {
                        _state.value = FeedUiState.Error(message)
                    }
                    if (isDeckRefresh) {
                        _deckRefreshState.value = DeckRefreshState.Failed(message, attemptId)
                    }
                    return@launch
                }

                sessionJob?.cancel()
                openedSessionId = null
                sessionClosed = false
                onDeckReplaced()
                val newDeckGeneration = ++deckGeneration
                _state.value = FeedUiState.Content(
                    cards = restartCards,
                    cardLimit = cardLimit,
                    minuteLimit = minuteLimit,
                    startedAtElapsedMs = startedAt,
                    offline = true,
                    deckGeneration = newDeckGeneration
                )
                if (isDeckRefresh) {
                    _deckRefreshState.value = DeckRefreshState.RestartedOffline(
                        minOf(restartCards.size, cardLimit),
                        attemptId
                    )
                }
                return@launch
            }

            val token = tokens.getToken()
            if (token == null) {
                val message = "Not logged in — log in again."
                _state.value = FeedUiState.Error(message)
                if (isDeckRefresh) _deckRefreshState.value = DeckRefreshState.Failed(message, attemptId)
                return@launch
            }
            // 1. Fast cache-first (stale-while-revalidate): If we have cached cards in Room
            // and we're not forcing a refill, immediately show the cached cards so the UI renders
            // instantaneously (<10ms) without blocking on network.
            if (!refill && _state.value !is FeedUiState.Content) {
                val cached = offline.feedDtos()
                val cachedCards = feedItemsToCards(cached)
                if (cachedCards.isNotEmpty()) {
                    _state.value = FeedUiState.Content(
                        cards = cachedCards,
                        cardLimit = cardLimit,
                        minuteLimit = minuteLimit,
                        startedAtElapsedMs = startedAt,
                        offline = true
                    )
                } else {
                    _state.value = FeedUiState.Loading
                }
            } else if (refill && _state.value !is FeedUiState.Content) {
                _state.value = FeedUiState.Loading
            }

            // 2. Open the server session concurrently. Feed fetching never waits for this.
            openedSessionId = null
            sessionClosed = false
            sessionJob = launch {
                openedSessionId = openSession(token, cardLimit, minuteLimit)
            }

            // 3. Fetch the fresh ranked deck from the API.
            try {
                val page = api.feed(
                    "Bearer $token",
                    limit = cardLimit,
                    sessionId = openedSessionId,
                    excludeSeen = !refill
                )
                val cards = feedItemsToCards(page.data)
                onDeckReplaced()
                val newDeckGeneration = ++deckGeneration

                // 4. Update the UI state immediately.
                _state.value = if (cards.isEmpty()) {
                    FeedUiState.Empty
                } else {
                    FeedUiState.Content(
                        cards = cards,
                        cardLimit = cardLimit,
                        minuteLimit = minuteLimit,
                        startedAtElapsedMs = startedAt,
                        offline = false,
                        deckGeneration = newDeckGeneration
                    )
                }
                if (isDeckRefresh) {
                    _deckRefreshState.value = if (cards.isEmpty()) {
                        DeckRefreshState.NoCards(attemptId)
                    } else {
                        DeckRefreshState.Refreshed(cards.size, attemptId)
                    }
                }

                // 5. Asynchronously refresh the Room offline cache without delaying the UI.
                launch {
                    offline.saveFeed(page.data)
                }
            } catch (e: HttpException) {
                val message = if (e.code() == 401) {
                    "Session expired — log out from Me and log back in."
                } else {
                    "Server error (${e.code()})."
                }
                if (_state.value !is FeedUiState.Content) {
                    _state.value = FeedUiState.Error(message)
                }
                if (isDeckRefresh) _deckRefreshState.value = DeckRefreshState.Failed(message, attemptId)
            } catch (_: IOException) {
                if (isDeckRefresh) {
                    // An offline refresh can still restart the last local deck.
                    // Prefer Room's snapshot, then the deck already visible in memory.
                    val cachedCards = feedItemsToCards(offline.feedDtos())
                    val currentCards = (_state.value as? FeedUiState.Content)?.cards.orEmpty()
                    val restartCards = cachedCards.ifEmpty { currentCards }
                    if (restartCards.isNotEmpty()) {
                        val newDeckGeneration = ++deckGeneration
                        onDeckReplaced()
                        _state.value = FeedUiState.Content(
                            cards = restartCards,
                            cardLimit = cardLimit,
                            minuteLimit = minuteLimit,
                            startedAtElapsedMs = startedAt,
                            offline = true,
                            deckGeneration = newDeckGeneration
                        )
                        _deckRefreshState.value = DeckRefreshState.RestartedOffline(
                            minOf(restartCards.size, cardLimit),
                            attemptId
                        )
                    } else {
                        val message = "Couldn't connect, and there isn't a saved deck to restart yet."
                        if (_state.value !is FeedUiState.Content) {
                            _state.value = FeedUiState.Error(message)
                        }
                        _deckRefreshState.value = DeckRefreshState.Failed(message, attemptId)
                    }
                } else if (_state.value !is FeedUiState.Content) {
                    // If the server is unreachable before any content appears, fall back to Room.
                    val cached = offline.feedDtos()
                    val cards = feedItemsToCards(cached)
                    _state.value = if (cards.isEmpty()) {
                        FeedUiState.Error(
                            "Couldn't connect to SPARK. Check your internet connection and try again."
                        )
                    } else {
                        FeedUiState.Content(cards, cardLimit, minuteLimit, startedAt, offline = true)
                    }
                }
            }
        }
    }

    /** Close the server session once (best-effort — the phone may be offline). */
    fun closeSession(reason: FinishReason, itemsSeen: Int) {
        if (sessionClosed) return
        sessionClosed = true
        viewModelScope.launch {
            val token = tokens.getToken() ?: return@launch
            sessionJob?.join()
            val sessionId = openedSessionId ?: return@launch
            try {
                api.closeSession(
                    "Bearer $token",
                    sessionId,
                    SessionCloseRequest(itemsSeen, limitReached = reason != FinishReason.STACK_DONE)
                )
            } catch (_: HttpException) {
                // Best-effort: the session row just stays open server-side.
            } catch (_: IOException) {
            }
        }
    }

    private suspend fun openSession(token: String, cardLimit: Int, minuteLimit: Int): String? =
        try {
            api.openSession(
                "Bearer $token",
                SessionRequest(settings.deviceId, cardLimit, minuteLimit)
            ).data.sessionId
        } catch (_: HttpException) {
            null // feed still works without a server session
        } catch (_: IOException) {
            null
        }
}
