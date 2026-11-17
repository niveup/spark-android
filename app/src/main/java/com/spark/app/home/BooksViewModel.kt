package com.spark.app.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spark.app.BuildConfig
import com.spark.app.data.remote.BookDto
import com.spark.app.data.remote.SparkApi
import com.spark.app.sync.TokenReader
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

sealed interface BooksUiState {
    data object Loading : BooksUiState
    data class Error(val message: String) : BooksUiState
    data class Content(val books: List<BookDto>) : BooksUiState
}

/**
 * Storytel-inspired Books library viewmodel.
 * Loads curated classics and audiobooks from /api/mobile/v1/books,
 * resolves cover URLs to the local or cloud backend proxy.
 */
class BooksViewModel(
    private val api: SparkApi,
    private val tokens: TokenReader
) : ViewModel() {

    private val _state = MutableStateFlow<BooksUiState>(BooksUiState.Loading)
    val state: StateFlow<BooksUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    fun load(force: Boolean = false) {
        if (_state.value is BooksUiState.Content && !force) {
            return
        }
        if (loadJob?.isActive == true) {
            if (!force) return
            loadJob?.cancel()
        }
        // Only show full-screen loading spinner on the very first load
        if (_state.value !is BooksUiState.Content) {
            _state.value = BooksUiState.Loading
        }
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            val token = tokens.getToken()
            if (token == null) {
                if (_state.value !is BooksUiState.Content) {
                    _state.value = BooksUiState.Error("Not logged in — log in again.")
                }
                return@launch
            }
            try {
                val res = api.books("Bearer $token")
                val resolvedBooks = res.data.map { book ->
                    val resolvedCover = book.coverUrl?.let { url ->
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            url
                        } else {
                            "${BuildConfig.API_BASE_URL.removeSuffix("/")}/${url.removePrefix("/")}"
                        }
                    }
                    val resolvedChapters = book.chapters.map { ch ->
                        val resolvedAudio = ch.audioUrl?.let { url ->
                            if (url.startsWith("http://") || url.startsWith("https://")) {
                                url
                            } else {
                                "${BuildConfig.API_BASE_URL.removeSuffix("/")}/${url.removePrefix("/")}"
                            }
                        }
                        ch.copy(audioUrl = resolvedAudio)
                    }
                    book.copy(coverUrl = resolvedCover, chapters = resolvedChapters)
                }
                _state.value = BooksUiState.Content(resolvedBooks)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                if (_state.value !is BooksUiState.Content) {
                    _state.value = BooksUiState.Error("Could not connect to library. Tap to retry.")
                }
            } catch (e: HttpException) {
                if (_state.value !is BooksUiState.Content) {
                    _state.value = BooksUiState.Error("Library server error (${e.code()}).")
                }
            } catch (e: Exception) {
                if (_state.value !is BooksUiState.Content) {
                    _state.value = BooksUiState.Error("Failed to load books. Tap to retry.")
                }
            }
        }
    }
}
