package com.spark.app.feed

import com.spark.app.data.Card
import com.spark.app.data.CardType
import com.spark.app.data.QuizData
import com.spark.app.data.remote.FeedItemDto

// Pure mapping from the API shape to the UI shape. Unknown card types are
// skipped, never shown (docs/API_CONTRACT.md compatibility promise).

fun cardTypeOf(raw: String): CardType? = when (raw) {
    "book_idea" -> CardType.BOOK_IDEA
    "summary" -> CardType.SUMMARY
    "quote" -> CardType.QUOTE
    "concept" -> CardType.CONCEPT
    "quiz" -> CardType.QUIZ
    "audio" -> CardType.AUDIO
    "challenge" -> CardType.CHALLENGE
    else -> null
}

/** "8:31" for audio cards; null when the server sent no duration. */
fun durationLabelOf(durationMs: Long?): String? {
    if (durationMs == null || durationMs <= 0) return null
    val totalSec = durationMs / 1000
    val min = totalSec / 60
    val sec = (totalSec % 60).toString().padStart(2, '0')
    return "$min:$sec"
}

fun feedItemToCard(item: FeedItemDto): Card? {
    val type = cardTypeOf(item.type) ?: return null
    return Card(
        id = item.id,
        type = type,
        topic = item.topicTags.firstOrNull { it.isNotBlank() } ?: "General",
        title = item.title,
        body = item.bodyMd,
        actionLine = item.actionLine,
        source = listOf(item.sourceRef, item.sourceType, item.license)
            .filter { it.isNotBlank() }
            .joinToString(" · ")
            .ifBlank { "SPARK library" },
        grounded = item.isGrounded,
        aiGenerated = item.isAiGenerated,
        bookLine = item.book?.let { b ->
            if (b.author.isBlank()) b.title else "${b.title} — ${b.author}"
        },
        quiz = item.quiz?.let { q ->
            QuizData(options = q.options, correctIndex = q.correctIndex, explanation = q.explanation)
        },
        durationLabel = durationLabelOf(item.durationMs)
    )
}

fun feedItemsToCards(items: List<FeedItemDto>): List<Card> = items.mapNotNull(::feedItemToCard)
