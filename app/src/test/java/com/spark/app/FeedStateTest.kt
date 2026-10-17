package com.spark.app

import com.spark.app.feed.FeedState
import com.spark.app.feed.FinishReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedStateTest {
    @Test
    fun `starts at 0 seen, not done`() {
        val state = FeedState()
        assertEquals(0, state.seenCount)
        assertFalse(state.done)
        assertNull(state.finishReason)
    }

    @Test
    fun `settling pages tracks the furthest seen`() {
        val state = FeedState()
        state.onPageSettled(0)
        state.onPageSettled(4)
        state.onPageSettled(2) // scrolling back does not lower the count
        assertEquals(5, state.seenCount)
    }

    @Test
    fun `finish records the reason once and ends the session`() {
        val state = FeedState()
        state.finish(FinishReason.CARD_LIMIT)
        state.finish(FinishReason.TIME_LIMIT) // first reason wins
        assertTrue(state.done)
        assertEquals(FinishReason.CARD_LIMIT, state.finishReason)
    }

    @Test
    fun `save toggles on and off`() {
        val state = FeedState()
        state.toggleSave("cuid_3")
        assertTrue(state.saved.contains("cuid_3"))
        state.toggleSave("cuid_3")
        assertFalse(state.saved.contains("cuid_3"))
    }

    @Test
    fun `like and dislike are mutually exclusive`() {
        val state = FeedState()
        state.toggleLike("cuid_5")
        state.toggleDislike("cuid_5")
        assertFalse(state.liked.contains("cuid_5"))
        assertTrue(state.disliked.contains("cuid_5"))
    }
}
