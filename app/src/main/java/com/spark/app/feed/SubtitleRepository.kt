package com.spark.app.feed

import com.spark.app.data.remote.SubtitleCueDto
import com.spark.app.data.remote.buildSparkApi
import java.util.concurrent.ConcurrentHashMap

object SubtitleRepository {
    private val cache = ConcurrentHashMap<String, List<SubtitleCueDto>>()
    private val api by lazy { buildSparkApi() }

    suspend fun getSubtitles(
        key: String,
        bodyFallback: String = "",
        durationLabel: String? = null
    ): List<SubtitleCueDto> {
        if (key.isBlank()) return parseFallbackCues(bodyFallback, durationLabel)

        cache[key]?.let { return it }

        return try {
            val response = api.subtitles(key)
            val cues = response.data ?: emptyList()
            if (cues.isNotEmpty()) {
                cache[key] = cues
                cues
            } else {
                val fallback = parseFallbackCues(bodyFallback, durationLabel)
                if (fallback.isNotEmpty()) cache[key] = fallback
                fallback
            }
        } catch (_: Exception) {
            val fallback = parseFallbackCues(bodyFallback, durationLabel)
            if (fallback.isNotEmpty()) cache[key] = fallback
            fallback
        }
    }

    fun parseFallbackCues(body: String, durationLabel: String?): List<SubtitleCueDto> {
        val clean = body
            .replace(Regex("^###[^\n\r]+", RegexOption.MULTILINE), "")
            .replace(Regex("-\\s*\\*\\*Audio\\*\\*:[^\n\r]+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("-\\s*\\*\\*[^*]+:[^\n\r]+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
            .replace(Regex("\\*([^*]+)\\*"), "$1")
            .trim()

        val sentences = clean.split(Regex("(?<=[.!?])\\s+|\n+"))
            .map { it.trim().trimStart('-', '•', ' ').trim() }
            .filter { it.isNotBlank() && it.length > 3 }

        if (sentences.isEmpty()) return emptyList()

        val totalMs = parseMinSecPlusMs(durationLabel ?: "3:00").coerceAtLeast(60_000L)
        val perSentenceMs = totalMs / sentences.size
        return sentences.mapIndexed { idx, text ->
            SubtitleCueDto(
                startMs = idx * perSentenceMs,
                endMs = (idx + 1) * perSentenceMs,
                text = text
            )
        }
    }

    private fun parseMinSecPlusMs(label: String): Long {
        val parts = label.split(":")
        val minutes = parts.getOrNull(parts.lastIndex - 1)?.toLongOrNull() ?: return 0
        val seconds = parts.lastOrNull()?.toLongOrNull() ?: return 0
        return (minutes * 60 + seconds) * 1000
    }
}
