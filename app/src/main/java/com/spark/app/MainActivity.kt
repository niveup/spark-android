package com.spark.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.spark.app.data.TokenStore
import com.spark.app.data.remote.buildSparkApi
import com.spark.app.feed.DoneScreen
import com.spark.app.feed.FinishReason
import com.spark.app.home.AppRoot
import com.spark.app.ui.theme.SparkTheme

/** Action sent by the "I'm bored" app shortcut (res/xml/shortcuts.xml). */
const val ACTION_OPEN_REELS = "com.spark.app.action.OPEN_REELS"

class MainActivity : ComponentActivity() {
    private var pendingOpenReels by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        pendingOpenReels = intent?.action == ACTION_OPEN_REELS
        clearTransientAudioCache()
        val api = buildSparkApi()
        val tokens = TokenStore(this)
        setContent {
            SparkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(
                        api = api,
                        tokens = tokens,
                        onCloseApp = { finish() },
                        startOnReels = pendingOpenReels,
                        onStartOnReelsHandled = {
                            pendingOpenReels = false
                            intent?.action = null // consumed: rotation must not re-open Reels
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingOpenReels = intent.action == ACTION_OPEN_REELS
    }

    override fun onStart() {
        super.onStart()
        clearTransientAudioCache()
    }

    override fun onStop() {
        super.onStop()
        clearTransientAudioCache()
    }

    override fun onDestroy() {
        clearTransientAudioCache()
        super.onDestroy()
    }

    /**
     * Ensures zero audio cache is ever retained on the device when the phone
     * closes, screen turns off, or app is minimized/closed.
     */
    private fun clearTransientAudioCache() {
        try {
            listOf(cacheDir, codeCacheDir, externalCacheDir).filterNotNull().forEach { dir ->
                dir.walkTopDown().forEach { file ->
                    if (file.isFile && (
                        file.extension.lowercase() in listOf("mp3", "m4a", "webm", "ogg", "wav", "aac") ||
                        file.name.contains("media", ignoreCase = true) ||
                        file.name.contains("exoplayer", ignoreCase = true)
                    )) {
                        file.delete()
                    }
                }
            }
        } catch (_: Exception) {}
    }

}

@Preview(showBackground = true)
@Composable
fun DoneScreenPreview() {
    SparkTheme {
        DoneScreen(
            reason = FinishReason.CARD_LIMIT,
            seenCount = 20,
            savedCards = emptyList(),
            onCloseApp = {}
        )
    }
}
