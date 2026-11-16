package com.spark.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spark.app.data.Card
import com.spark.app.data.TokenStore
import com.spark.app.data.local.ItemCache
import com.spark.app.data.remote.SparkApi
import com.spark.app.feed.feedItemsToCards
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface SavedUiState {
    data object Loading : SavedUiState
    data class Error(val message: String) : SavedUiState
    data class Content(
        val cards: List<Card>,
        val query: String,
        /** True when the list came from the Room cache (server unreachable). */
        val offline: Boolean = false
    ) : SavedUiState
}

/** Saved tab: server list with substring search (GET /items/saved). */
class SavedViewModel(
    private val api: SparkApi,
    private val tokens: TokenStore,
    private val offline: ItemCache
) : ViewModel() {

    private val _state = MutableStateFlow<SavedUiState>(SavedUiState.Loading)
    val state: StateFlow<SavedUiState> = _state.asStateFlow()

    private var latestQuery = ""
    private var loadJob: Job? = null

    fun load(query: String) {
        loadJob?.cancel()
        latestQuery = query
        loadJob = viewModelScope.launch {
            val token = tokens.getToken()
            if (token == null) {
                _state.value = SavedUiState.Error("Not logged in — log in again.")
                return@launch
            }
            val cachedCards = feedItemsToCards(offline.savedDtos())
            if (query == latestQuery && cachedCards.isNotEmpty()) {
                _state.value = SavedUiState.Content(
                    cards = cachedCards.filterByQuery(query),
                    query = query,
                    offline = true
                )
            } else if (query == latestQuery) {
                _state.value = SavedUiState.Loading
            }
            val result = try {
                val page = api.savedItems("Bearer $token", limit = 50, q = query.ifBlank { null })
                offline.saveFeed(page.data) // cached rows keep `saved = true` for offline reads
                SavedUiState.Content(feedItemsToCards(page.data), query)
            } catch (e: HttpException) {
                if (e.code() == 401) {
                    SavedUiState.Error("Session expired — log out from Me and log back in.")
                } else if (cachedCards.isNotEmpty()) {
                    SavedUiState.Content(cachedCards.filterByQuery(query), query, offline = true)
                } else {
                    SavedUiState.Error("Server error (${e.code()}).")
                }
            } catch (_: IOException) {
                // Phase 7: offline — show the cached saved cards, filtered here
                // (the local search mirrors the server's substring match).
                val cards = feedItemsToCards(offline.savedDtos())
                if (cards.isEmpty()) {
                    SavedUiState.Error(
                        "Couldn't load your saved library. Check your internet connection and try again."
                    )
                } else {
                    val filtered = if (query.isBlank()) cards else cards.filterByQuery(query)
                    SavedUiState.Content(filtered, query, offline = true)
                }
            }
            if (query == latestQuery) _state.value = result // ignore stale responses
        }
    }

    /**
     * Show cached results before the screen waits for pending uploads or a
     * server search. Room reads complete independently of network state.
     */
    suspend fun showCached(query: String, force: Boolean = false) {
        if (force || query != latestQuery) loadJob?.cancel()
        latestQuery = query
        val current = _state.value as? SavedUiState.Content
        if (!force && current?.query == query && !current.offline) return
        if (tokens.getToken() == null) {
            _state.value = SavedUiState.Error("Not logged in — log in again.")
            return
        }
        val cards = feedItemsToCards(offline.savedDtos())
        _state.value = if (cards.isEmpty()) {
            SavedUiState.Loading
        } else {
            SavedUiState.Content(cards.filterByQuery(query), query, offline = true)
        }
    }
}

/** Case-insensitive substring match on title and body (server search mirror). */
private fun List<Card>.filterByQuery(query: String): List<Card> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return this
    return filter { card ->
        card.title.lowercase().contains(q) || card.body.lowercase().contains(q)
    }
}
