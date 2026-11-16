package com.spark.app

import com.spark.app.data.isValidatedInternet
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkStatusTest {
    @Test
    fun `internet capability alone does not mean network is usable`() {
        assertFalse(isValidatedInternet(hasInternet = true, isValidated = false))
    }

    @Test
    fun `validated network must also advertise internet`() {
        assertFalse(isValidatedInternet(hasInternet = false, isValidated = true))
    }

    @Test
    fun `usable default network has both capabilities`() {
        assertTrue(isValidatedInternet(hasInternet = true, isValidated = true))
    }
}
