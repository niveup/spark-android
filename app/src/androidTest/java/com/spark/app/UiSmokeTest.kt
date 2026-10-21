package com.spark.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.spark.app.auth.AuthStatus
import com.spark.app.auth.AuthUiState
import com.spark.app.auth.LoginScreen
import com.spark.app.data.Card
import com.spark.app.data.CardType
import com.spark.app.data.SavedExportFormat
import com.spark.app.data.remote.StatsData
import com.spark.app.feed.DoneScreen
import com.spark.app.feed.FinishReason
import com.spark.app.home.MeScreen
import com.spark.app.home.SavedScreen
import com.spark.app.home.SavedUiState
import com.spark.app.home.StatsUiState
import com.spark.app.ui.theme.SparkTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 8 on-device Compose smoke tests. These are intentionally stateless:
 * they exercise the screens the owner touches every day without needing the
 * API, a token, or the Room cache. Run with:
 *   .\gradlew.bat :app:connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class UiSmokeTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun doneScreenShowsLimitAndCloseCallback() {
        var closed = false
        compose.setContent {
            SparkTheme {
                DoneScreen(
                    reason = FinishReason.CARD_LIMIT,
                    seenCount = 20,
                    savedCards = emptyList(),
                    onCloseApp = { closed = true }
                )
            }
        }

        compose.onNodeWithText("You're done.").assertIsDisplayed()
        compose.onNodeWithText("You hit your card limit for this session.").assertIsDisplayed()
        compose.onNodeWithText("Close app").performClick()
        assertTrue(closed)
    }

    @Test
    fun doneScreenEmptySavedStateIsExplicit() {
        compose.setContent {
            SparkTheme {
                DoneScreen(
                    reason = FinishReason.TIME_LIMIT,
                    seenCount = 5,
                    savedCards = emptyList(),
                    onCloseApp = {}
                )
            }
        }

        compose.onNodeWithText("Review saved").performClick()
        compose.onNodeWithText("Nothing saved this session.").assertIsDisplayed()
    }

    @Test
    fun loginScreenForwardsTypedPasscode() {
        var submitted: String? = null
        compose.setContent {
            SparkTheme {
                LoginScreen(
                    state = AuthUiState(AuthStatus.LOGGED_OUT),
                    onLogin = { submitted = it }
                )
            }
        }

        compose.onNode(hasSetTextAction()).performTextInput("test-passcode")
        compose.onNodeWithText("Unlock").performClick()
        assertEquals("test-passcode", submitted)
    }

    @Test
    fun loginScreenShowsServerError() {
        compose.setContent {
            SparkTheme {
                LoginScreen(
                    state = AuthUiState(AuthStatus.LOGGED_OUT, "Wrong passcode."),
                    onLogin = {}
                )
            }
        }

        compose.onNodeWithText("Wrong passcode.").assertIsDisplayed()
    }

    @Test
    fun savedScreenEmptyStateExplainsHowToSave() {
        compose.setContent {
            SparkTheme {
                SavedScreen(
                    state = SavedUiState.Content(emptyList(), ""),
                    query = "",
                    onQueryChange = {},
                    onRetry = {},
                    onExport = {}
                )
            }
        }

        compose.onNodeWithText("Nothing saved yet. Tap ♡ Save on any card in Reels.")
            .assertIsDisplayed()
    }

    @Test
    fun savedScreenErrorStateRetries() {
        var retried = false
        compose.setContent {
            SparkTheme {
                SavedScreen(
                    state = SavedUiState.Error("Could not load saved cards."),
                    query = "",
                    onQueryChange = {},
                    onRetry = { retried = true },
                    onExport = {}
                )
            }
        }

        compose.onNodeWithText("Retry").performClick()
        assertTrue(retried)
    }

    @Test
    fun savedScreenExportsVisibleCardsInBothFormats() {
        val card = Card(
            id = "card-1",
            type = CardType.CONCEPT,
            topic = "habits",
            title = "Start small",
            body = "Make the habit obvious.",
            actionLine = "Do two minutes today.",
            source = "Test source",
            grounded = true
        )
        var requested: SavedExportFormat? = null
        compose.setContent {
            SparkTheme {
                SavedScreen(
                    state = SavedUiState.Content(listOf(card), ""),
                    query = "",
                    onQueryChange = {},
                    onRetry = {},
                    onExport = { requested = it }
                )
            }
        }

        compose.onNodeWithText("Export Markdown").performClick()
        assertEquals(SavedExportFormat.MARKDOWN, requested)
        compose.onNodeWithText("Export JSON").performClick()
        assertEquals(SavedExportFormat.JSON, requested)
    }

    @Test
    fun meScreenShowsStatsAccessibleSteppersAndLogout() {
        var loggedOut = false
        compose.setContent {
            SparkTheme {
                MeScreen(
                    stats = StatsUiState.Content(
                        StatsData(
                            seenTotal = 12,
                            savedTotal = 3,
                            likedTotal = 2,
                            quizzesDue = 1,
                            sessionsTotal = 4
                        )
                    ),
                    cardLimit = 20,
                    minuteLimit = 15,
                    pendingSync = 0,
                    onCardLimitDelta = {},
                    onMinuteLimitDelta = {},
                    onRetryStats = {},
                    cachedCount = 7,
                    onClearCache = {},
                    onLogout = { loggedOut = true }
                )
            }
        }

        compose.onNodeWithText("Me").assertIsDisplayed()
        compose.onNodeWithText("Cards seen").assertIsDisplayed()
        compose.onNodeWithText("12").assertIsDisplayed()
        compose.onNodeWithContentDescription("Increase Cards per session").assertExists()
        compose.onNodeWithText("All changes synced ✓").assertIsDisplayed()
        compose.onNodeWithText("Log out").performScrollTo().performClick()
        assertTrue(loggedOut)
    }
}
