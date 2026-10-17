package com.spark.app

import com.spark.app.sync.InteractionQueue
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractionQueueTest {

    private fun update(n: Int): JsonObject = buildJsonObject { put("clientEventId", "e$n") }

    private fun idOf(obj: JsonObject): String =
        (obj["clientEventId"] as JsonPrimitive).content

    @Test
    fun `peek is oldest-first and does not remove`() {
        val queue = InteractionQueue()
        queue.enqueue(update(1))
        queue.enqueue(update(2))
        assertEquals(2, queue.size)
        val batch = queue.peekBatch()
        assertEquals("e1", idOf(batch[0]))
        assertEquals("e2", idOf(batch[1]))
        assertEquals(2, queue.size) // still pending
    }

    @Test
    fun `flush removes exactly the sent head`() {
        val queue = InteractionQueue()
        queue.enqueue(update(1))
        queue.enqueue(update(2))
        queue.enqueue(update(3))
        val batch = queue.peekBatch().take(2)
        queue.onFlushed(batch)
        assertEquals(1, queue.size)
        assertEquals("e3", idOf(queue.peekBatch().first()))
    }

    @Test
    fun `peek caps a batch at 100`() {
        val queue = InteractionQueue()
        repeat(150) { queue.enqueue(update(it)) }
        assertEquals(100, queue.peekBatch().size)
        assertEquals(150, queue.size)
    }

    @Test
    fun `empty queue peeks empty`() {
        val queue = InteractionQueue()
        assertTrue(queue.isEmpty)
        assertTrue(queue.peekBatch().isEmpty())
    }

    @Test
    fun `flush with an oversized batch never underflows`() {
        val queue = InteractionQueue()
        queue.enqueue(update(1))
        queue.enqueue(update(2))
        queue.onFlushed(listOf(update(1), update(2), update(3)))
        assertEquals(0, queue.size)
        assertTrue(queue.isEmpty)
    }
}
