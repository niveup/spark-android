package com.spark.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Retrofit interface generated from docs/API_CONTRACT.md
// (Phase 5a: health + auth + feed read; Phase 5b: state sync + sessions + stats).
interface SparkApi {
    @GET("api/mobile/v1/health")
    suspend fun health(): DataResponse<HealthData>

    @POST("api/mobile/v1/auth/login")
    suspend fun login(@Body body: LoginRequest): DataResponse<LoginData>

    @POST("api/mobile/v1/auth/logout")
    suspend fun logout(@Header("Authorization") auth: String): DataResponse<Revoked>

    @GET("api/mobile/v1/feed")
    suspend fun feed(
        @Header("Authorization") auth: String,
        @Query("limit") limit: Int = 20,
        @Query("cursor") cursor: String? = null,
        @Query("sessionId") sessionId: String? = null,
        @Query("excludeSeen") excludeSeen: Boolean = true
    ): FeedPage

    /** Batched interaction upload; replays are idempotent server-side. */
    @POST("api/mobile/v1/items/state")
    suspend fun postStateBatch(
        @Header("Authorization") auth: String,
        @Body body: StateUpdateBody
    ): DataResponse<BatchStateData>

    @POST("api/mobile/v1/sessions")
    suspend fun openSession(
        @Header("Authorization") auth: String,
        @Body body: SessionRequest
    ): DataResponse<SessionOpenData>

    @PATCH("api/mobile/v1/sessions/{id}")
    suspend fun closeSession(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Body body: SessionCloseRequest
    ): DataResponse<SessionCloseData>

    @GET("api/mobile/v1/items/saved")
    suspend fun savedItems(
        @Header("Authorization") auth: String,
        @Query("limit") limit: Int = 50,
        @Query("cursor") cursor: String? = null,
        @Query("q") q: String? = null
    ): FeedPage

    @GET("api/mobile/v1/stats")
    suspend fun stats(@Header("Authorization") auth: String): DataResponse<StatsData>
}
