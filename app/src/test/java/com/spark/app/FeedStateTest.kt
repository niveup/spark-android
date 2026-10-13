package com.spark.app

import com.spark.app.data.SESSION_CARD_LIMIT
import com.spark.app.feed.FeedState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedStateTest {
    @Test
    fun `starts at 0 seen, not done`() {
        val state = FeedState()
        assertEquals(0, state.seenCount)
        assertFalse(state.done)
        assertEquals(SESSION_CARD_LIMIT, FeedState.limit())
    }

    @Test
    fun `reaching last page then finish ends session`() {
        val state = FeedState()
        for (page in 0 until SESSION_CARD_LIMIT) state.onPageSettled(page)
        assertEquals(SESSION_CARD_LIMIT, state.seenCount)
        assertTrue(state.isLastPage(SESSION_CARD_LIMIT))
        state.finish()
        assertTrue(state.done)
    }

    @Test
    fun `save toggles on and off`() {
        val state = FeedState()
        state.toggleSave(3)
        assertTrue(state.saved.contains(3))
        state.toggleSave(3)
        assertFalse(state.saved.contains(3))
    }

    @Test
    fun `like and dislike are mutually exclusive`() {
        val state = FeedState()
        state.toggleLike(5)
        state.toggleDislike(5)
        assertFalse(state.liked.contains(5))
        assertTrue(state.disliked.contains(5))
    }
}
