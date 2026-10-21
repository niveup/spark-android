package com.spark.app

import com.spark.app.data.Card
import com.spark.app.data.CardType
import com.spark.app.data.QuizData
import com.spark.app.data.SavedExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SavedExportTest {

    private val card = Card(
        id = "card-1",
        type = CardType.QUIZ,
        topic = "habits",
        title = "Start \"small\"",
        body = "Make the habit obvious.",
        actionLine = "Do two minutes today.",
        source = "Test source",
        grounded = true,
        bookLine = "A Book — An Author",
        quiz = QuizData(
            options = listOf("Option A", "Option B"),
            correctIndex = 1,
            explanation = "Because B."
        )
    )

    @Test
    fun jsonExportsAllCardFieldsAndEscapesQuotes() {
        val json = SavedExport.toJson(listOf(card))

        assertTrue(json.contains("\"id\": \"card-1\""))
        assertTrue(json.contains("\"title\": \"Start \\\"small\\\"\""))
        assertTrue(json.contains("\"book\": \"A Book — An Author\""))
        assertTrue(json.contains("\"correctIndex\": 1"))
        assertTrue(json.contains("\"grounded\": true"))
    }

    @Test
    fun markdownKeepsAttributionAndAction() {
        val markdown = SavedExport.toMarkdown(listOf(card), LocalDate.of(2026, 10, 21))

        assertTrue(markdown.contains("# SPARK saved cards"))
        assertTrue(markdown.contains("_Exported 2026-10-21 · 1 card(s)_"))
        assertTrue(markdown.contains("## Start \"small\""))
        assertTrue(markdown.contains("**Book:** A Book — An Author"))
        assertTrue(markdown.contains("> **Action:** Do two minutes today."))
        assertTrue(markdown.contains("Source: Test source"))
        assertTrue(markdown.contains("Grounded in the source text."))
    }

    @Test
    fun emptySavedListExportsValidEmptyJson() {
        assertEquals("[]", SavedExport.toJson(emptyList()).trim())
        assertTrue(SavedExport.toMarkdown(emptyList()).contains("0 card(s)"))
    }
}
