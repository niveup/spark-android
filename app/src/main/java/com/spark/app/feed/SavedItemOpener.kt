package com.spark.app.feed

import com.spark.app.data.Card
import com.spark.app.data.local.ItemCache
import com.spark.app.data.remote.SparkApi
import com.spark.app.sync.TokenReader
import kotlinx.coroutines.CancellationException

/**
 * Opens a saved card in the Reels deck.
 *
 * The Saved tab stores one key per card: the server id. Opening that card
 * maps the id back to the card:
 *
 *  - online: the id is fetched from the API and cached, so the card is
 *    readable offline afterwards too;
 *  - offline: the same id is looked up in this device's cache;
 *  - neither: the caller falls back to the offline deck.
 */
class SavedItemOpener(
    private val api: SparkApi,
    private val tokens: TokenReader,
    private val cache: ItemCache
) {

    /**
     * Resolves [itemId] to a card. [offlineOnly] skips the network entirely
     * (the phone already knows it has no connection).
     */
    suspend fun open(itemId: String, offlineOnly: Boolean = false): SavedItemResult {
        if (!offlineOnly) {
            val token = tokens.getToken()
            if (token != null) {
                val fetched = safeCall { api.item("Bearer $token", itemId).data }
                if (fetched != null) {
                    // Keep it on the device: the next offline open finds it here.
                    safeCall { cache.saveFeed(listOf(fetched)) }
                    val card = feedItemToCard(fetched)
                    if (card != null) return SavedItemResult.Found(card, fromCache = false)
                }
            }
        }
        val cached = safeCall { cache.itemById(itemId) }
        val card = cached?.let { feedItemToCard(it) }
        return if (card != null) {
            SavedItemResult.Found(card, fromCache = true)
        } else {
            SavedItemResult.Unavailable
        }
    }

    /** Network and cache failures must not crash the screen that opened the card. */
    private suspend fun <T> safeCall(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        null
    }
}

/** What the opener found for a saved id. */
sealed interface SavedItemResult {

    /** The card to show, and where it came from. */
    data class Found(val card: Card, val fromCache: Boolean) : SavedItemResult

    /**
     * The card is not on this device and the server could not be reached —
     * the caller shows the offline deck instead.
     */
    data object Unavailable : SavedItemResult
}
