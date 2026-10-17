package com.spark.app

import com.spark.app.data.SESSION_CARD_LIMIT
import com.spark.app.data.SettingsStore
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsStoreTest {

    @Test
    fun `card limit clamps to 5 through 100`() {
        assertEquals(5, SettingsStore.clampCards(0))
        assertEquals(5, SettingsStore.clampCards(5))
        assertEquals(20, SettingsStore.clampCards(20))
        assertEquals(100, SettingsStore.clampCards(500))
    }

    @Test
    fun `minute limit clamps to 5 through 120`() {
        assertEquals(5, SettingsStore.clampMinutes(1))
        assertEquals(15, SettingsStore.clampMinutes(15))
        assertEquals(120, SettingsStore.clampMinutes(999))
    }

    @Test
    fun `defaults match the phase brief (20 cards, 15 minutes)`() {
        assertEquals(20, SettingsStore.DEFAULT_CARD_LIMIT)
        assertEquals(15, SettingsStore.DEFAULT_MINUTE_LIMIT)
        assertEquals(20, SESSION_CARD_LIMIT)
    }
}
