package com.spark.app.audio

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheWriter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pre-downloads the next upcoming book chapters (e.g. next 2 chapters) into temporary cache.
 * Downloads sequentially on [Dispatchers.IO] so it does not compete with the currently
 * playing chapter's network stream.
 */
@OptIn(UnstableApi::class)
class AudioPreloadManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var preloadJob: Job? = null

    @Volatile
    private var activeWriter: CacheWriter? = null

    @Volatile
    private var currentPreloadUrl: String? = null

    /**
     * Schedules the pre-download of [urls] (e.g. the next 2 chapters) into temp cache.
     * Cancels any previously active preloading.
     */
    fun preloadNextChapters(urls: List<String>) {
        cancelAll()
        if (urls.isEmpty()) return

        preloadJob = scope.launch {
            // Give the active chapter 1.5s to start streaming and buffer its first chunks smoothly
            delay(1500L)
            if (!isActive) return@launch

            for (url in urls) {
                if (!isActive) break
                try {
                    preloadUrl(url)
                } catch (e: CancellationException) {
                    break
                } catch (_: Exception) {
                    // Network hiccup or partial failure: continue to next chapter
                }
            }
        }
    }

    private suspend fun preloadUrl(url: String) {
        if (url.isBlank()) return
        if (SparkAudioCache.isFullyCached(context, url)) {
            return
        }

        withContext(Dispatchers.IO) {
            val cacheDataSource = SparkAudioCache.createDataSourceFactory(context).createDataSource()
            val dataSpec = DataSpec.Builder()
                .setUri(Uri.parse(url))
                .build()

            val writer = CacheWriter(
                cacheDataSource,
                dataSpec,
                null,
                null
            )
            activeWriter = writer
            currentPreloadUrl = url

            try {
                if (!isActive) {
                    writer.cancel()
                    return@withContext
                }
                writer.cache()
            } catch (e: Exception) {
                if (!isActive || e is InterruptedException) {
                    throw CancellationException("Preload cancelled for $url")
                }
            } finally {
                if (currentPreloadUrl == url) {
                    activeWriter = null
                    currentPreloadUrl = null
                }
            }
        }
    }

    /**
     * Called when a chapter starts playing. If that chapter is currently being
     * preloaded, immediately cancel the preloader so ExoPlayer gets uninterrupted access.
     */
    fun onPlayingUrl(url: String) {
        if (currentPreloadUrl == url) {
            cancelAll()
        }
    }

    fun cancelAll() {
        try {
            activeWriter?.cancel()
        } catch (_: Exception) {}
        activeWriter = null
        currentPreloadUrl = null
        preloadJob?.cancel()
        preloadJob = null
    }

    fun release() {
        cancelAll()
        scope.cancel()
    }
}
