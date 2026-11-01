package com.spark.app.data.remote

/**
 * Single source of truth for the failure copy shown by every screen.
 *
 * The feed, saved and stats ViewModels all talk to the same API, so they must
 * say the same thing when it fails. Keeping the strings here stops the three
 * copies from drifting apart (they already had two different 401 wordings).
 */
object ApiErrors {
    const val NOT_LOGGED_IN = "Not logged in — log in again."
    const val OFFLINE =
        "Can't reach your PC. Check the USB cable and that the API server is running."
    const val SESSION_EXPIRED = "Session expired — log out from Me and log back in."

    /** Message for an HTTP failure; 401 means the token is no longer valid. */
    fun forHttp(code: Int): String =
        if (code == 401) SESSION_EXPIRED else "Server error ($code)."
}
