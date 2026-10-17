package com.spark.app.sync

import com.spark.app.data.TokenStore
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

/**
 * Uploads interaction events to POST /items/state in batches.
 *
 * - Flush triggers: batch limit reached, explicit [flush] (session end), or
 *   the backoff timer after a failure.
 * - Failures keep the queue intact and retry with exponential backoff
 *   (5s -> 60s cap). Nothing is lost while the app stays open.
 * - Idempotent end-to-end: every event carries a clientEventId, so a replay
 *   after a network timeout never double-counts; unknown items are dropped.
 */
class InteractionSync(
    private val api: SparkApi,
    private val tokens: TokenStore,
    private val scope: CoroutineScope,
    private val flushAt: Int = 10
) {
    private val queue = InteractionQueue()
    private val drainMutex = Mutex()
    private var retryJob: Job? = null
    private var backoffMs = INITIAL_BACKOFF_MS

    private val _pending = MutableStateFlow(0)

    /** Events not yet accepted by the server (shown in the Me tab). */
    val pending: StateFlow<Int> = _pending.asStateFlow()

    fun record(update: JsonObject) {
        queue.enqueue(update)
        _pending.value = queue.size
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
                val token = tokens.getToken() ?: return
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

    private companion object {
        const val INITIAL_BACKOFF_MS = 5_000L
        const val MAX_BACKOFF_MS = 60_000L
    }
}
