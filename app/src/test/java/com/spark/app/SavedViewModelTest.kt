package com.spark.app

import com.spark.app.data.local.SavedCardsCache
import com.spark.app.data.remote.BatchStateData
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
import com.spark.app.home.SavedUiState
import com.spark.app.home.SavedViewModel
import com.spark.app.sync.TokenReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SavedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeApi : SparkApi {
        var shouldThrowIoException = false
        var httpErrorCode: Int? = null
        var returnedItems: List<FeedItemDto> = emptyList()

        override suspend fun savedItems(
            auth: String,
            limit: Int,
            cursor: String?,
            q: String?
        ): FeedPage {
            if (shouldThrowIoException) throw IOException("Network unreachable")
            httpErrorCode?.let { code ->
                throw HttpException(Response.error<FeedPage>(code, "".toResponseBody()))
            }
            return FeedPage(data = returnedItems)
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
        override suspend fun stats(auth: String): DataResponse<StatsData> = error("unused")
        override suspend fun postStateBatch(
            auth: String,
            body: StateUpdateBody
        ): DataResponse<BatchStateData> = error("unused")
    }

    private class FakeCache(var items: List<FeedItemDto> = emptyList()) : SavedCardsCache {
        val savedFeeds = mutableListOf<List<FeedItemDto>>()

        override suspend fun savedDtos(): List<FeedItemDto> = items

        override suspend fun saveFeed(items: List<FeedItemDto>) {
            savedFeeds.add(items)
            this.items = items
        }
    }

    private fun sampleItem(id: String, title: String, body: String = "") = FeedItemDto(
        id = id,
        type = "quote",
        title = title,
        bodyMd = body,
        saved = true
    )

    @Test
    fun `init pre-populates state with cached cards`() = runTest(testDispatcher) {
        val cached = listOf(sampleItem("c1", "Atomic Habits"))
        val cache = FakeCache(cached)
        val api = FakeApi()
        val tokens = TokenReader { "test-token" }

        val vm = SavedViewModel(api, tokens, cache)
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is SavedUiState.Content)
        val content = state as SavedUiState.Content
        assertEquals(1, content.cards.size)
        assertEquals("Atomic Habits", content.cards[0].title)
        assertTrue(content.offline)
    }

    @Test
    fun `load shows cached cards immediately when offline`() = runTest(testDispatcher) {
        val cached = listOf(sampleItem("c1", "Deep Work"))
        val cache = FakeCache(cached)
        val api = FakeApi().apply { shouldThrowIoException = true }
        val tokens = TokenReader { "test-token" }

        val vm = SavedViewModel(api, tokens, cache)
        vm.load("")
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is SavedUiState.Content)
        val content = state as SavedUiState.Content
        assertEquals(1, content.cards.size)
        assertEquals("Deep Work", content.cards[0].title)
        assertTrue(content.offline)
    }

    @Test
    fun `load filters cached cards by query when offline`() = runTest(testDispatcher) {
        val cached = listOf(
            sampleItem("c1", "Deep Work", body = "Focus completely"),
            sampleItem("c2", "Atomic Habits", body = "Small changes")
        )
        val cache = FakeCache(cached)
        val api = FakeApi().apply { shouldThrowIoException = true }
        val tokens = TokenReader { "test-token" }

        val vm = SavedViewModel(api, tokens, cache)
        vm.load("Habits")
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is SavedUiState.Content)
        val content = state as SavedUiState.Content
        assertEquals(1, content.cards.size)
        assertEquals("Atomic Habits", content.cards[0].title)
    }

    @Test
    fun `load emits error when offline and cache is empty`() = runTest(testDispatcher) {
        val cache = FakeCache(emptyList())
        val api = FakeApi().apply { shouldThrowIoException = true }
        val tokens = TokenReader { "test-token" }

        val vm = SavedViewModel(api, tokens, cache)
        vm.load("")
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is SavedUiState.Error)
        val error = state as SavedUiState.Error
        assertTrue(error.message.contains("Can't reach your PC"))
    }

    @Test
    fun `load emits online content and saves to cache when server succeeds`() = runTest(testDispatcher) {
        val cache = FakeCache(emptyList())
        val serverItems = listOf(sampleItem("s1", "Server Book"))
        val api = FakeApi().apply { returnedItems = serverItems }
        val tokens = TokenReader { "test-token" }

        val vm = SavedViewModel(api, tokens, cache)
        vm.load("")
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is SavedUiState.Content)
        val content = state as SavedUiState.Content
        assertEquals(1, content.cards.size)
        assertEquals("Server Book", content.cards[0].title)
        assertEquals(false, content.offline)
        assertEquals(1, cache.savedFeeds.size)
        assertEquals(serverItems, cache.savedFeeds[0])
    }

    @Test
    fun `load emits session expired on 401 HTTP response`() = runTest(testDispatcher) {
        val cache = FakeCache(emptyList())
        val api = FakeApi().apply { httpErrorCode = 401 }
        val tokens = TokenReader { "test-token" }

        val vm = SavedViewModel(api, tokens, cache)
        vm.load("")
        advanceUntilIdle()

        val state = vm.state.value
        assertTrue(state is SavedUiState.Error)
        val error = state as SavedUiState.Error
        assertTrue(error.message.contains("Session expired"))
    }
}
