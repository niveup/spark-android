package com.spark.app.data.local

import com.spark.app.data.remote.BookRef
import com.spark.app.data.remote.FeedItemDto
import com.spark.app.data.remote.QuizDto

/**
 * Pure mapping between the API shape (FeedItemDto) and the Room row
 * (CachedItemEntity). No Android imports so it is unit-tested directly.
 *
 * Every FeedItemDto field needed to rebuild the UI card is stored; extra
 * server fields (progressMs, language, dismissed, publishedAt …) round-trip
 * so an offline deck is byte-equivalent to the online one.
 */

/** Field separator for list columns. Tags are server-normalized slugs. */
private const val SEP = "\u0001"

fun FeedItemDto.toCachedEntity(forBatchMs: Long, deckIndex: Int): CachedItemEntity =
    CachedItemEntity(
        id = id,
        type = type,
        title = title,
        bodyMd = bodyMd,
        actionLine = actionLine,
        topicTags = topicTags.joinToString(SEP),
        language = language,
        estSeconds = estSeconds,
        progressMs = progressMs,
        quizOptions = quiz?.options?.joinToString(SEP),
        quizCorrectIndex = quiz?.correctIndex,
        quizExplanation = quiz?.explanation,
        audioUrl = audioUrl,
        durationMs = durationMs,
        bookId = book?.id,
        bookTitle = book?.title,
        bookAuthor = book?.author,
        bookCoverUrl = book?.coverUrl,
        sourceType = sourceType,
        sourceRef = sourceRef,
        license = license,
        isAiGenerated = isAiGenerated,
        isGrounded = isGrounded,
        saved = saved,
        liked = liked,
        dismissed = dismissed,
        publishedAt = publishedAt,
        cachedAtMs = forBatchMs,
        deckIndex = deckIndex
    )

fun CachedItemEntity.toFeedItemDto(): FeedItemDto = FeedItemDto(
    id = id,
    type = type,
    title = title,
    bodyMd = bodyMd,
    actionLine = actionLine,
    topicTags = topicTags.splitToListOfNonBlank(SEP),
    language = language,
    estSeconds = estSeconds,
    progressMs = progressMs,
    quiz = quizOptions?.let { opts ->
        QuizDto(
            options = opts.splitToListOfNonBlank(SEP),
            correctIndex = quizCorrectIndex ?: 0,
            explanation = quizExplanation ?: ""
        )
    },
    audioUrl = audioUrl,
    durationMs = durationMs,
    book = bookId?.let { bid ->
        BookRef(
            id = bid,
            title = bookTitle ?: "",
            author = bookAuthor ?: "",
            coverUrl = bookCoverUrl
        )
    },
    sourceType = sourceType,
    sourceRef = sourceRef,
    license = license,
    isAiGenerated = isAiGenerated,
    isGrounded = isGrounded,
    saved = saved,
    liked = liked,
    dismissed = dismissed,
    publishedAt = publishedAt
)

private fun String.splitToListOfNonBlank(sep: String): List<String> {
    val cleaned = removeSuffix(sep)
    if (cleaned.isEmpty()) return emptyList()
    return cleaned.split(sep).filter { it.isNotBlank() }
}

/**
 * Offline cap (Phase 7 plan): keep at most [cap] fresh feed cards and ALWAYS
 * keep saved ones (the Saved tab must work fully offline). Rows are compared
 * newest-first by (cachedAtMs, deckIndex) — the reverse of feed order.
 */
fun pruneForCap(all: List<CachedItemEntity>, cap: Int): List<CachedItemEntity> {
    if (all.size <= cap) return all
    val saved = all.filter { it.saved }
    val savedIds = saved.map { it.id }.toSet()
    val fresh = all.filter { !it.saved }
        .sortedWith(compareByDescending<CachedItemEntity> { it.cachedAtMs }.thenBy { it.deckIndex })
    return saved + fresh.take(cap)
}
