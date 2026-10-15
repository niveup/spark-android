package com.spark.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors docs/API_CONTRACT.md. Unknown JSON fields are ignored so the app
// never crashes on additive server changes (contract § compatibility).

@Serializable
data class DataResponse<T>(
    @SerialName("data") val data: T
)

@Serializable
data class ErrorBody(
    @SerialName("code") val code: String = "",
    @SerialName("message") val message: String = ""
)

@Serializable
data class ErrorResponse(
    @SerialName("error") val error: ErrorBody = ErrorBody()
)

@Serializable
data class LoginRequest(
    @SerialName("passcode") val passcode: String,
    @SerialName("deviceLabel") val deviceLabel: String
)

@Serializable
data class LoginData(
    @SerialName("token") val token: String,
    @SerialName("expiresAt") val expiresAt: String
)

@Serializable
data class Revoked(
    @SerialName("revoked") val revoked: Boolean = false
)

@Serializable
data class QuizDto(
    @SerialName("options") val options: List<String> = emptyList(),
    @SerialName("correctIndex") val correctIndex: Int = 0,
    @SerialName("explanation") val explanation: String = ""
)

@Serializable
data class BookRef(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("author") val author: String = "",
    @SerialName("coverUrl") val coverUrl: String? = null
)

@Serializable
data class FeedItemDto(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("title") val title: String,
    @SerialName("bodyMd") val bodyMd: String,
    @SerialName("actionLine") val actionLine: String = "",
    @SerialName("topicTags") val topicTags: List<String> = emptyList(),
    @SerialName("language") val language: String = "en",
    @SerialName("estSeconds") val estSeconds: Int = 45,
    @SerialName("progressMs") val progressMs: Long? = null,
    @SerialName("quiz") val quiz: QuizDto? = null,
    @SerialName("audioUrl") val audioUrl: String? = null,
    @SerialName("durationMs") val durationMs: Long? = null,
    @SerialName("book") val book: BookRef? = null,
    @SerialName("sourceType") val sourceType: String = "",
    @SerialName("sourceRef") val sourceRef: String = "",
    @SerialName("license") val license: String = "",
    @SerialName("isAiGenerated") val isAiGenerated: Boolean = false,
    @SerialName("isGrounded") val isGrounded: Boolean = true,
    @SerialName("publishedAt") val publishedAt: String = ""
)

@Serializable
data class FeedPage(
    @SerialName("data") val data: List<FeedItemDto> = emptyList(),
    @SerialName("nextCursor") val nextCursor: String? = null
)

@Serializable
data class HealthData(
    @SerialName("ok") val ok: Boolean = false,
    @SerialName("version") val version: String = ""
)
