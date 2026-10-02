package com.spark.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spark.app.data.Card

@Composable
fun SavedScreen(savedCards: List<Card>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "Saved", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        if (savedCards.isEmpty()) {
            Text("Nothing saved yet. Tap ♡ Save on any card in Reels.")
        } else {
            savedCards.forEach { card ->
                Text("● ${card.title}", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${card.type.name.lowercase()} · ${card.topic}",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MeScreen(seenCount: Int, savedCount: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "Me", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "SPARK · my personal feed", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "GOALS",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text("● GATE Mechanical — master weak topics")
        Text("● Read public-domain books, one card a day")
        Text("● Scroll less, do more")
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "THIS SESSION",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text("Seen $seenCount · Saved $savedCount")
        Spacer(modifier = Modifier.height(16.dp))
        Text("SPARK v0.1.0 · debug", style = MaterialTheme.typography.bodySmall)
    }
}
