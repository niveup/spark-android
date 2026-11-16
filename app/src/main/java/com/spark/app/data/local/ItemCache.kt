package com.spark.app.data.local

import com.spark.app.data.remote.FeedItemDto

/**
 * The slice of the offline store the Reels and Saved screens need.
 *
 * [OfflineCache] implements it over Room; unit tests fake it (Room needs an
 * Android context). The app treats the server as the source of truth and this
 * store as the offline copy of the last thing the server said.
 */
interface ItemCache {

    /** The cached deck, newest batch first. */
    suspend fun feedDtos(): List<FeedItemDto>

    /** Saved cards only (however they got flagged — feed or the saved list). */
    suspend fun savedDtos(): List<FeedItemDto>

    /**
     * One cached card by its server id, or null when this device does not
     * hold it. This is the offline half of "open a saved card in Reels":
     * the same id the Saved tab stored is looked up here.
     */
    suspend fun itemById(id: String): FeedItemDto?

    /** Cache a batch of cards (server wins: the batch overwrites older rows). */
    suspend fun saveFeed(items: List<FeedItemDto>)
}
