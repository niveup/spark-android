package com.spark.app

import com.spark.app.data.remote.ApiErrors
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiErrorsTest {
    @Test
    fun `401 asks the user to log back in`() {
        assertEquals(ApiErrors.SESSION_EXPIRED, ApiErrors.forHttp(401))
    }

    @Test
    fun `other HTTP codes keep the code visible`() {
        assertEquals("Server error (500).", ApiErrors.forHttp(500))
    }
}
