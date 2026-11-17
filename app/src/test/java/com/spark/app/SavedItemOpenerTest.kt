package com.spark.app

import com.spark.app.data.local.ItemCache
import com.spark.app.data.remote.BatchStateData
import com.spark.app.data.remote.BookDto
import com.spark.app.data.remote.SubtitleCueDto
import com.spark.app.data.remote.DataResponse
import com.spark.app.data.remote.FeedItemDto
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
import com.spark.app.feed.SavedItemOpener
import com.spark.app.feed.SavedItemResult
import com.spark.app.sync.TokenReader
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException

/**
 * The Saved → Reels rule: a saved card is stored as its id, and opening it
 * maps that id back to a card — from the server when there is a connection,
 * otherwise from this device's cache.
 */
class SavedItemOpenerTest {

    @Test
    fun onlineFetchesTheCardByIdAndCachesIt() = runTest {
        val api = FakeApi { id -> card(id) }
        val cache = FakeCache()

        val result = SavedItemOpener(api, TokenReader { "token" }, cache).open("card-1")

        val found = result as SavedItemResult.Found
        assertEquals("card-1", found.card.id)
        assertEquals(false, found.fromCache)
        assertEquals(1, api.itemCalls)
        // Cached right away: the next offline open finds it on the device.
        assertEquals(listOf("card-1"), cache.saved.map { it.id })
    }

    @Test
    fun offlineLooksTheSameIdUpInTheDeviceCache() = runTest {
        val api = FakeApi { throw java.io.IOException("offline") }
        val cache = FakeCache().apply { rows["card-1"] = card("card-1") }

        val result = SavedItemOpener(api, TokenReader { "token" }, cache).open("card-1")

        val found = result as SavedItemResult.Found
        assertEquals("card-1", found.card.id)
        assertEquals(true, found.fromCache)
        assertEquals(1, api.itemCalls)
    }

    @Test
    fun offlineOnlyNeverTouchesTheNetwork() = runTest {
        val api = FakeApi { card("card-1") }
        val cache = FakeCache().apply { rows["card-1"] = card("card-1") }

        val result = SavedItemOpener(api, TokenReader { "token" }, cache)
            .open("card-1", offlineOnly = true)

        assertEquals(0, api.itemCalls)
        assertEquals(true, (result as SavedItemResult.Found).fromCache)
    }

    @Test
    fun aCardTheServerNoLongerHasFallsBackToTheCachedCopy() = runTest {
        val api = FakeApi { throw HttpException(notFound()) }
        val cache = FakeCache().apply { rows["card-1"] = card("card-1") }

        val result = SavedItemOpener(api, TokenReader { "token" }, cache).open("card-1")

        assertEquals(true, (result as SavedItemResult.Found).fromCache)
    }

    @Test
    fun aCardThatIsNowhereToBeFoundIsUnavailable() = runTest {
        val api = FakeApi { throw java.io.IOException("offline") }
        val cache = FakeCache()

        val result = SavedItemOpener(api, TokenReader { "token" }, cache).open("card-1")

        assertTrue(result is SavedItemResult.Unavailable)
    }

    @Test
    fun withoutATokenTheCacheIsUsed() = runTest {
        val api = FakeApi { card("card-1") }
        val cache = FakeCache().apply { rows["card-1"] = card("card-1") }

        val result = SavedItemOpener(api, TokenReader { null }, cache).open("card-1")

        assertEquals(0, api.itemCalls)
        assertEquals(true, (result as SavedItemResult.Found).fromCache)
    }

    private fun card(id: String) = FeedItemDto(
        id = id,
        type = "concept",
        title = "Title $id",
        bodyMd = "Body",
        saved = true
    )

    /** A 404 response, the way Retrofit reports a card the server no longer has. */
    private fun notFound(): retrofit2.Response<Any> =
        retrofit2.Response.error<Any>(404, "".toResponseBody())

    /** In-memory stand-in for the Room cache. */
    private class FakeCache : ItemCache {
        val rows = linkedMapOf<String, FeedItemDto>()
        val saved = mutableListOf<FeedItemDto>()

        override suspend fun feedDtos(): List<FeedItemDto> = rows.values.toList()

        override suspend fun savedDtos(): List<FeedItemDto> = rows.values.filter { it.saved }

        override suspend fun itemById(id: String): FeedItemDto? = rows[id]

        override suspend fun saveFeed(items: List<FeedItemDto>) {
            items.forEach {
                rows[it.id] = it
                saved.add(it)
            }
        }
    }

    /** Serves one item by id; the lambda may throw to simulate a failure. */
    private class FakeApi(private val onItem: (String) -> FeedItemDto) : SparkApi {
        var itemCalls = 0

        override suspend fun item(auth: String, id: String): DataResponse<FeedItemDto> {
            itemCalls++
            return DataResponse(onItem(id))
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

        override suspend fun postStateBatch(
            auth: String,
            body: StateUpdateBody
        ): DataResponse<BatchStateData> = error("unused")

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

        override suspend fun books(auth: String): DataResponse<List<BookDto>> = error("unused")
        override suspend fun subtitles(key: String): DataResponse<List<SubtitleCueDto>> = DataResponse(emptyList())

        override suspend fun stats(auth: String): DataResponse<StatsData> = error("unused")
    }
}
