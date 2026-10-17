// Phase 6: pure audio-time formatting used by the audiobook player UI.
package com.spark.app

import com.spark.app.audio.formatMinSec
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioTimeTest {

    @Test
    fun `formats minutes and seconds`() {
        assertEquals("0:00", formatMinSec(0))
        assertEquals("0:04", formatMinSec(4000)) // rounds 3.5s up
        assertEquals("2:05", formatMinSec(125_000))
        assertEquals("19:54", formatMinSec(1_194_000))
    }

    @Test
    fun `formats hours when over one hour`() {
        assertEquals("1:00:00", formatMinSec(3_600_000))
        assertEquals("1:31:09", formatMinSec(5_469_000))
    }

    @Test
    fun `negative values clamp to zero`() {
        assertEquals("0:00", formatMinSec(-1))
    }
}
