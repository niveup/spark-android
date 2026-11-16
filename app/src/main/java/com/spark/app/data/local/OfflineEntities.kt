package com.spark.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * One cached feed/saved card (Phase 7). Columns mirror every field the UI
 * needs, so an offline deck is rebuilt through the same mapper as an online
 * one (CachedItemMapping.kt). Max 50 fresh cards; saved cards are never
 * evicted (pruneForCap).
 */
@Entity(tableName = "cached_items")
data class CachedItemEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val bodyMd: String,
    val actionLine: String,
    val topicTags: String,
    val language: String,
    val estSeconds: Int,
    val progressMs: Long?,
    val quizOptions: String?,
    val quizCorrectIndex: Int?,
    val quizExplanation: String?,
    val audioUrl: String?,
    val durationMs: Long?,
    val bookId: String?,
    val bookTitle: String?,
    val bookAuthor: String?,
    val bookCoverUrl: String?,
    val sourceType: String,
    val sourceRef: String,
    val license: String,
    val isAiGenerated: Boolean,
    val isGrounded: Boolean,
    val saved: Boolean,
    val liked: Boolean?,
    val dismissed: Boolean,
    val publishedAt: String,
    /** Batch timestamp; deckIndex keeps the server's ranked order inside a batch. */
    val cachedAtMs: Long,
    val deckIndex: Int = 0
)

/**
 * One interaction update whose upload failed or is still queued (Phase 7):
 * survives app restarts so offline save/like actions are never lost.
 * Rows replay oldest-first and are idempotent server-side via clientEventId.
 */
@Entity(tableName = "pending_events")
data class PendingEventEntity(
    @PrimaryKey val clientEventId: String,
    val payload: String,
    val createdAtMs: Long
)

@Dao
interface CachedItemDao {

    @Upsert
    suspend fun upsertAll(items: List<CachedItemEntity>)

    @Query("SELECT * FROM cached_items ORDER BY cachedAtMs DESC")
    suspend fun all(): List<CachedItemEntity>

    @Query("SELECT * FROM cached_items WHERE saved = 1 ORDER BY cachedAtMs DESC")
    suspend fun saved(): List<CachedItemEntity>

    @Query("SELECT * FROM cached_items WHERE id = :itemId LIMIT 1")
    suspend fun byId(itemId: String): CachedItemEntity?

    @Query("SELECT COUNT(*) FROM cached_items")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM cached_items")
    fun observeCount(): Flow<Int>

    @Query("UPDATE cached_items SET saved = :saved WHERE id = :itemId")
    suspend fun setSaved(itemId: String, saved: Boolean)

    @Query("DELETE FROM cached_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("DELETE FROM cached_items")
    suspend fun clear()
}

@Dao
interface PendingEventDao {

    @Upsert
    suspend fun upsertAll(events: List<PendingEventEntity>)

    @Query("SELECT * FROM pending_events ORDER BY createdAtMs ASC")
    suspend fun all(): List<PendingEventEntity>

    @Query("DELETE FROM pending_events WHERE clientEventId IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT COUNT(*) FROM pending_events")
    suspend fun count(): Int

    @Query("DELETE FROM pending_events")
    suspend fun clear()
}
