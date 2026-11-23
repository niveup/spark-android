package com.spark.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioPreloadLogicTest {

    private val allowedTitles = listOf(
        "the 48 laws of power",
        "the laws of human nature",
        "the art of war",
        "atomic habits",
        "what is life",
        "why i am an atheist"
    )

    private fun isAllowedBook(title: String): Boolean {
        val norm = title.lowercase().replace(Regex("[^a-z0-9]"), " ").replace(Regex("\\s+"), " ").trim()
        return allowedTitles.any { allowed ->
            val normAllowed = allowed.lowercase().replace(Regex("[^a-z0-9]"), " ").replace(Regex("\\s+"), " ").trim()
            norm.contains(normAllowed) || normAllowed.contains(norm)
        }
    }

    private fun computeUpcomingChapters(
        playlist: List<String>,
        currentIndex: Int
    ): List<String> {
        if (playlist.isEmpty() || currentIndex !in playlist.indices) return emptyList()
        return playlist.drop(currentIndex + 1).take(2)
    }

    @Test
    fun `preloads exactly next 2 chapters when at chapter 1`() {
        val playlist = listOf("ch1", "ch2", "ch3", "ch4", "ch5")
        val next = computeUpcomingChapters(playlist, currentIndex = 0)
        assertEquals(listOf("ch2", "ch3"), next)
    }

    @Test
    fun `preloads exactly next 2 chapters when at middle chapter`() {
        val playlist = listOf("ch1", "ch2", "ch3", "ch4", "ch5")
        val next = computeUpcomingChapters(playlist, currentIndex = 2)
        assertEquals(listOf("ch4", "ch5"), next)
    }

    @Test
    fun `preloads 1 chapter when at penultimate chapter`() {
        val playlist = listOf("ch1", "ch2", "ch3", "ch4", "ch5")
        val next = computeUpcomingChapters(playlist, currentIndex = 3)
        assertEquals(listOf("ch5"), next)
    }

    @Test
    fun `preloads 0 chapters when at final chapter`() {
        val playlist = listOf("ch1", "ch2", "ch3", "ch4", "ch5")
        val next = computeUpcomingChapters(playlist, currentIndex = 4)
        assertTrue(next.isEmpty())
    }

    @Test
    fun `allowed books filter allows only the 6 specified books`() {
        assertTrue(isAllowedBook("The 48 Laws of Power"))
        assertTrue(isAllowedBook("The Laws of Human Nature"))
        assertTrue(isAllowedBook("The Art of War"))
        assertTrue(isAllowedBook("Atomic Habits"))
        assertTrue(isAllowedBook("What Is Life?"))
        assertTrue(isAllowedBook("Why I Am an Atheist"))

        assertFalse(isAllowedBook("The Prince"))
        assertFalse(isAllowedBook("Frankenstein"))
        assertFalse(isAllowedBook("Pride and Prejudice"))
        assertFalse(isAllowedBook("The 33 Strategies of War"))
        assertFalse(isAllowedBook("Jane Eyre: An Autobiography"))
    }
}
