package com.spark.app

import com.spark.app.data.remote.DataResponse
import com.spark.app.data.remote.ErrorResponse
import com.spark.app.data.remote.FeedItemDto
import com.spark.app.data.remote.FeedPage
import com.spark.app.data.remote.LoginData
import com.spark.app.data.remote.sparkJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DtoParsingTest {
    @Test
    fun `feed item parses with quiz and book`() {
        val raw = """
        {"id":"a1","type":"quiz","title":"Q","bodyMd":"B","actionLine":"",
         "topicTags":["thermo"],"language":"en","estSeconds":45,
         "quiz":{"options":["x","y"],"correctIndex":1,"explanation":"why"},
         "book":{"id":"b1","title":"T","author":"A","coverUrl":null},
         "sourceType":"manual","sourceRef":"deck","license":"original",
         "isAiGenerated":false,"isGrounded":true,"publishedAt":"2026-10-15T00:00:00Z"}
        """.trimIndent()
        val item = sparkJson.decodeFromString<FeedItemDto>(raw)
        assertEquals("a1", item.id)
        assertEquals(2, item.quiz?.options?.size)
        assertEquals(1, item.quiz?.correctIndex)
        assertEquals("T", item.book?.title)
    }

    @Test
    fun `unknown server fields are ignored`() {
        val raw = """{"id":"a1","type":"quote","title":"T","bodyMd":"B",
            "futureField":{"nested":true},"anotherOne":42}"""
        val item = sparkJson.decodeFromString<FeedItemDto>(raw)
        assertEquals("quote", item.type)
    }

    @Test
    fun `login envelope and error envelope parse`() {
        val ok = sparkJson.decodeFromString<DataResponse<LoginData>>(
            """{"data":{"token":"abc","expiresAt":"2027-01-01T00:00:00Z"}}"""
        )
        assertEquals("abc", ok.data.token)
        val err = sparkJson.decodeFromString<ErrorResponse>(
            """{"error":{"code":"UNAUTHORIZED","message":"Wrong passcode."}}"""
        )
        assertEquals("UNAUTHORIZED", err.error.code)
    }

    @Test
    fun `feed page parses with null cursor`() {
        val page = sparkJson.decodeFromString<FeedPage>(
            """{"data":[],"nextCursor":null}"""
        )
        assertEquals(0, page.data.size)
        assertNull(page.nextCursor)
    }
}
