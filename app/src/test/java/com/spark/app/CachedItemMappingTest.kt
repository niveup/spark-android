package com.spark.app

import com.spark.app.data.local.CachedItemEntity
import com.spark.app.data.local.toCachedEntity
import com.spark.app.data.local.toFeedItemDto
import com.spark.app.data.remote.BookRef
import com.spark.app.data.remote.FeedItemDto
import com.spark.app.data.remote.QuizDto
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Round-trip guarantee for the offline cache (Phase 7): every field the UI
 * card needs survives FeedItemDto -> CachedItemEntity -> FeedItemDto.
 */
class CachedItemMappingTest {

    @Test
    fun `fully populated dto survives the round trip`() {
        val dto = FeedItemDto(
            id = "feed-42",
            type = "audio",
            title = "How to Live on Twenty-Four Hours a Day — 01",
            bodyMd = "Arnold Bennett opens the daily miracle chapter…",
            actionLine = "Try this today: plan 90 free minutes.",
            topicTags = listOf("habits", "focus"),
            language = "en",
            estSeconds = 120,
            progressMs = 31_000L,
            quiz = QuizDto(options = listOf("A", "B"), correctIndex = 1, explanation = "Because."),
            audioUrl = "https://archive.org/stream/x/01.mp3",
            durationMs = 446_000L,
            book = BookRef(id = "book-7", title = "How to Live", author = "Arnold Bennett"),
            sourceType = "librivox",
            sourceRef = "librivox://how-to-live/01",
            license = "public-domain",
            isAiGenerated = false,
            isGrounded = true,
            saved = true,
            liked = false,
            dismissed = false,
            publishedAt = "2026-10-17T10:00:00.000Z"
        )

        val entity = dto.toCachedEntity(forBatchMs = 1_000L, deckIndex = 3)
        val back = entity.toFeedItemDto()

        assertEquals(dto, back)
    }

    @Test
    fun `minimal dto survives the round trip`() {
        val dto = FeedItemDto(id = "q-1", type = "quote", title = "On anger", bodyMd = "…")

        val back = dto.toCachedEntity(forBatchMs = 2_000L, deckIndex = 0).toFeedItemDto()

        assertEquals(dto, back)
    }

    @Test
    fun `quiz with no options survives`() {
        val dto = FeedItemDto(
            id = "quiz-9",
            type = "quiz",
            title = "Habit loop",
            bodyMd = "cue, routine, reward",
            quiz = QuizDto(emptyList(), correctIndex = 0, explanation = "")
        )
        val entity: CachedItemEntity = dto.toCachedEntity(forBatchMs = 3_000L, deckIndex = 1)

        assertEquals(dto, entity.toFeedItemDto())
    }
}
