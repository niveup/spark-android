package com.spark.app.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spark.app.data.SESSION_CARD_LIMIT

/** In-memory session state. Persistence arrives with Room in Phase 7. */
class FeedState {
    var maxReached: Int by mutableStateOf(-1)
        private set
    var done: Boolean by mutableStateOf(false)
        private set

    private val _saved = mutableStateListOf<String>()
    val saved: List<String> get() = _saved
    private val _liked = mutableStateListOf<String>()
    val liked: List<String> get() = _liked
    private val _disliked = mutableStateListOf<String>()
    val disliked: List<String> get() = _disliked

    val seenCount: Int get() = maxReached + 1

    fun onPageSettled(page: Int) {
        if (page > maxReached) maxReached = page
    }

    fun isLastPage(totalPages: Int): Boolean = maxReached >= totalPages - 1

    fun finish() {
        done = true
    }

    fun toggleSave(id: String) {
        if (_saved.contains(id)) _saved.remove(id) else _saved.add(id)
    }

    fun toggleLike(id: String) {
        if (_liked.contains(id)) {
            _liked.remove(id)
        } else {
            _liked.add(id)
            _disliked.remove(id)
        }
    }

    fun toggleDislike(id: String) {
        if (_disliked.contains(id)) {
            _disliked.remove(id)
        } else {
            _disliked.add(id)
            _liked.remove(id)
        }
    }

    companion object {
        fun limit(): Int = SESSION_CARD_LIMIT
    }
}
