package com.spark.app.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spark.app.data.Card

@Composable
fun DoneScreen(
    seenCount: Int,
    savedCards: List<Card>,
    onCloseApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaved by remember { mutableStateOf(false) }
    var actionText by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "You're done.", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$seenCount cards · ${savedCards.size} saved",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { showSaved = !showSaved }, modifier = Modifier.fillMaxWidth()) {
            Text("Review saved")
        }
        if (showSaved) {
            Spacer(modifier = Modifier.height(8.dp))
            if (savedCards.isEmpty()) {
                Text("Nothing saved this session.")
            } else {
                savedCards.forEach { card ->
                    Text("● ${card.title}", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                actionText = savedCards.firstOrNull()?.actionLine
                    ?: "Pick any card above and do its action line."
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Do the action now")
        }
        if (actionText != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = actionText ?: "")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onCloseApp, modifier = Modifier.fillMaxWidth()) {
            Text("Close app")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("No more cards load.", style = MaterialTheme.typography.bodySmall)
    }
}
