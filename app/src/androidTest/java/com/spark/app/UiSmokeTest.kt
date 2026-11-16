package com.spark.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
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
import com.spark.app.feed.FeedUiState
import com.spark.app.feed.DeckRefreshState
import com.spark.app.home.HomeScreen
import com.spark.app.home.MeScreen
import com.spark.app.home.SavedScreen
import com.spark.app.home.SavedUiState
import com.spark.app.home.StatsUiState
import com.spark.app.ui.theme.SparkTheme
import androidx.compose.ui.unit.dp
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
        var deckRestarted = false
        compose.setContent {
            SparkTheme {
                DoneScreen(
                    reason = FinishReason.CARD_LIMIT,
                    seenCount = 20,
                    savedCards = emptyList(),
                    onCloseApp = { closed = true },
                    onReadAgain = { deckRestarted = true }
                )
            }
        }

        compose.onNodeWithText("You're done.").assertIsDisplayed()
        compose.onNodeWithText("You hit your planned reading limit. Clean stop.").assertIsDisplayed()
        compose.onNodeWithText("Read this deck again")
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertTrue(deckRestarted)
        compose.onNodeWithText("Close app").performClick()
        assertTrue(closed)
    }

    @Test
    fun doneScreenEmptySavedStateShowsZeroCount() {
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

        compose.onNodeWithText("0 saved").assertIsDisplayed()
        compose.onNodeWithText("Read this deck again").assertIsDisplayed()
        compose.onNodeWithText("Close app").assertIsDisplayed()
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
        var openedReels = false
        compose.setContent {
            SparkTheme {
                SavedScreen(
                    state = SavedUiState.Content(emptyList(), ""),
                    query = "",
                    onQueryChange = {},
                    onRetry = {},
                    onExport = {},
                    onOpenReels = { openedReels = true }
                )
            }
        }

        compose.onNodeWithContentDescription("Nothing saved yet. Tap ♡ Save on any card in Reels.")
            .assertIsDisplayed()
        compose.onNodeWithText("Browse Reels").assertIsDisplayed().performClick()
        assertTrue(openedReels)
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

        compose.onNodeWithText("Try again").performClick()
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

        compose.onNodeWithContentDescription("Export Markdown").performClick()
        assertEquals(SavedExportFormat.MARKDOWN, requested)
        compose.onNodeWithContentDescription("Export JSON").performClick()
        assertEquals(SavedExportFormat.JSON, requested)
    }

    @Test
    fun savedCardOpensInReelsAndPreviewKeepsItHere() {
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
        var opened: Card? = null
        compose.setContent {
            SparkTheme {
                SavedScreen(
                    state = SavedUiState.Content(listOf(card), ""),
                    query = "",
                    onQueryChange = {},
                    onRetry = {},
                    onExport = {},
                    onOpenCard = { opened = it }
                )
            }
        }

        // Tapping a saved card opens that card in Reels.
        compose.onNodeWithContentDescription("Open in Reels: Start small").performClick()
        assertEquals("card-1", opened?.id)

        // The body is only in the expanded preview, not in the collapsed row.
        compose.onNodeWithText("Make the habit obvious.", substring = true).assertDoesNotExist()

        // Preview keeps the card on this screen instead of opening Reels.
        compose.onNodeWithContentDescription("Preview this card here").performClick()
        compose.onNodeWithText("Make the habit obvious.", substring = true).assertIsDisplayed()
        assertEquals("card-1", opened?.id)
    }

    @Test
    fun savedCardShowsOpeningWhileItIsBeingResolved() {
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
        compose.setContent {
            SparkTheme {
                SavedScreen(
                    state = SavedUiState.Content(listOf(card), ""),
                    query = "",
                    onQueryChange = {},
                    onRetry = {},
                    onExport = {},
                    openingItemId = "card-1"
                )
            }
        }

        compose.onNodeWithText("Opening…").assertIsDisplayed()
    }

    @Test
    fun homePeekOpensAndClosesAndPrimaryActionOpensFeed() {
        val card = Card(
            id = "home-preview-card",
            type = CardType.CONCEPT,
            topic = "habits",
            title = "A small habit for tomorrow",
            body = "A small useful thought.",
            actionLine = "Try this today: prepare one thing before bed.",
            source = "OpenStax",
            grounded = true,
            bookLine = "Building better habits"
        )
        var openedFeed = false
        var connectionChecked = false
        compose.setContent {
            SparkTheme {
                HomeScreen(
                    seenCount = 2,
                    savedCount = 1,
                    cardLimit = 20,
                    minuteLimit = 15,
                    offline = true,
                    onRetryConnection = { connectionChecked = true },
                    deckSize = 20,
                    nextCard = card,
                    onOpenReels = { openedFeed = true },
                    onOpenSaved = {}
                )
            }
        }

        compose.onNodeWithText("Peek inside").assertIsDisplayed().performClick()
        compose.onNodeWithContentDescription("Offline. Tap to retry Home and check the connection.")
            .assertIsDisplayed()
            .performClick()
        assertTrue(connectionChecked)
        compose.onNodeWithText("Showing cards stored on this phone").assertDoesNotExist()
        compose.onNodeWithContentDescription("Your shelf").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("Continue session").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithContentDescription("Hide peek").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithText("A small useful thought.", substring = true).assertExists()
        compose.onNodeWithText("Hide peek").assertIsDisplayed().performClick()
        compose.onNodeWithText("Peek inside").assertIsDisplayed()
        compose.onNodeWithText("Continue session").performClick()
        assertTrue(openedFeed)
        compose.onNodeWithText("One minute, one idea").assertDoesNotExist()
    }

    @Test
    fun meScreenShowsStatsAccessibleSteppersAndLogout() {
        var cardLimitDelta = 0
        var deckRefreshed = false
        var connectionChecked = false
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
                    deckRefreshState = DeckRefreshState.Refreshed(cardsReady = 20),
                    offline = true,
                    onRetryConnection = { connectionChecked = true },
                    cardLimit = 20,
                    minuteLimit = 15,
                    pendingSync = 0,
                    onCardLimitDelta = { cardLimitDelta += it },
                    onMinuteLimitDelta = {},
                    onRetryStats = {},
                    cachedCount = 7,
                    onClearCache = {},
                    deckState = FeedUiState.Content(
                        cards = emptyList(),
                        cardLimit = 20,
                        minuteLimit = 15,
                        startedAtElapsedMs = 0L
                    ),
                    onRefreshDeck = { deckRefreshed = true },
                    onLogout = { loggedOut = true }
                )
            }
        }

        compose.onNodeWithText("Your space").assertIsDisplayed()
        compose.onNodeWithContentDescription("Offline. Tap to check the connection.")
            .assertIsDisplayed()
            .performClick()
        assertTrue(connectionChecked)
        compose.onNodeWithText("Diagnostics").assertDoesNotExist()
        compose.onNodeWithText("cards read").assertIsDisplayed()
        compose.onNodeWithText("12").assertIsDisplayed()
        compose.onNodeWithText("Saved").assertIsDisplayed()
        compose.onNodeWithText("Liked").assertIsDisplayed()
        compose.onNodeWithText("Sessions").assertIsDisplayed()
        compose.onNodeWithText("Quizzes due").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
        compose.onNodeWithContentDescription("Refresh your deck")
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        compose.onNodeWithText("Deck refreshed — 20 cards ready to read again").assertExists()
        assertTrue(deckRefreshed)
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(3)
        compose.onNodeWithContentDescription("Increase Cards").performClick()
        assertEquals(1, cardLimitDelta)
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(4)
        compose.onNodeWithText("Device & data").assertIsDisplayed()
        compose.onNodeWithText("Offline library").assertIsDisplayed()
        compose.onNodeWithText("All changes synced").assertIsDisplayed()
        compose.onNodeWithText("7 cards stored for offline reading").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(5)
        compose.onNodeWithText("Log out").performClick()
        assertTrue(loggedOut)
    }
}
