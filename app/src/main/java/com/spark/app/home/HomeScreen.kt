package com.spark.app.home

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime

@Composable
fun HomeScreen(
    seenCount: Int,
    savedCount: Int,
    onOpenReels: () -> Unit,
    onOpenSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        val greeting = remember {
            when (LocalTime.now().hour) {
                in 5..11 -> "Morning."
                in 12..16 -> "Afternoon."
                in 17..21 -> "Evening."
                else -> "Late night."
            }
        }
        Text(text = greeting, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(text = "Pick up where you left.", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "TODAY",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("● Thermo — 5 due")
        Text("● SOM — 3 due")
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onOpenReels, modifier = Modifier.fillMaxWidth()) {
            Text("Open Reels")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = onOpenSaved, modifier = Modifier.fillMaxWidth()) {
            Text("Review saved ($savedCount)")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Seen $seenCount · Saved $savedCount",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
