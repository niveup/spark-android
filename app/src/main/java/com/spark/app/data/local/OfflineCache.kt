package com.spark.app.data.local

import android.content.Context
import com.spark.app.data.remote.FeedItemDto
import com.spark.app.sync.PendingEventStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.atomic.AtomicLong

/**
 * Phase 7 offline facade over Room:
 *  - [saveFeed]: cache the cards each successful online load returned, then
 *    prune to CACHE_CAP fresh cards (saved ones are always kept).
 *  - [feedDtos] / [savedDtos]: offline rebuilds for the Reels / Saved tabs
 *    (server-wins: the cache is overwritten on every online load).
 *  - PendingEventStore impl: persists queued interaction events across app
 *    restarts until the server accepts them (idempotent via clientEventId).
 */
class OfflineCache(context: Context) : PendingEventStore {

    private val db = OfflineDatabase.build(context)

    suspend fun saveFeed(items: List<FeedItemDto>) {
        if (items.isEmpty()) return
        val entities = items.toCachedEntities(batchMs())
        db.cachedItemDao().upsertAll(entities)
        applyPendingSavedChanges()
        val all = db.cachedItemDao().all()
        val keptIds = pruneForCap(all, CACHE_CAP).map { it.id }.toSet()
        val evictIds = all.map { it.id }.filterNot { it in keptIds }
        if (evictIds.isNotEmpty()) db.cachedItemDao().deleteByIds(evictIds)
    }

    /** Full cached deck, most recent batch first, server order inside a batch. */
    suspend fun feedDtos(): List<FeedItemDto> {
        applyPendingSavedChanges()
        return db.cachedItemDao().all().map { it.toFeedItemDto() }
    }

    /** Saved cards only (however they got flagged — feed or the saved list). */
    suspend fun savedDtos(): List<FeedItemDto> {
        applyPendingSavedChanges()
        return db.cachedItemDao().saved().map { it.toFeedItemDto() }
    }

    suspend fun cachedCount(): Int = db.cachedItemDao().count()

    fun observeCachedCount(): Flow<Int> = db.cachedItemDao().observeCount()

    suspend fun clearCachedCards() {
        db.cachedItemDao().clear()
    }

    // --- PendingEventStore ---

    override suspend fun persist(events: List<JsonObject>) {
        if (events.isEmpty()) return
        val now = System.currentTimeMillis()
        val rows = events.map { event ->
            PendingEventEntity(
                clientEventId = requireClientId(event),
                payload = event.toString(),
                createdAtMs = now
            )
        }
        db.pendingEventDao().upsertAll(rows)
        applyPendingSavedChanges()
    }

    override suspend fun all(): List<JsonObject> =
        db.pendingEventDao().all().mapNotNull { row ->
            runCatching { json.parseToJsonElement(row.payload) as? JsonObject }.getOrNull()
        }

    override suspend fun delete(clientEventIds: List<String>) {
        if (clientEventIds.isEmpty()) return
        db.pendingEventDao().deleteByIds(clientEventIds)
    }

    override suspend fun clear() {
        db.pendingEventDao().clear()
    }

    /**
     * Batch timestamp: rows of one load share one timestamp; next load is
     * (in practice) seconds later, so cachedAtMs DESC replays batches newest
     * first. Inside a batch, deckIndex carries the server's ranked order.
     */
    private fun batchMs(): Long = System.currentTimeMillis()

    /**
     * Every StateEvents builder stamps clientEventId — it is the row key, and
     * the server's idempotency key, so an event without one is a bug.
     */
    private fun requireClientId(event: JsonObject): String {
        val raw = event[CLIENT_EVENT_ID_KEY]
        require(raw is JsonPrimitive && raw.isString && raw.content.isNotBlank()) {
            "pending events must carry a clientEventId"
        }
        return raw.content
    }

    private fun List<FeedItemDto>.toCachedEntities(batchMs: Long): List<CachedItemEntity> =
        mapIndexed { index, dto -> dto.toCachedEntity(batchMs, index) }

    /** Keep offline Saved in sync with queued Save/Unsave actions before upload. */
    private suspend fun applyPendingSavedChanges() {
        db.pendingEventDao().all().forEach { row ->
            val event = runCatching {
                json.parseToJsonElement(row.payload) as? JsonObject
            }.getOrNull() ?: return@forEach
            val itemId = (event[ITEM_ID_KEY] as? JsonPrimitive)
                ?.takeIf { it.isString }
                ?.content
                ?: return@forEach
            val saved = (event[SAVED_KEY] as? JsonPrimitive)?.booleanOrNull ?: return@forEach
            db.cachedItemDao().setSaved(itemId, saved)
        }
    }

    private companion object {
        const val CACHE_CAP = 50
        const val CLIENT_EVENT_ID_KEY = "clientEventId"
        const val ITEM_ID_KEY = "itemId"
        const val SAVED_KEY = "saved"
        val json = Json { ignoreUnknownKeys = true }
    }
}
