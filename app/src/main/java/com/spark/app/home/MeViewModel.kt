package com.spark.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spark.app.data.remote.SparkApi
import com.spark.app.data.remote.StatsData
import com.spark.app.sync.TokenReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface StatsUiState {
    data object Loading : StatsUiState
    data class Error(val message: String) : StatsUiState
    data class Content(val stats: StatsData) : StatsUiState
}

/** Me tab: personal totals from GET /stats (additive v1.1 endpoint). */
class MeViewModel(
    private val api: SparkApi,
    private val tokens: TokenReader
) : ViewModel() {

    private val _state = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val state: StateFlow<StatsUiState> = _state.asStateFlow()
    private var refreshJob: Job? = null

    fun refresh(force: Boolean = false) {
        if (refreshJob?.isActive == true) {
            if (!force) return
            refreshJob?.cancel()
        }
        // Only set Loading state on the initial load or explicit retry to prevent tab switch jank
        if (force || _state.value !is StatsUiState.Content) {
            _state.value = StatsUiState.Loading
        }
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            val token = tokens.getToken()
            if (token == null) {
                _state.value = StatsUiState.Error("Not logged in — log in again.")
                return@launch
            }
            try {
                val stats = api.stats("Bearer $token").data
                _state.value = StatsUiState.Content(stats)
            } catch (e: HttpException) {
                if (_state.value !is StatsUiState.Content) {
                    _state.value = StatsUiState.Error(
                        if (e.code() == 401) "Session expired — log out and log back in."
                        else "Server error (${e.code()})."
                    )
                }
            } catch (_: IOException) {
                if (_state.value !is StatsUiState.Content) {
                    _state.value = StatsUiState.Error(
                        "Couldn't load your reading history. Check your internet connection and try again."
                    )
                }
            }
        }
    }
}
