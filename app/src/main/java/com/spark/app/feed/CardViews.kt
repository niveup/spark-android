package com.spark.app.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spark.app.audio.TtsHelper
import com.spark.app.data.Card
import com.spark.app.data.CardType

@Composable
fun CardView(
    card: Card,
    counter: String,
    tts: TtsHelper,
    state: FeedState,
    showFinish: Boolean,
    modifier: Modifier = Modifier
) {
    var showWhy by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "[${card.type.name}·${card.topic}]",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = counter, style = MaterialTheme.typography.labelLarge)
            }
            Spacer(modifier = Modifier.height(16.dp))
            key(card.id) {
                when (card.type) {
                    CardType.QUOTE -> QuoteBody(card)
                    CardType.QUIZ -> QuizBody(card)
                    else -> StandardBody(card)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (card.actionLine.isNotBlank()) {
                Text(
                    text = "▸ ${card.actionLine}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (showFinish) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { state.finish() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Finish session")
                }
            }
        }
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { state.toggleSave(card.id) }) {
                    Text(if (state.saved.contains(card.id)) "♥ Saved" else "♡ Save")
                }
                TextButton(onClick = { state.toggleLike(card.id) }) {
                    Text(if (state.liked.contains(card.id)) "👍!" else "👍")
                }
                TextButton(onClick = { state.toggleDislike(card.id) }) {
                    Text(if (state.disliked.contains(card.id)) "👎!" else "👎")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = { tts.speak("${card.title}. ${card.body}") }) {
                    Text("🔊 Listen")
                }
                TextButton(onClick = { showWhy = true }) {
                    Text("ⓘ Why this?")
                }
            }
        }
    }
    if (showWhy) {
        AlertDialog(
            onDismissRequest = { showWhy = false },
            title = { Text("Why am I seeing this?") },
            text = {
                Column {
                    Text("Source: ${card.source}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (card.grounded) "[Grounded ✓]" else "[AI-written, unverified]"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Topic: ${card.topic}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showWhy = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun StandardBody(card: Card) {
    Text(text = card.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))
    Text(text = card.body, fontSize = 18.sp, lineHeight = 26.sp)
    if (card.durationLabel != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${card.durationLabel} · tap Listen to hear it",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun QuoteBody(card: Card) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = card.title, style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "\"${card.body}\"", fontSize = 26.sp, lineHeight = 34.sp)
    }
}

@Composable
private fun QuizBody(card: Card) {
    val quiz = card.quiz
    Text(text = card.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(12.dp))
    Text(text = card.body, fontSize = 18.sp)
    Spacer(modifier = Modifier.height(12.dp))
    if (quiz == null) {
        Text("Quiz data missing.")
        return
    }
    var selected by remember { mutableStateOf<Int?>(null) }
    quiz.options.forEachIndexed { index, option ->
        val label = ('A' + index).toString()
        OutlinedButton(
            onClick = { selected = index },
            modifier = Modifier.fillMaxWidth()
        ) {
            val mark = when {
                selected == null -> ""
                index == quiz.correctIndex -> " ✓"
                index == selected -> " ✗"
                else -> ""
            }
            Text("$label) $option$mark")
        }
    }
    if (selected != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = quiz.explanation, style = MaterialTheme.typography.bodyMedium)
    }
}
