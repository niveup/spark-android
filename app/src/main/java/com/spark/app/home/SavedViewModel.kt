package com.spark.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spark.app.data.Card
import com.spark.app.data.TokenStore
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
    data class Content(val cards: List<Card>, val query: String) : SavedUiState
}

/** Saved tab: server list with substring search (GET /items/saved). */
class SavedViewModel(
    private val api: SparkApi,
    private val tokens: TokenStore
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
                SavedUiState.Content(feedItemsToCards(page.data), query)
            } catch (e: HttpException) {
                SavedUiState.Error(
                    if (e.code() == 401) "Session expired — log out from Me and log back in."
                    else "Server error (${e.code()})."
                )
            } catch (_: IOException) {
                SavedUiState.Error(
                    "Can't reach your PC. Check the USB cable and that the API server is running."
                )
            }
            if (query == latestQuery) _state.value = result // ignore stale responses
        }
    }
}
