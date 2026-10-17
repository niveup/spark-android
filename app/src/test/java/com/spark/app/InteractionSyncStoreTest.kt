package com.spark.app

import com.spark.app.data.remote.BatchStateData
import com.spark.app.data.remote.DataResponse
import com.spark.app.data.remote.FeedPage
import com.spark.app.data.remote.HealthData
import com.spark.app.data.remote.LoginData
import com.spark.app.data.remote.LoginRequest
import com.spark.app.data.remote.Revoked
import com.spark.app.data.remote.SessionCloseData
import com.spark.app.data.remote.SessionCloseRequest
import com.spark.app.data.remote.SessionOpenData
import com.spark.app.data.remote.SessionRequest
import com.spark.app.data.remote.SparkApi
import com.spark.app.data.remote.StateUpdateBody
import com.spark.app.data.remote.StatsData
import com.spark.app.sync.InteractionSync
import com.spark.app.sync.PendingEventStore
import com.spark.app.sync.StateEvents
import com.spark.app.sync.TokenReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Phase 7: InteractionSync × PendingEventStore.
 * Unsent events are persisted immediately, replay on startup, upload
 * oldest-first, and leave the store only after the server accepted them.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InteractionSyncStoreTest {

    /** Capture-all SparkApi: consumes [failuresRemaining] state posts with IOException. */
    private class FakeApi(var failuresRemaining: Int) : SparkApi {
        val attempts get() = attemptsMade
        val batches = mutableListOf<List<JsonObject>>()
        private var attemptsMade = 0

        override suspend fun postStateBatch(
            auth: String,
            body: StateUpdateBody
        ): DataResponse<BatchStateData> {
            attemptsMade++
            if (failuresRemaining > 0) {
                failuresRemaining--
                throw IOException("offline")
            }
            batches.add(body.updates)
            return DataResponse(BatchStateData())
        }

        override suspend fun health(): DataResponse<HealthData> = error("unused")
        override suspend fun login(body: LoginRequest): DataResponse<LoginData> = error("unused")
        override suspend fun logout(auth: String): DataResponse<Revoked> = error("unused")
        override suspend fun feed(
            auth: String,
            limit: Int,
            cursor: String?,
            sessionId: String?,
            excludeSeen: Boolean
        ): FeedPage = error("unused")

        override suspend fun openSession(
            auth: String,
            body: SessionRequest
        ): DataResponse<SessionOpenData> = error("unused")

        override suspend fun closeSession(
            auth: String,
            id: String,
            body: SessionCloseRequest
        ): DataResponse<SessionCloseData> = error("unused")

        override suspend fun savedItems(
            auth: String,
            limit: Int,
            cursor: String?,
            q: String?
        ): FeedPage = error("unused")

        override suspend fun stats(auth: String): DataResponse<StatsData> = error("unused")
    }

    /** In-memory PendingEventStore with call recording (stands in for Room). */
    private class FakeStore : PendingEventStore {
        val rows = linkedMapOf<String, JsonObject>()
        val persistCalls = mutableListOf<List<String>>()
        val deleteCalls = mutableListOf<List<String>>()

        override suspend fun persist(events: List<JsonObject>) {
            persistCalls.add(events.map { it.clientEventId() })
            events.forEach { rows[it.clientEventId()] = it }
        }

        override suspend fun all(): List<JsonObject> = rows.values.toList()

        override suspend fun delete(clientEventIds: List<String>) {
            deleteCalls.add(clientEventIds)
            clientEventIds.forEach { rows.remove(it) }
        }

        override suspend fun clear() = rows.clear()
    }

    private fun newSync(
        api: SparkApi,
        store: PendingEventStore,
        scope: CoroutineScope
    ): InteractionSync = InteractionSync(
        api = api,
        tokens = TokenReader { "tok" },
        scope = scope,
        store = store
    )

    @Test
    fun `records are persisted even before any flush`() = runTest {
        val api = FakeApi(failuresRemaining = 0)
        val store = FakeStore()
        val sync = newSync(api, store, this)
        advanceUntilIdle() // let the construction-time pass settle (empty store -> no uploads)

        sync.record(StateEvents.seen("i-1"))
        sync.record(StateEvents.saved("i-2", saved = true))
        advanceUntilIdle() // below the batch limit (10) -> no auto-flush

        assertEquals(2, store.rows.size)
        assertEquals(0, api.batches.size)
        assertEquals(2, sync.pending.value)
    }

    @Test
    fun `flush uploads oldest-first and clears the store rows`() = runTest {
        val api = FakeApi(failuresRemaining = 0)
        val store = FakeStore()
        val sync = newSync(api, store, this)
        advanceUntilIdle() // settle the construction-time pass

        sync.record(StateEvents.seen("i-1"))
        sync.record(StateEvents.seen("i-2"))
        sync.flush()
        advanceUntilIdle()

        val batch = api.batches.single()
        assertEquals(2, batch.size)
        val ids = batch.map { it.clientEventId() }
        // The store lost exactly the uploaded ids, in the upload batch.
        assertEquals(ids, store.deleteCalls.single())
        assertTrue(store.rows.isEmpty())
        assertEquals(0, sync.pending.value)
        assertEquals(1, api.attempts)
    }

    @Test
    fun `failed upload keeps the rows until the retry succeeds`() = runTest {
        val api = FakeApi(failuresRemaining = 1)
        val store = FakeStore()
        val sync = newSync(api, store, this)
        advanceUntilIdle() // settle the construction-time pass

        sync.record(StateEvents.seen("i-1"))
        sync.flush()
        advanceUntilIdle() // drain fails -> virtual 5s backoff -> retry succeeds

        assertEquals(2, api.attempts) // failed attempt + successful retry
        assertEquals(1, api.batches.size)
        assertEquals(api.batches.single().map { it.clientEventId() }, store.deleteCalls.single())
        assertTrue(store.rows.isEmpty())
        assertEquals(0, sync.pending.value)
    }

    @Test
    fun `startup replays persisted events and uploads them once`() = runTest {
        val store = FakeStore()
        // A previous session recorded two events and never uploaded them.
        store.persist(listOf(StateEvents.seen("i-1"), StateEvents.rating("i-2", liked = true)))
        val api = FakeApi(failuresRemaining = 0)

        val sync = newSync(api, store, this)
        advanceUntilIdle()

        assertEquals(1, api.batches.size)
        assertEquals(2, api.batches.single().size)
        assertEquals(api.batches.single().map { it.clientEventId() }, store.deleteCalls.single())
        assertTrue(store.rows.isEmpty())
        assertEquals(0, sync.pending.value)
    }
}

/** clientEventId is the idempotency key of every queued update. */
private fun JsonObject.clientEventId(): String =
    (this["clientEventId"] as JsonPrimitive).content
