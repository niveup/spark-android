package com.spark.app.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spark.app.BuildConfig
import com.spark.app.R
import com.spark.app.data.Card
import com.spark.app.data.SavedExportFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun SavedScreen(
    state: SavedUiState,
    query: String,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onExport: (SavedExportFormat) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Saved",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search saved cards…") }
        )
        // Keep the export row composed while loading/empty so it does not
        // appear and disappear during tab transitions; disabled until there is
        // content. The Material3 1.4.0 child-semantics crash itself is
        // worked around in CardAction (feed/CardViews.kt).
        val canExport = state is SavedUiState.Content && state.cards.isNotEmpty()
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { onExport(SavedExportFormat.JSON) },
                enabled = canExport,
                modifier = Modifier.weight(1f)
            ) { Text("Export JSON") }
            OutlinedButton(
                onClick = { onExport(SavedExportFormat.MARKDOWN) },
                enabled = canExport,
                modifier = Modifier.weight(1f)
            ) { Text("Export Markdown") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        when (state) {
            is SavedUiState.Loading -> Text("Loading saved cards…")
            is SavedUiState.Error -> {
                Text(state.message)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                    Text("Retry")
                }
            }
            is SavedUiState.Content -> {
                if (state.cards.isEmpty()) {
                    Text(
                        if (state.query.isBlank()) {
                            "Nothing saved yet. Tap ♡ Save on any card in Reels."
                        } else {
                            "No saved cards match \"${state.query}\"."
                        }
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.cards, key = { it.id }) { card -> SavedRow(card) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedRow(card: Card) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("● ${card.title}", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "${card.type.name.lowercase()} · ${card.topic}",
            style = MaterialTheme.typography.bodySmall
        )
        if (card.body.isNotBlank()) {
            Text(text = card.body, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        }
    }
}

@Composable
fun MeScreen(
    stats: StatsUiState,
    cardLimit: Int,
    minuteLimit: Int,
    pendingSync: Int,
    onCardLimitDelta: (Int) -> Unit,
    onMinuteLimitDelta: (Int) -> Unit,
    onRetryStats: () -> Unit,
    cachedCount: Int,
    onClearCache: () -> Unit,
    onLogout: () -> Unit,
    deckCount: Int?,
    onRefreshDeck: () -> Unit,
    modifier: Modifier = Modifier,
    offline: Boolean = false
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var cacheCleared by remember { mutableStateOf(false) }
    var lastRefresh by remember { mutableStateOf<LocalTime?>(null) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Me",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { heading() }
            )
            Icon(
                painter = painterResource(
                    id = if (offline) R.drawable.ic_wifi_off_24 else R.drawable.ic_wifi_24
                ),
                contentDescription = if (offline) "Offline" else "Online",
                tint = if (offline) Color(0xFFE57373) else Color(0xFF4CAF50),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "SPARK · my personal feed", style = MaterialTheme.typography.bodyLarge)

        Spacer(modifier = Modifier.height(16.dp))
        SectionLabel("MY STATS")
        when (stats) {
            is StatsUiState.Loading -> Text("Loading stats…")
            is StatsUiState.Error -> {
                Text(stats.message)
                TextButton(onClick = onRetryStats) { Text("Retry") }
            }
            is StatsUiState.Content -> {
                StatRow("Cards seen", stats.stats.seenTotal)
                StatRow("Saved", stats.stats.savedTotal)
                StatRow("Liked", stats.stats.likedTotal)
                StatRow("Quizzes due", stats.stats.quizzesDue)
                StatRow("Sessions", stats.stats.sessionsTotal)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionLabel("SESSION LIMITS")
        StepperRow("Cards per session", cardLimit, onCardLimitDelta)
        StepperRow("Minutes per session", minuteLimit, onMinuteLimitDelta)
        Text("Applies from your next session.", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(16.dp))
        SectionLabel("DECK")
        Text(
            when {
                deckCount == null -> "Fetching today's cards…"
                deckCount == 0 -> "No cards loaded. Refresh to fetch today's deck."
                deckCount == 1 -> "1 card ready to read"
                else -> "$deckCount cards ready to read"
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                lastRefresh = LocalTime.now()
                onRefreshDeck()
            },
            enabled = deckCount != null
        ) {
            Text("Refresh deck")
        }
        Text(
            "Pulls a fresh deck from the server, so a session never runs dry.",
            style = MaterialTheme.typography.bodySmall
        )
        lastRefresh?.let { time ->
            Text(
                "Last refresh ${time.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionLabel("SYNC")
        Text(
            if (pendingSync > 0) "$pendingSync change(s) waiting to upload"
            else "All changes synced ✓"
        )

        Spacer(modifier = Modifier.height(16.dp))
        SectionLabel("OFFLINE CACHE")
        Text(
            if (cachedCount > 0) "$cachedCount card(s) on this phone for offline reading"
            else "Nothing cached yet — open the feed once while online"
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                showClearDialog = true
                cacheCleared = false
            },
            enabled = cachedCount > 0
        ) {
            Text("Clear offline cache")
        }
        if (cacheCleared) Text("Offline cache cleared ✓")
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("Clear offline cache?") },
                text = {
                    Text(
                        "Deletes the cards stored on this phone for offline reading. " +
                            "Nothing on the server is touched."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showClearDialog = false
                        onClearCache()
                        cacheCleared = true
                    }) { Text("Clear") }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) { Text("Keep") }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        SectionLabel("GOALS")
        Text("● Build better habits, one card a day")
        Text("● Read public-domain books, one card a day")
        Text("● Scroll less, do more")

        // Phase 8 diagnostics: only in debug builds. Crashes the app on
        // purpose so we can prove the Sentry pipeline works end to end
        // (event appears in Sentry within seconds).
        if (BuildConfig.DEBUG) {
            Spacer(modifier = Modifier.height(16.dp))
            SectionLabel("DIAGNOSTICS")
            OutlinedButton(
                onClick = {
                    throw IllegalStateException("SPARK test crash (debug build only)")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send test crash to Sentry")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text("Log out")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("SPARK v0.1.0 · debug", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun StatRow(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text("$value", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StepperRow(label: String, value: Int, onDelta: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(
                onClick = { onDelta(-1) },
                modifier = Modifier.semantics { contentDescription = "Decrease $label" }
            ) { Text("-", fontSize = 20.sp) }
            Text(
                text = "$value",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
            TextButton(
                onClick = { onDelta(+1) },
                modifier = Modifier.semantics { contentDescription = "Increase $label" }
            ) { Text("+", fontSize = 20.sp) }
        }
    }
}
