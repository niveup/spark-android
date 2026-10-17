package com.spark.app.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spark.app.data.Card
import com.spark.app.data.SESSION_CARD_LIMIT
import com.spark.app.data.TokenStore
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
    data class Content(val cards: List<Card>) : FeedUiState
}

/** Loads the ranked feed from the DEV API (Phase 5a: read side only). */
class FeedViewModel(
    private val api: SparkApi,
    private val tokens: TokenStore
) : ViewModel() {

    private val _state = MutableStateFlow<FeedUiState>(FeedUiState.Loading)
    val state: StateFlow<FeedUiState> = _state.asStateFlow()

    fun load() {
        _state.value = FeedUiState.Loading
        viewModelScope.launch {
            val token = tokens.getToken()
            if (token == null) {
                _state.value = FeedUiState.Error("Not logged in — log in again.")
                return@launch
            }
            try {
                val page = api.feed("Bearer $token", limit = SESSION_CARD_LIMIT)
                val cards = feedItemsToCards(page.data)
                _state.value =
                    if (cards.isEmpty()) FeedUiState.Empty else FeedUiState.Content(cards)
            } catch (e: HttpException) {
                _state.value = FeedUiState.Error(
                    if (e.code() == 401) "Session expired — log out from Me and log back in."
                    else "Server error (${e.code()})."
                )
            } catch (_: IOException) {
                _state.value = FeedUiState.Error(
                    "Can't reach your PC. Check the USB cable and that the API server is running."
                )
            }
        }
    }
}
