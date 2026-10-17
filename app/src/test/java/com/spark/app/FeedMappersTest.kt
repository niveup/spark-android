package com.spark.app

import com.spark.app.data.CardType
import com.spark.app.data.remote.BookRef
import com.spark.app.data.remote.FeedItemDto
import com.spark.app.data.remote.QuizDto
import com.spark.app.feed.durationLabelOf
import com.spark.app.feed.feedItemToCard
import com.spark.app.feed.feedItemsToCards
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedMappersTest {

    private fun item(
        id: String = "i1",
        type: String = "quote",
        title: String = "Title",
        bodyMd: String = "Body",
        topicTags: List<String> = listOf("Stoicism"),
        sourceType: String = "wikiquote",
        sourceRef: String = "https://en.wikiquote.org/wiki/Marcus_Aurelius",
        license: String = "CC BY-SA",
        isAiGenerated: Boolean = true,
        isGrounded: Boolean = false,
        quiz: QuizDto? = null,
        book: BookRef? = null,
        durationMs: Long? = null
    ) = FeedItemDto(
        id = id,
        type = type,
        title = title,
        bodyMd = bodyMd,
        topicTags = topicTags,
        sourceType = sourceType,
        sourceRef = sourceRef,
        license = license,
        isAiGenerated = isAiGenerated,
        isGrounded = isGrounded,
        quiz = quiz,
        book = book,
        durationMs = durationMs
    )

    @Test
    fun `all contract types map to card types`() {
        val pairs = mapOf(
            "book_idea" to CardType.BOOK_IDEA,
            "summary" to CardType.SUMMARY,
            "quote" to CardType.QUOTE,
            "concept" to CardType.CONCEPT,
            "quiz" to CardType.QUIZ,
            "audio" to CardType.AUDIO,
            "challenge" to CardType.CHALLENGE
        )
        pairs.forEach { (wire, expected) ->
            assertEquals(expected, feedItemToCard(item(type = wire))?.type)
        }
    }

    @Test
    fun `unknown type is skipped, not shown`() {
        assertNull(feedItemToCard(item(type = "podcast")))
        val cards = feedItemsToCards(listOf(item(id = "a"), item(id = "b", type = "podcast")))
        assertEquals(listOf("a"), cards.map { it.id })
    }

    @Test
    fun `duration label formats mm ss and rejects null or zero`() {
        assertEquals("8:31", durationLabelOf(511_000))
        assertEquals("0:45", durationLabelOf(45_000))
        assertEquals("12:04", durationLabelOf(724_000))
        assertNull(durationLabelOf(null))
        assertNull(durationLabelOf(0))
    }

    @Test
    fun `grounded and ai flags pass through`() {
        val grounded = feedItemToCard(item(isGrounded = true, isAiGenerated = false))
        assertTrue(grounded!!.grounded)
        assertFalse(grounded.aiGenerated)

        val ai = feedItemToCard(item(isGrounded = false, isAiGenerated = true))
        assertFalse(ai!!.grounded)
        assertTrue(ai.aiGenerated)
    }

    @Test
    fun `topic falls back to General when tags empty`() {
        val card = feedItemToCard(item(topicTags = emptyList()))
        assertEquals("General", card?.topic)
    }

    @Test
    fun `source joins ref type license and falls back`() {
        val card = feedItemToCard(item())
        assertTrue(card!!.source.startsWith("https://en.wikiquote.org"))
        assertTrue(card.source.contains("wikiquote"))
        assertTrue(card.source.contains("CC BY-SA"))

        val blank = feedItemToCard(item(sourceType = "", sourceRef = "", license = ""))
        assertEquals("SPARK library", blank?.source)
    }

    @Test
    fun `book line builds with and without author`() {
        val withAuthor = feedItemToCard(
            item(book = BookRef(id = "b1", title = "Meditations", author = "Marcus Aurelius"))
        )
        assertEquals("Meditations — Marcus Aurelius", withAuthor?.bookLine)

        val noAuthor = feedItemToCard(
            item(book = BookRef(id = "b2", title = "Meditations", author = ""))
        )
        assertEquals("Meditations", noAuthor?.bookLine)
        assertNull(feedItemToCard(item())?.bookLine)
    }

    @Test
    fun `quiz payload maps only when present`() {
        val q = QuizDto(options = listOf("A", "B"), correctIndex = 1, explanation = "Because B.")
        val card = feedItemToCard(item(type = "quiz", quiz = q))
        assertEquals(listOf("A", "B"), card?.quiz?.options)
        assertEquals(1, card?.quiz?.correctIndex)
        assertNull(feedItemToCard(item(type = "quote"))?.quiz)
    }
}
