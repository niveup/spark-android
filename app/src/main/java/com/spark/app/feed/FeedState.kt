package com.spark.app.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spark.app.data.Card

/** Why the session ended — shown on the done screen + sent to PATCH /sessions. */
enum class FinishReason { STACK_DONE, CARD_LIMIT, TIME_LIMIT }

/** In-memory session state. Persistence arrives with Room in Phase 7. */
class FeedState {
    var maxReached: Int by mutableStateOf(-1)
        private set
    var finishReason: FinishReason? by mutableStateOf(null)
        private set

    val done: Boolean get() = finishReason != null

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

    fun finish(reason: FinishReason) {
        if (finishReason == null) finishReason = reason
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

    /**
     * Seeds card state from a fresh feed load so cards already saved/rated on the
     * server render correctly (the server is the source of truth). Only adds —
     * an in-session toggle is never overwritten.
     */
    fun seedFrom(cards: List<Card>) {
        for (card in cards) {
            if (card.saved && !_saved.contains(card.id)) _saved.add(card.id)
            if (card.liked == true && !_liked.contains(card.id)) _liked.add(card.id)
            if (card.liked == false && !_disliked.contains(card.id)) _disliked.add(card.id)
        }
    }
}
