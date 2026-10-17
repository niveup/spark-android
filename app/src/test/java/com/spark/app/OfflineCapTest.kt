package com.spark.app

import com.spark.app.data.local.CachedItemEntity
import com.spark.app.data.local.pruneForCap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Offline cap policy (Phase 7): keep at most `cap` fresh cards, always keep
 * saved ones, evict the oldest fresh cards first.
 */
class OfflineCapTest {

    private fun row(id: String, saved: Boolean, batchMs: Long, deckIndex: Int = 0) =
        CachedItemEntity(
            id = id, type = "quote", title = id, bodyMd = "", actionLine = "",
            topicTags = "", language = "en", estSeconds = 30, progressMs = null,
            quizOptions = null, quizCorrectIndex = null, quizExplanation = null,
            audioUrl = null, durationMs = null, bookId = null, bookTitle = null,
            bookAuthor = null, bookCoverUrl = null, sourceType = "", sourceRef = "",
            license = "", isAiGenerated = false, isGrounded = true,
            saved = saved, liked = null, dismissed = false, publishedAt = "",
            cachedAtMs = batchMs, deckIndex = deckIndex
        )

    @Test
    fun `nothing is evicted below the cap`() {
        val all = List(10) { i -> row("n$i", saved = false, batchMs = 1_000L + i) }

        assertEquals(all, pruneForCap(all, cap = 50))
    }

    @Test
    fun `fresh cards evict oldest first`() {
        val all = List(60) { i -> row("n$i", saved = false, batchMs = 1_000L + i) }

        val kept = pruneForCap(all, cap = 50)

        assertEquals(50, kept.size)
        assertTrue("oldest batch evicted", kept.none { it.id == "n0" })
        assertTrue("newest batch kept", kept.any { it.id == "n59" })
    }

    @Test
    fun `saved cards are never evicted even beyond the cap`() {
        val saved = List(25) { i -> row("s$i", saved = true, batchMs = 100L + i) }
        val fresh = List(60) { i -> row("n$i", saved = false, batchMs = 1_000L + i) }

        val kept = pruneForCap(saved + fresh, cap = 50)

        assertEquals(25, kept.count { it.saved })
        assertEquals(saved.toSet(), kept.filter { it.saved }.toSet())
        assertEquals(50, kept.count { !it.saved }) // oldest fresh evicted, cap reached
    }

    @Test
    fun `within one batch the deck order survives the cap`() {
        // One batch, 55 cards: deckIndex 0 is the top-ranked card.
        val all = List(55) { i -> row("n$i", saved = false, batchMs = 5_000L, deckIndex = i) }

        val kept = pruneForCap(all, cap = 50)

        assertTrue("top-ranked card survives", kept.any { it.id == "n0" })
        assertTrue("tail evicted", kept.none { it.id == "n54" })
        assertEquals(50, kept.size)
    }
}
