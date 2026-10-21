package com.spark.app.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** The two portable formats the Saved tab can export (Phase 8). */
enum class SavedExportFormat(val extension: String, val mimeType: String) {
    JSON("json", "application/json"),
    MARKDOWN("md", "text/markdown")
}

/** Pure formatting so the export shape is unit-tested without Android. */
object SavedExport {
    private val prettyJson = Json { prettyPrint = true }

    fun toJson(cards: List<Card>): String {
        val array = JsonArray(
            cards.map { card ->
                buildJsonObject {
                    put("id", card.id)
                    put("type", card.type.name.lowercase())
                    put("topic", card.topic)
                    put("title", card.title)
                    put("body", card.body)
                    put("actionLine", card.actionLine)
                    put("source", card.source)
                    put("grounded", card.grounded)
                    put("aiGenerated", card.aiGenerated)
                    card.bookLine?.let { put("book", it) }
                    card.audioUrl?.let { put("audioUrl", it) }
                    card.quiz?.let { quiz ->
                        put(
                            "quiz",
                            buildJsonObject {
                                put("options", buildJsonArray { quiz.options.forEach { add(it) } })
                                put("correctIndex", quiz.correctIndex)
                                put("explanation", quiz.explanation)
                            }
                        )
                    }
                }
            }
        )
        return prettyJson.encodeToString(JsonArray.serializer(), array)
    }

    fun toMarkdown(cards: List<Card>, exportedOn: LocalDate = LocalDate.now()): String =
        buildString {
            appendLine("# SPARK saved cards")
            appendLine()
            appendLine("_Exported $exportedOn · ${cards.size} card(s)_")
            cards.forEach { card ->
                appendLine()
                appendLine("## ${card.title}")
                appendLine()
                appendLine("**Type:** ${card.type.name.lowercase()} · **Topic:** ${card.topic}")
                card.bookLine?.let { appendLine("**Book:** $it") }
                appendLine()
                appendLine(card.body)
                if (card.actionLine.isNotBlank()) {
                    appendLine()
                    appendLine("> **Action:** ${card.actionLine}")
                }
                appendLine()
                appendLine("Source: ${card.source}")
                appendLine(if (card.grounded) "Grounded in the source text." else "AI-written, unverified.")
            }
        }
}

/** Writes the export into the app cache and opens Android's share sheet. */
object SavedExporter {
    fun share(context: Context, cards: List<Card>, format: SavedExportFormat) {
        if (cards.isEmpty()) return
        try {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            val file = File(dir, "spark-saved-$stamp.${format.extension}")
            file.writeText(
                when (format) {
                    SavedExportFormat.JSON -> SavedExport.toJson(cards)
                    SavedExportFormat.MARKDOWN -> SavedExport.toMarkdown(cards)
                }
            )
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val send = Intent(Intent.ACTION_SEND).apply {
                type = format.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(send, "Export saved cards")
            if (context !is Activity) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) {
            Toast.makeText(
                context,
                "Couldn't export saved cards. Try again.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
