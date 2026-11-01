package com.spark.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spark.app.data.TokenStore
import com.spark.app.data.remote.ApiErrors
import com.spark.app.data.remote.SparkApi
import com.spark.app.data.remote.StatsData
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
    private val tokens: TokenStore
) : ViewModel() {

    private val _state = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    fun refresh() {
        _state.value = StatsUiState.Loading
        viewModelScope.launch {
            val token = tokens.getToken()
            if (token == null) {
                _state.value = StatsUiState.Error(ApiErrors.NOT_LOGGED_IN)
                return@launch
            }
            _state.value = try {
                StatsUiState.Content(api.stats("Bearer $token").data)
            } catch (e: HttpException) {
                StatsUiState.Error(ApiErrors.forHttp(e.code()))
            } catch (_: IOException) {
                StatsUiState.Error(ApiErrors.OFFLINE)
            }
        }
    }
}
