package com.spark.app.data

/** Card types mirroring the server's FeedItemType (docs/API_CONTRACT.md). */
enum class CardType {
    BOOK_IDEA,
    SUMMARY,
    QUOTE,
    CONCEPT,
    QUIZ,
    AUDIO,
    CHALLENGE
}

data class QuizData(
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

/**
 * UI model for one feed card. Built from the server's FeedItemDto by
 * com.spark.app.feed.feedItemToCard — ids are server-opaque strings.
 */
data class Card(
    val id: String,
    val type: CardType,
    val topic: String,
    val title: String,
    val body: String,
    val actionLine: String,
    val source: String,
    /** True = grounded in a real source text. False = AI-written, unverified. */
    val grounded: Boolean,
    /** True when the server flagged this card as AI-generated (never shown as a quote). */
    val aiGenerated: Boolean = false,
    /** "Title — Author" when the card belongs to a book, else null. */
    val bookLine: String? = null,
    val quiz: QuizData? = null,
    val durationLabel: String? = null,
    /** Server-side interaction state at load time (Phase 5b). */
    val saved: Boolean = false,
    val liked: Boolean? = null
)

const val SESSION_CARD_LIMIT = 20
