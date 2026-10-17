package com.spark.app.data.remote

import com.spark.app.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

val sparkJson: Json = Json {
    ignoreUnknownKeys = true // additive server fields never break the app
    explicitNulls = false
}

// Timeouts kept short: phone on mobile data should fail fast, not hang.
fun buildSparkApi(baseUrl: String = BuildConfig.API_BASE_URL): SparkApi {
    val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    return Retrofit.Builder()
        .baseUrl(url)
        .client(client)
        .addConverterFactory(sparkJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(SparkApi::class.java)
}
