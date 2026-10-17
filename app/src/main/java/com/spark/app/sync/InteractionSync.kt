package com.spark.app.sync

import com.spark.app.data.remote.SparkApi
import com.spark.app.data.remote.StateUpdateBody
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Uploads interaction events to POST /items/state in batches.
 *
 * - Flush triggers: batch limit reached, explicit [flush] (session end), or
 *   the backoff timer after a failure.
 * - Failures keep the queue intact and retry with exponential backoff
 *   (5s -> 60s cap). Nothing is lost while the app stays open.
 * - Phase 7: a [PendingEventStore] (Room) persists unsent events across app
 *   restarts — on startup [restore] replays them oldest-first and retrys
 *   the upload automatically.
 * - Idempotent end-to-end: every event carries a clientEventId, so a replay
 *   after a network timeout never double-counts; unknown items are dropped.
 */
class InteractionSync(
    private val api: SparkApi,
    tokens: TokenReader,
    private val scope: CoroutineScope,
    private val flushAt: Int = 10,
    private val store: PendingEventStore = NoopPendingEventStore,
    /** Test seam only — production resolves the token from [TokenStore]. */
    private val tokenProvider: suspend () -> String? = { tokens.getToken() }
) {
    private val queue = InteractionQueue()
    private val drainMutex = Mutex()
    private var retryJob: Job? = null
    private var backoffMs = INITIAL_BACKOFF_MS

    init {
        // Replay events persisted by previous sessions, then immediately
        // retry the upload (they were never accepted by the server).
        scope.launch {
            drainMutex.withLock {
                queue.restore(store.all())
            }
            _pending.value = queue.size
            flush()
        }
    }

    private val _pending = MutableStateFlow(0)

    /** Events not yet accepted by the server (shown in the Me tab). */
    val pending: StateFlow<Int> = _pending.asStateFlow()

    fun record(update: JsonObject) {
        queue.enqueue(update)
        _pending.value = queue.size
        scope.launch { store.persist(listOf(update)) }
        if (queue.size >= flushAt) flush()
    }

    /** Best-effort: starts a drain unless one (or a retry timer) is running. */
    fun flush() {
        if (retryJob?.isActive == true) return
        retryJob = scope.launch { drain() }
    }

    /**
     * Suspends until the queued events are uploaded, or a retry is scheduled after a
     * failure. Used before screens that read server state (the Saved tab) so a
     * just-recorded save is visible immediately, not only after the next batch flush.
     */
    suspend fun awaitFlush() {
        if (queue.size > 0) drain()
    }

    private suspend fun drain() {
        drainMutex.withLock {
            while (true) {
                val batch = queue.peekBatch()
                if (batch.isEmpty()) {
                    backoffMs = INITIAL_BACKOFF_MS
                    _pending.value = 0
                    return
                }
                val token = tokenProvider() ?: return
                val uploaded = try {
                    api.postStateBatch("Bearer $token", StateUpdateBody(batch))
                    true
                } catch (e: CancellationException) {
                    throw e // the caller's scope was cancelled — keep the queue
                } catch (_: Exception) {
                    false
                }
                if (!uploaded) {
                    scheduleRetry()
                    return
                }
                queue.onFlushed(batch)
                // Server accepted the batch — drop only those rows from the
                // persistent store (idempotent replays are safe anyway).
                store.delete(batch.mapNotNull { it.clientEventIdOrNull() })
                _pending.value = queue.size
            }
        }
    }

    private fun scheduleRetry() {
        val waitMs = backoffMs
        backoffMs = (backoffMs * 2).coerceAtMost(MAX_BACKOFF_MS)
        retryJob = scope.launch {
            delay(waitMs)
            drain()
        }
    }

    private fun JsonObject.clientEventIdOrNull(): String? =
        (this["clientEventId"] as? JsonPrimitive)?.contentOrNull

    private companion object {
        const val INITIAL_BACKOFF_MS = 5_000L
        const val MAX_BACKOFF_MS = 60_000L
    }
}
