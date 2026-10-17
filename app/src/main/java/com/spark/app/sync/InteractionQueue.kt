package com.spark.app.sync

import kotlinx.serialization.json.JsonObject

/**
 * In-memory FIFO of pending /items/state updates. Pure logic (no coroutines,
 * no Android) so it is unit-tested directly. Persistence arrives with Room in
 * a later phase; until then unsent events live only for the app session.
 */
class InteractionQueue(private val maxBatch: Int = 100) {

    private val pending = ArrayDeque<JsonObject>()

    val size: Int get() = pending.size
    val isEmpty: Boolean get() = pending.isEmpty()

    fun enqueue(update: JsonObject) {
        pending.addLast(update)
    }

    /** Oldest-first snapshot of up to [maxBatch] updates; empty when drained. */
    fun peekBatch(): List<JsonObject> = pending.take(maxBatch)

    /** Removes the head entries that were successfully flushed. */
    fun onFlushed(batch: List<JsonObject>) {
        var removed = 0
        while (removed < batch.size && pending.isNotEmpty()) {
            pending.removeFirst()
            removed++
        }
    }
}
