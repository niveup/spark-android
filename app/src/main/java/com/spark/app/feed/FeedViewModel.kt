package com.spark.app.feed

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spark.app.data.Card
import com.spark.app.data.SettingsStore
import com.spark.app.data.TokenStore
import com.spark.app.data.local.OfflineCache
import com.spark.app.data.remote.SessionCloseRequest
import com.spark.app.data.remote.SessionRequest
import com.spark.app.data.remote.SparkApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

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
        val offline: Boolean = false
    ) : FeedUiState
}

/**
 * Loads the ranked feed from the API and owns the server-side session
 * (Phase 5b): POST /sessions on load, PATCH /sessions/:id on finish.
 * Session limits come from SettingsStore (Me tab).
 */
class FeedViewModel(
    private val api: SparkApi,
    private val tokens: TokenStore,
    private val settings: SettingsStore,
    private val offline: OfflineCache
) : ViewModel() {

    private val _state = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    private var openedSessionId: String? = null
    private var sessionClosed = false

    /**
     * [refill] = true re-requests the deck with seen cards included (server
     * "refill mode": saved first, then previously read). Used by the empty
     * state's button when the 30-day no-repeat rule has nothing left to serve.
     */
    fun load(refill: Boolean = false) {
        _state.value = FeedUiState.Loading
        viewModelScope.launch {
            val token = tokens.getToken()
            if (token == null) {
                _state.value = FeedUiState.Error("Not logged in — log in again.")
                return@launch
            }
            val cardLimit = settings.cardLimit
            val minuteLimit = settings.minuteLimit
            val startedAt = SystemClock.elapsedRealtime()
            openedSessionId = openSession(token, cardLimit, minuteLimit)
            sessionClosed = false
            try {
                val page = api.feed(
                    "Bearer $token",
                    limit = cardLimit,
                    sessionId = openedSessionId,
                    excludeSeen = !refill
                )
                val cards = feedItemsToCards(page.data)
                offline.saveFeed(page.data) // Phase 7: refresh the offline copy (server-wins)
                _state.value = if (cards.isEmpty()) {
                    FeedUiState.Empty
                } else {
                    FeedUiState.Content(cards, cardLimit, minuteLimit, startedAt)
                }
            } catch (e: HttpException) {
                _state.value = FeedUiState.Error(
                    if (e.code() == 401) "Session expired — log out from Me and log back in."
                    else "Server error (${e.code()})."
                )
            } catch (_: IOException) {
                // Phase 7: fall back to the cached deck when the server is
                // unreachable. Seen/dwell events keep queueing locally and
                // upload on the next successful connection.
                val cached = offline.feedDtos()
                val cards = feedItemsToCards(cached)
                _state.value =
                    if (cards.isEmpty()) {
                        FeedUiState.Error(
                            "Can't reach your PC. Check the USB cable and that the API server is running."
                        )
                    } else {
                        FeedUiState.Content(cards, cardLimit, minuteLimit, startedAt, offline = true)
                    }
            }
        }
    }

    /** Close the server session once (best-effort — the phone may be offline). */
    fun closeSession(reason: FinishReason, itemsSeen: Int) {
        val sessionId = openedSessionId ?: return
        if (sessionClosed) return
        sessionClosed = true
        viewModelScope.launch {
            val token = tokens.getToken() ?: return@launch
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
