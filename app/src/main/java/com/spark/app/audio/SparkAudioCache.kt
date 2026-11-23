package com.spark.app.audio

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/**
 * Temporary disk cache for book chapters.
 * Stored strictly in [Context.getCacheDir] with an LRU eviction limit (150 MB).
 * Never uses permanent external storage.
 */
@OptIn(UnstableApi::class)
object SparkAudioCache {

    private const val CACHE_DIR_NAME = "spark_audio_cache"
    private const val MAX_CACHE_SIZE_BYTES = 150L * 1024L * 1024L // 150 MB temp limit

    @Volatile
    private var simpleCache: SimpleCache? = null

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        val existing = simpleCache
        if (existing != null) {
            return existing
        }
        val cacheDir = File(context.applicationContext.cacheDir, CACHE_DIR_NAME)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val evictor = LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE_BYTES)
        val databaseProvider = StandaloneDatabaseProvider(context.applicationContext)
        val created = SimpleCache(cacheDir, evictor, databaseProvider)
        simpleCache = created
        return created
    }

    fun createDataSourceFactory(context: Context): CacheDataSource.Factory {
        val cache = getCache(context)
        val upstreamFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("SPARK-Audio")
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun isFullyCached(context: Context, url: String): Boolean {
        return try {
            val cache = getCache(context)
            val metadata = cache.getContentMetadata(url)
            val length = ContentMetadata.getContentLength(metadata)
            if (length <= 0) return false
            val cachedBytes = cache.getCachedBytes(url, 0L, length)
            cachedBytes >= length
        } catch (_: Exception) {
            false
        }
    }

    @Synchronized
    fun release() {
        try {
            simpleCache?.release()
        } catch (_: Exception) {}
        simpleCache = null
    }
}
