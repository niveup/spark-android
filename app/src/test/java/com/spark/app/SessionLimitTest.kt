package com.spark.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SessionLimitTest {
    @Test
    fun `default session is 20 cards`() {
        assertEquals(20, Phase0Config.DEFAULT_SESSION_CARDS)
    }
}
