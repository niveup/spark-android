package com.spark.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

// Retrofit interface generated from docs/API_CONTRACT.md (Phase 4 subset:
// health + auth + feed read. Writes arrive in Phase 5b.)
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
}
