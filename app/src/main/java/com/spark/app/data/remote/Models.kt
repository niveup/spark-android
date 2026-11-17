package com.spark.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

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
    @SerialName("saved") val saved: Boolean = false,
    @SerialName("liked") val liked: Boolean? = null,
    @SerialName("dismissed") val dismissed: Boolean = false,
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

@Serializable
data class SubtitleCueDto(
    @SerialName("startMs") val startMs: Long = 0L,
    @SerialName("endMs") val endMs: Long = 0L,
    @SerialName("text") val text: String = ""
)

// --- Phase 5b: interaction sync + sessions + stats ---

/** Body of POST /items/state; each update is a StateEvents-built JSON object. */
@Serializable
data class StateUpdateBody(
    @SerialName("updates") val updates: List<JsonObject>
)

@Serializable
data class ItemStateDto(
    @SerialName("itemId") val itemId: String,
    @SerialName("seenAt") val seenAt: String? = null,
    @SerialName("saved") val saved: Boolean = false,
    @SerialName("liked") val liked: Boolean? = null,
    @SerialName("dismissed") val dismissed: Boolean = false,
    @SerialName("dwellMs") val dwellMs: Long = 0,
    @SerialName("srsDueAt") val srsDueAt: String? = null
)

@Serializable
data class BatchStateData(
    @SerialName("states") val states: List<ItemStateDto> = emptyList(),
    @SerialName("rejected") val rejected: List<String> = emptyList()
)

@Serializable
data class SessionRequest(
    @SerialName("deviceId") val deviceId: String,
    @SerialName("cardLimit") val cardLimit: Int,
    @SerialName("minuteLimit") val minuteLimit: Int
)

@Serializable
data class SessionOpenData(
    @SerialName("sessionId") val sessionId: String,
    @SerialName("startedAt") val startedAt: String = ""
)

@Serializable
data class SessionCloseRequest(
    @SerialName("itemsSeen") val itemsSeen: Int,
    @SerialName("limitReached") val limitReached: Boolean
)

@Serializable
data class SessionCloseData(
    @SerialName("sessionId") val sessionId: String,
    @SerialName("endedAt") val endedAt: String = ""
)

@Serializable
data class StatsData(
    @SerialName("seenTotal") val seenTotal: Int = 0,
    @SerialName("savedTotal") val savedTotal: Int = 0,
    @SerialName("likedTotal") val likedTotal: Int = 0,
    @SerialName("quizzesDue") val quizzesDue: Int = 0,
    @SerialName("sessionsTotal") val sessionsTotal: Int = 0
)

@Serializable
data class BookChapterDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("type") val type: String = "summary",
    @SerialName("audioUrl") val audioUrl: String? = null,
    @SerialName("durationMs") val durationMs: Long? = null,
    @SerialName("actionLine") val actionLine: String = ""
)

@Serializable
data class BookDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("author") val author: String = "Unknown",
    @SerialName("coverUrl") val coverUrl: String? = null,
    @SerialName("hasAudio") val hasAudio: Boolean = false,
    @SerialName("chapterCount") val chapterCount: Int = 0,
    @SerialName("chapters") val chapters: List<BookChapterDto> = emptyList()
)
