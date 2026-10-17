package com.spark.app.sync

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

/**
 * Builds POST /items/state update payloads (docs/API_CONTRACT.md).
 *
 * Every call creates a fresh clientEventId (UUID) unless one is supplied.
 * The server ignores replays of the same id, so dwell time never double-counts.
 *
 * Fields are assembled as JSON by hand on purpose: "liked": null (clear the
 * rating) must be sent explicitly, while unrelated fields must stay absent —
 * kotlinx data classes with explicitNulls=false cannot express both.
 */
object StateEvents {

    fun newEventId(): String = UUID.randomUUID().toString()

    fun seen(itemId: String, eventId: String = newEventId()): JsonObject =
        event(itemId, eventId) { put("seen", true) }

    fun saved(itemId: String, saved: Boolean, eventId: String = newEventId()): JsonObject =
        event(itemId, eventId) { put("saved", saved) }

    /** true = like, false = dislike, null = clear the rating (explicit JSON null). */
    fun rating(itemId: String, liked: Boolean?, eventId: String = newEventId()): JsonObject =
        event(itemId, eventId) {
            val value: JsonElement = if (liked == null) JsonNull else JsonPrimitive(liked)
            put("liked", value)
        }

    fun dwell(itemId: String, dwellMs: Long, eventId: String = newEventId()): JsonObject =
        event(itemId, eventId) { put("dwellMs", dwellMs) }

    fun quiz(itemId: String, correct: Boolean, eventId: String = newEventId()): JsonObject =
        event(itemId, eventId) { put("quizCorrect", correct) }

    private fun event(
        itemId: String,
        eventId: String,
        extra: JsonObjectBuilder.() -> Unit
    ): JsonObject = buildJsonObject {
        put("itemId", itemId)
        put("clientEventId", eventId)
        extra()
    }
}
