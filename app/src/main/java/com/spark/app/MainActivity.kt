package com.spark.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.spark.app.data.TokenStore
import com.spark.app.data.remote.buildSparkApi
import com.spark.app.feed.DoneScreen
import com.spark.app.home.AppRoot
import com.spark.app.ui.theme.SparkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val api = buildSparkApi()
        val tokens = TokenStore(this)
        setContent {
            SparkTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(api = api, tokens = tokens, onCloseApp = { finish() })
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DoneScreenPreview() {
    SparkTheme {
        DoneScreen(seenCount = 20, savedCards = emptyList(), onCloseApp = {})
    }
}
