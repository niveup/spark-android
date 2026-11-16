package com.spark.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spark.app.data.Card
import com.spark.app.data.local.SavedCardsCache
import com.spark.app.data.remote.SparkApi
import com.spark.app.feed.feedItemsToCards
import com.spark.app.sync.TokenReader
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
    private val tokens: TokenReader,
    private val offline: SavedCardsCache
) : ViewModel() {

    private val _state = MutableStateFlow<SavedUiState>(SavedUiState.Loading)
    val state: StateFlow<SavedUiState> = _state.asStateFlow()

    private var latestQuery = ""
    private var loadJob: Job? = null

    init {
        // Pre-populate with cached saved cards on startup so opening the
        // Saved tab is instant with zero lag, even when offline.
        viewModelScope.launch {
            val cached = feedItemsToCards(offline.savedDtos())
            if (cached.isNotEmpty() && _state.value is SavedUiState.Loading) {
                _state.value = SavedUiState.Content(cached, query = "", offline = true)
            }
        }
    }

    fun load(query: String) {
        latestQuery = query
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val token = tokens.getToken()
            if (token == null) {
                _state.value = SavedUiState.Error("Not logged in — log in again.")
                return@launch
            }

            // Immediately load and show cached cards if available, avoiding offline / network delay
            val cachedCards = feedItemsToCards(offline.savedDtos())
            val filteredCached = if (query.isBlank()) cachedCards else cachedCards.filterByQuery(query)
            if (cachedCards.isNotEmpty()) {
                if (query == latestQuery) {
                    _state.value = SavedUiState.Content(filteredCached, query, offline = true)
                }
            } else if (_state.value !is SavedUiState.Content) {
                _state.value = SavedUiState.Loading
            }

            try {
                val page = api.savedItems("Bearer $token", limit = 50, q = query.ifBlank { null })
                offline.saveFeed(page.data) // cached rows keep `saved = true` for offline reads
                if (query == latestQuery) {
                    _state.value = SavedUiState.Content(feedItemsToCards(page.data), query, offline = false)
                }
            } catch (e: HttpException) {
                if (query == latestQuery) {
                    _state.value = SavedUiState.Error(
                        if (e.code() == 401) "Session expired — log out from Me and log back in."
                        else "Server error (${e.code()})."
                    )
                }
            } catch (_: IOException) {
                // Offline — keep the cached cards if available; only show error if cache is empty.
                if (query == latestQuery) {
                    if (cachedCards.isEmpty()) {
                        _state.value = SavedUiState.Error(
                            "Can't reach your PC. Check the USB cable and that the API server is running."
                        )
                    } else {
                        _state.value = SavedUiState.Content(filteredCached, query, offline = true)
                    }
                }
            }
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
