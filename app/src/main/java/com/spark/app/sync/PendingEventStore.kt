package com.spark.app.sync

import kotlinx.serialization.json.JsonObject

/**
 * Where InteractionSync keeps events that failed to upload (Phase 7: Room
 * persistence across app restarts). Everything is suspend + idempotent:
 * persist() upserts by clientEventId, delete() removes only once-accepted
 * rows. The default no-op keeps the queue in memory only.
 */
interface PendingEventStore {
    suspend fun persist(events: List<JsonObject>)
    suspend fun all(): List<JsonObject>
    suspend fun delete(clientEventIds: List<String>)
    suspend fun clear()
}

object NoopPendingEventStore : PendingEventStore {
    override suspend fun persist(events: List<JsonObject>) = Unit
    override suspend fun all(): List<JsonObject> = emptyList()
    override suspend fun delete(clientEventIds: List<String>) = Unit
    override suspend fun clear() = Unit
}

/**
 * Minimal token seam so InteractionSync stays testable on the JVM without a
 * real (EncryptedSharedPreferences-backed) TokenStore instance. Synchronous
 * by design — the real store reads prefs synchronously.
 */
fun interface TokenReader {
    fun getToken(): String?
}
