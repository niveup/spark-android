package com.spark.app

import com.spark.app.sync.StateEvents
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StateEventsTest {

    @Test
    fun `seen event carries itemId, eventId and seen=true`() {
        val json = StateEvents.seen("item-1", "evt-1")
        assertEquals(JsonPrimitive("item-1"), json["itemId"])
        assertEquals(JsonPrimitive("evt-1"), json["clientEventId"])
        assertEquals(JsonPrimitive(true), json["seen"])
        assertFalse(json.containsKey("saved"))
    }

    @Test
    fun `saved event carries the new value`() {
        val json = StateEvents.saved("item-1", saved = false, eventId = "evt-2")
        assertEquals(JsonPrimitive(false), json["saved"])
    }

    @Test
    fun `rating true and false are primitives`() {
        assertEquals(JsonPrimitive(true), StateEvents.rating("i", true, "e")["liked"])
        assertEquals(JsonPrimitive(false), StateEvents.rating("i", false, "e")["liked"])
    }

    @Test
    fun `rating null is an explicit JSON null (clear the rating)`() {
        val json = StateEvents.rating("i", null, "e")
        assertTrue(json.containsKey("liked"))
        assertEquals(JsonNull, json["liked"])
    }

    @Test
    fun `dwell and quiz events carry their fields`() {
        assertEquals(JsonPrimitive(1234L), StateEvents.dwell("i", 1234L, "e")["dwellMs"])
        assertEquals(JsonPrimitive(true), StateEvents.quiz("i", true, "e")["quizCorrect"])
    }

    @Test
    fun `default event ids are unique per event`() {
        assertNotEquals(StateEvents.newEventId(), StateEvents.newEventId())
        val first = StateEvents.seen("i")["clientEventId"]
        val second = StateEvents.seen("i")["clientEventId"]
        assertNotEquals(first, second)
    }
}
