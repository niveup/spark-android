package com.spark.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spark.app.data.Card
import com.spark.app.data.TokenStore
import com.spark.app.data.local.OfflineCache
import com.spark.app.data.remote.SparkApi
import com.spark.app.feed.feedItemsToCards
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
    private val offline: OfflineCache
) : ViewModel() {

    private val _state = MutableStateFlow<SavedUiState>(SavedUiState.Loading)
    val state: StateFlow<SavedUiState> = _state.asStateFlow()

    private var latestQuery = ""

    fun load(query: String) {
        latestQuery = query
        viewModelScope.launch {
            val token = tokens.getToken()
            if (token == null) {
                _state.value = SavedUiState.Error("Not logged in — log in again.")
                return@launch
            }
            val result = try {
                val page = api.savedItems("Bearer $token", limit = 50, q = query.ifBlank { null })
                offline.saveFeed(page.data) // cached rows keep `saved = true` for offline reads
                SavedUiState.Content(feedItemsToCards(page.data), query)
            } catch (e: HttpException) {
                SavedUiState.Error(
                    if (e.code() == 401) "Session expired — log out from Me and log back in."
                    else "Server error (${e.code()})."
                )
            } catch (_: IOException) {
                // Phase 7: offline — show the cached saved cards, filtered here
                // (the local search mirrors the server's substring match).
                val cards = feedItemsToCards(offline.savedDtos())
                if (cards.isEmpty()) {
                    SavedUiState.Error(
                        "Can't reach your PC. Check the USB cable and that the API server is running."
                    )
                } else {
                    val filtered = if (query.isBlank()) cards else cards.filterByQuery(query)
                    SavedUiState.Content(filtered, query, offline = true)
                }
            }
            if (query == latestQuery) _state.value = result // ignore stale responses
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
