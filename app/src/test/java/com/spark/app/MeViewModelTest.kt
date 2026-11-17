package com.spark.app

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
import com.spark.app.home.MeViewModel
import com.spark.app.home.StatsUiState
import com.spark.app.sync.TokenReader
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class MeViewModelTest {
    private lateinit var mainDispatcher: TestDispatcher

    @Before
    fun setUp() {
        mainDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun overlappingRefreshesShareTheInFlightRequest() = runTest(mainDispatcher) {
        val started = CompletableDeferred<Unit>()
        val response = CompletableDeferred<DataResponse<StatsData>>()
        val api = DeferredStatsApi(started, response)
        val expected = StatsData(seenTotal = 7, savedTotal = 2, sessionsTotal = 3)
        val viewModel = MeViewModel(api, TokenReader { "unit-test-token" })

        viewModel.refresh()
        withContext(Dispatchers.IO) {
            started.await()
        }
        viewModel.refresh()

        assertEquals(1, api.statsCalls.get())
        response.complete(DataResponse(expected))
        viewModel.state.first { it is StatsUiState.Content }
        assertEquals(StatsUiState.Content(expected), viewModel.state.value)
    }

    private class DeferredStatsApi(
        private val started: CompletableDeferred<Unit>,
        private val response: CompletableDeferred<DataResponse<StatsData>>
    ) : SparkApi {
        val statsCalls = AtomicInteger()

        override suspend fun stats(auth: String): DataResponse<StatsData> {
            statsCalls.incrementAndGet()
            started.complete(Unit)
            return response.await()
        }

        override suspend fun health(): DataResponse<HealthData> = error("Unexpected health call")
        override suspend fun login(body: LoginRequest): DataResponse<LoginData> = error("Unexpected login call")
        override suspend fun logout(auth: String): DataResponse<Revoked> = error("Unexpected logout call")
        override suspend fun feed(
            auth: String,
            limit: Int,
            cursor: String?,
            sessionId: String?,
            excludeSeen: Boolean
        ): FeedPage = error("Unexpected feed call")

        override suspend fun postStateBatch(
            auth: String,
            body: StateUpdateBody
        ): DataResponse<BatchStateData> = error("Unexpected state call")

        override suspend fun openSession(
            auth: String,
            body: SessionRequest
        ): DataResponse<SessionOpenData> = error("Unexpected session-open call")

        override suspend fun closeSession(
            auth: String,
            id: String,
            body: SessionCloseRequest
        ): DataResponse<SessionCloseData> = error("Unexpected session-close call")

        override suspend fun savedItems(
            auth: String,
            limit: Int,
            cursor: String?,
            q: String?
        ): FeedPage = error("Unexpected saved-items call")

        override suspend fun item(auth: String, id: String): DataResponse<FeedItemDto> =
            error("Unexpected item call")

        override suspend fun books(auth: String): DataResponse<List<BookDto>> =
            error("Unexpected books call")
        override suspend fun subtitles(key: String): DataResponse<List<SubtitleCueDto>> =
            DataResponse(emptyList())
    }
}
