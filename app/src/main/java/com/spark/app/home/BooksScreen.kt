package com.spark.app.home

import androidx.activity.compose.BackHandler

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spark.app.audio.MiniAudioPlayer
import com.spark.app.audio.extractDominantColor
import com.spark.app.audio.generateFallbackColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import coil.imageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.spark.app.R
import com.spark.app.audio.AudioPlayerBox
import com.spark.app.data.remote.BookChapterDto
import com.spark.app.data.remote.BookDto
import com.spark.app.ui.EmptyState
import com.spark.app.ui.ErrorState
import com.spark.app.ui.LoadingState
import com.spark.app.ui.OfflineConnectivityButton
import com.spark.app.ui.theme.SparkSpacing
import com.spark.app.ui.theme.SparkType

private val Page = Color(0xFF121110)
private val Paper = Color(0xFFECE6E0)
private val PaperDeep = Color(0xFF242220)
private val CardBg = Color(0xFF1A1817)
private val ReadingSurface = Color(0xFF211B15)
private val Ink = Color(0xFFECE6E0)
private val InkSoft = Color(0xFFCFC7BE)
private val Line = Color(0xFF383431)
private val Ember = Color(0xFFFFB454)
private val Forest = Color(0xFF214A36)
private val SageSoft = Color(0xFF1E4F42)
private val SageAccent = Color(0xFFA6D8C4)

private val Display = FontFamily.Serif

@Composable
fun BooksScreen(
    state: BooksUiState,
    onRetry: () -> Unit,
    offline: Boolean,
    onRetryConnection: () -> Unit,
    onOpenReels: () -> Unit,
    onOpenChapterInReels: (String) -> Unit = {},
    onOpenMe: () -> Unit = {},
    player: AudioPlayerBox? = null,
    onOpenAudioPlayer: () -> Unit = {},
    bottomClearance: Dp = 100.dp
) {
    var selectedBook by remember { mutableStateOf<BookDto?>(null) }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }

    Box(modifier = Modifier.fillMaxSize().background(Page)) {
        when (state) {
            is BooksUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingState("Loading curated library…")
                }
            }
            is BooksUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorState(
                        text = state.message,
                        onRetry = onRetry
                    )
                }
            }
            is BooksUiState.Content -> {
                if (state.books.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        EmptyState(
                            text = "No books in library yet.",
                            actionLabel = "Retry",
                            onAction = onRetry
                        )
                    }
                } else {
                    BooksContent(
                        books = state.books,
                        selectedFilter = selectedFilter,
                        onSelectFilter = { selectedFilter = it },
                        onSelectBook = { selectedBook = it },
                        offline = offline,
                        onRetryConnection = onRetryConnection,
                        bottomClearance = bottomClearance,
                        onOpenMe = onOpenMe
                    )
                }
            }
        }

        // Book Detail Modal Sheet (opens on tapping any book card)
        selectedBook?.let { book ->
            BookDetailDialog(
                book = book,
                onDismiss = { selectedBook = null },
                onOpenReels = {
                    selectedBook = null
                    onOpenReels()
                },
                onOpenChapterInReels = { chapterId ->
                    selectedBook = null
                    onOpenChapterInReels(chapterId)
                },
                player = player,
                onOpenAudioPlayer = onOpenAudioPlayer
            )
        }
    }
}

@Composable
private fun BooksContent(
    books: List<BookDto>,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    onSelectBook: (BookDto) -> Unit,
    offline: Boolean,
    onRetryConnection: () -> Unit,
    bottomClearance: Dp,
    onOpenMe: () -> Unit
) {
    val filters = listOf("All", "Audio", "Philosophy", "Self-Growth", "Classics")

    // Storytel-style categories
    val editorsChoiceBooks = remember(books) {
        val dynamicChoice = books.filter { it.isEditorsChoice }
        val list = if (dynamicChoice.isNotEmpty()) {
            dynamicChoice
        } else {
            books.filter { b ->
                val titleLower = b.title.lowercase()
                titleLower.contains("48 laws") ||
                    titleLower.contains("human nature") ||
                    titleLower.contains("art of war")
            }
        }
        list.sortedBy { b ->
            val t = b.title.lowercase()
            when {
                t.contains("48") -> 0
                t.contains("human nature") -> 1
                t.contains("art of war") -> 2
                else -> 3
            }
        }
    }
    val audioBooks = remember(books) { books.filter { it.hasAudio } }
    val philosophyBooks = remember(books) {
        books.filter { b ->
            val titleLower = b.title.lowercase()
            val authorLower = b.author.lowercase()
            b.category?.equals("Philosophy", ignoreCase = true) == true ||
                titleLower.contains("what is life") ||
                titleLower.contains("prince") ||
                titleLower.contains("meditation") ||
                titleLower.contains("epictetus") ||
                titleLower.contains("enchiridion") ||
                titleLower.contains("thinketh") ||
                titleLower.contains("art of war") ||
                authorLower.contains("schrödinger") ||
                authorLower.contains("schrodinger") ||
                authorLower.contains("machiavelli") ||
                authorLower.contains("aurelius") ||
                authorLower.contains("allen") ||
                authorLower.contains("sun tzu")
        }
    }
    val growthBooks = remember(books) {
        books.filter { b ->
            val titleLower = b.title.lowercase()
            val authorLower = b.author.lowercase()
            b.category?.equals("Self-Growth", ignoreCase = true) == true ||
                titleLower.contains("atomic") ||
                titleLower.contains("rich") ||
                titleLower.contains("hours") ||
                titleLower.contains("franklin") ||
                titleLower.contains("self help") ||
                titleLower.contains("diamonds") ||
                titleLower.contains("power") ||
                titleLower.contains("48") ||
                titleLower.contains("art of war") ||
                authorLower.contains("clear") ||
                authorLower.contains("wattles") ||
                authorLower.contains("bennett") ||
                authorLower.contains("smiles") ||
                authorLower.contains("greene") ||
                authorLower.contains("sun tzu")
        }
    }
    val classicLiterature = remember(books) {
        books.filter { b ->
            val titleLower = b.title.lowercase()
            b.category?.equals("Classics", ignoreCase = true) == true ||
                titleLower.contains("pride") ||
                titleLower.contains("frankenstein") ||
                titleLower.contains("jane eyre") ||
                titleLower.contains("moby")
        }
    }

    val filteredList = remember(books, selectedFilter) {
        when (selectedFilter) {
            "Audio" -> audioBooks
            "Philosophy" -> philosophyBooks
            "Self-Growth" -> growthBooks
            "Classics" -> classicLiterature
            else -> books
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomClearance + 24.dp)
    ) {
        // Top Header
        item(key = "header", contentType = "header") {
            BooksHeader(
                offline = offline,
                onRetryConnection = onRetryConnection,
                onOpenMe = onOpenMe
            )
        }

        // Filter Pills
        item(key = "filter_chips", contentType = "filter_chips") {
            FilterChipsRow(
                filters = filters,
                selected = selectedFilter,
                onSelect = onSelectFilter
            )
            Spacer(Modifier.height(16.dp))
        }

        if (selectedFilter == "All") {
            // Shelf 0: Editor's Choice (Only 48 Laws of Power & The Laws of Human Nature)
            if (editorsChoiceBooks.isNotEmpty()) {
                item(key = "shelf_editors_choice", contentType = "shelf") {
                    BookShelfSection(
                        title = "Editor's Choice",
                        subtitle = "Curated masterworks in your library",
                        books = editorsChoiceBooks,
                        onSelectBook = onSelectBook,
                        onSeeAll = { onSelectFilter("All") }
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            // Shelf 1: Available with Audio
            if (audioBooks.isNotEmpty()) {
                item(key = "shelf_audio", contentType = "shelf") {
                    BookShelfSection(
                        title = "Available with Audio",
                        subtitle = "Full recordings & narrator chapters",
                        books = audioBooks,
                        onSelectBook = onSelectBook,
                        onSeeAll = { onSelectFilter("Audio") }
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            // Shelf 2: Philosophy & Stoicism
            if (philosophyBooks.isNotEmpty()) {
                item(key = "shelf_philosophy", contentType = "shelf") {
                    BookShelfSection(
                        title = "Philosophy & Stoicism",
                        subtitle = "Ancient wisdom for modern clarity",
                        books = philosophyBooks,
                        onSelectBook = onSelectBook,
                        onSeeAll = { onSelectFilter("Philosophy") }
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            // Shelf 3: Conduct of Life & Habits
            if (growthBooks.isNotEmpty()) {
                item(key = "shelf_growth", contentType = "shelf") {
                    BookShelfSection(
                        title = "Conduct of Life & Habits",
                        subtitle = "Classics on focus, mindset & mastery",
                        books = growthBooks,
                        onSelectBook = onSelectBook,
                        onSeeAll = { onSelectFilter("Self-Growth") }
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            // Shelf 4: Curated Classic Literature
            if (classicLiterature.isNotEmpty()) {
                item(key = "shelf_classics", contentType = "shelf") {
                    BookShelfSection(
                        title = "Classic Literature",
                        subtitle = "Timeless masterpieces",
                        books = classicLiterature,
                        onSelectBook = onSelectBook,
                        onSeeAll = { onSelectFilter("Classics") }
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }

        // Categorized grid only when a specific category is selected
        if (selectedFilter != "All") {
            item(key = "category_header", contentType = "category_header") {
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedFilter,
                            fontFamily = Display,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink
                        )
                        Text(
                            text = "${filteredList.size} books",
                            fontSize = 12.sp,
                            color = InkSoft
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }

            items(
                items = filteredList.chunked(2),
                key = { pair -> pair.joinToString("-") { it.id } },
                contentType = { "grid_row" }
            ) { pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BookCard(
                        book = pair[0],
                        onClick = { onSelectBook(pair[0]) },
                        modifier = Modifier.weight(1f)
                    )
                    if (pair.size > 1) {
                        BookCard(
                            book = pair[1],
                            onClick = { onSelectBook(pair[1]) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BooksHeader(
    offline: Boolean,
    onRetryConnection: () -> Unit,
    onOpenMe: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "A library for everyone,",
                    fontSize = 13.sp,
                    color = Ember,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Booktips for you",
                    fontFamily = Display,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (offline) {
                    OfflineConnectivityButton(
                        onRetry = onRetryConnection,
                        modifier = Modifier
                    )
                }

                // SPARK brand mark (the peak of the A on its own)
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CardBg.copy(alpha = 0.85f))
                        .border(1.dp, Line, CircleShape)
                        .clickable(onClick = onOpenMe),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(com.spark.app.R.drawable.ic_spark_mark),
                        contentDescription = "Open profile",
                        tint = Ember,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Public domain book summaries and chapter audio.",
            fontSize = 12.sp,
            color = InkSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FilterChipsRow(
    filters: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = filters,
            key = { it },
            contentType = { "filter_chip" }
        ) { filter ->
            val isSelected = filter == selected
            val bg = if (isSelected) Forest else CardBg
            val borderCol = if (isSelected) SageAccent.copy(alpha = 0.5f) else Line
            val textCol = if (isSelected) Ink else InkSoft

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
                    .border(1.dp, borderCol, RoundedCornerShape(20.dp))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                        onSelect(filter)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = filter,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = textCol
                )
            }
        }
    }
}

@Composable
private fun BookShelfSection(
    title: String,
    subtitle: String,
    books: List<BookDto>,
    onSelectBook: (BookDto) -> Unit,
    onSeeAll: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    fontFamily = Display,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = InkSoft
                )
            }
            TextButton(onClick = onSeeAll) {
                Text(
                    text = "See all",
                    fontSize = 12.sp,
                    color = Ember,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(
                items = books,
                key = { it.id },
                contentType = { "shelf_book" }
            ) { book ->
                ShelfBookCard(
                    book = book,
                    onClick = { onSelectBook(book) }
                )
            }
        }
    }
}

@Composable
private fun ShelfBookCard(
    book: BookDto,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val imageRequest = remember(book.coverUrl) {
        if (book.coverUrl.isNullOrBlank()) null
        else {
            ImageRequest.Builder(context)
                .data(book.coverUrl)
                .crossfade(false)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .networkCachePolicy(coil.request.CachePolicy.ENABLED)
                .build()
        }
    }

    Column(
        modifier = Modifier
            .width(136.dp)
            .clickable(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onClick()
                }
            )
    ) {
        // Book Cover Box with Audio Badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(204.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(PaperDeep)
                .border(1.dp, Line, RoundedCornerShape(10.dp))
        ) {
            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "${book.title} cover",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Elegant fallback cover
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ReadingSurface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = book.title,
                        fontFamily = Display,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = book.author,
                        fontSize = 10.sp,
                        color = InkSoft,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }

            // Headphone Audio Badge (Storytel signature)
            if (book.hasAudio) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC000000))
                        .border(0.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_headphones_16),
                        contentDescription = "Audiobook available",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = book.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = book.author.ifBlank { "Unknown" },
                    fontSize = 11.sp,
                    color = InkSoft,
                    minLines = 1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val chapterLabel = when (book.chapterCount) {
                0 -> "0 chapters"
                1 -> "1 chapter"
                else -> "${book.chapterCount} chapters"
            }
            Text(
                text = chapterLabel,
                fontSize = 12.sp,
                color = if (book.chapterCount > 0) Ember else InkSoft.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold,
                minLines = 1,
                maxLines = 1,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun BookCard(
    book: BookDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val imageRequest = remember(book.coverUrl) {
        if (book.coverUrl.isNullOrBlank()) null
        else {
            ImageRequest.Builder(context)
                .data(book.coverUrl)
                .crossfade(false)
                .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                .networkCachePolicy(coil.request.CachePolicy.ENABLED)
                .build()
        }
    }

    Column(
        modifier = modifier
            .clickable(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                    onClick()
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(PaperDeep)
                .border(1.dp, Line, RoundedCornerShape(10.dp))
        ) {
            if (imageRequest != null) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = "${book.title} cover",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ReadingSurface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = book.title,
                        fontFamily = Display,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink,
                        textAlign = TextAlign.Center,
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = book.author,
                        fontSize = 11.sp,
                        color = InkSoft,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }

            if (book.hasAudio) {
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC000000))
                        .border(0.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_headphones_16),
                        contentDescription = "Audiobook available",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = book.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = book.author.ifBlank { "Unknown" },
                    fontSize = 11.sp,
                    color = InkSoft,
                    minLines = 1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val chapterLabel = when (book.chapterCount) {
                0 -> "0 chapters"
                1 -> "1 chapter"
                else -> "${book.chapterCount} chapters"
            }
            Text(
                text = chapterLabel,
                fontSize = 12.sp,
                color = if (book.chapterCount > 0) Ember else InkSoft.copy(alpha = 0.8f),
                fontWeight = FontWeight.SemiBold,
                minLines = 1,
                maxLines = 1,
                lineHeight = 16.sp
            )
        }
    }
}

/**
 * Storytel-style Book Detail Dialog / Sheet.
 * Displays book metadata, high-res cover, audio play button, read in reels button,
 * and a scrollable chapter-by-chapter tracklist with listen & read actions.
 */
@Composable
private fun BookDetailDialog(
    book: BookDto,
    onDismiss: () -> Unit,
    onOpenReels: () -> Unit,
    onOpenChapterInReels: (String) -> Unit,
    player: AudioPlayerBox?,
    onOpenAudioPlayer: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    BackHandler(onBack = onDismiss)
    var isBookmarked by remember { mutableStateOf(false) }

    val audioUi by player?.state?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
    val hasAudio = audioUi?.url?.isNotBlank() == true

    val audioPlaylist = remember(book) {
        book.chapters.filter { !it.audioUrl.isNullOrBlank() }.map {
            com.spark.app.audio.PlaylistItem(
                url = it.audioUrl!!,
                title = it.title,
                coverUrl = book.coverUrl,
                author = book.author,
                bookTitle = book.title
            )
        }
    }

    val isCurrentBookPlaying = remember(audioUi, book) {
        val currentUrl = audioUi?.url ?: ""
        val currentTitle = audioUi?.title ?: ""
        if (currentUrl.isBlank()) false
        else {
            currentTitle.contains(book.title, ignoreCase = true) ||
            book.chapters.any { ch ->
                val chUrl = ch.audioUrl ?: ""
                chUrl.isNotBlank() && (
                    currentUrl == chUrl ||
                    currentUrl.endsWith(chUrl) ||
                    currentUrl.substringAfterLast("/").substringBefore("?") == chUrl.substringAfterLast("/").substringBefore("?")
                )
            }
        }
    }
    val isAudioPlayingNow = isCurrentBookPlaying && (audioUi?.playing == true)
    val isAudioBufferingNow = isCurrentBookPlaying && (audioUi?.buffering == true)

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    var dominantColor by remember { mutableStateOf(generateFallbackColor(book.title)) }

    LaunchedEffect(book.coverUrl, book.title) {
        val cover = book.coverUrl
        if (!cover.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val request = ImageRequest.Builder(context)
                        .data(cover)
                        .allowHardware(false)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build()
                    val result = context.imageLoader.execute(request)
                    val drawable = result.drawable
                    if (drawable is android.graphics.drawable.BitmapDrawable) {
                        dominantColor = extractDominantColor(drawable.bitmap)
                    } else {
                        dominantColor = generateFallbackColor(book.title)
                    }
                } catch (_: Exception) {
                    dominantColor = generateFallbackColor(book.title)
                }
            }
        } else {
            dominantColor = generateFallbackColor(book.title)
        }
    }

    val animatedDominantColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 650),
        label = "book ambient glow color"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090D11))
        ) {
            // Radial ambient glow: scatters the book's dominant color around the book artwork
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                animatedDominantColor.copy(alpha = 0.72f),
                                animatedDominantColor.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            center = Offset(
                                with(LocalDensity.current) { (screenWidth / 2).toPx() },
                                with(LocalDensity.current) { (screenHeight * 0.22f).toPx() }
                            ),
                            radius = with(LocalDensity.current) { (screenWidth * 1.25f).toPx() }
                        )
                    )
            )

            // Vertical ambient falloff gradient: smooth fade down into rich dark playback surface
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to animatedDominantColor.copy(alpha = 0.35f),
                            0.35f to animatedDominantColor.copy(alpha = 0.20f),
                            0.65f to Color(0xFF090D11).copy(alpha = 0.90f),
                            1.0f to Color(0xFF090D11)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "Book Details",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                isBookmarked = !isBookmarked
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                Toast.makeText(
                                    context,
                                    if (isBookmarked) "Saved to your shelf" else "Removed from shelf",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Bookmark book",
                                tint = if (isBookmarked) Ember else Color.White.copy(alpha = 0.8f)
                            )
                        }

                        // Triple dot options menu requested by user
                        var menuExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "More options",
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(Color(0xFF1B222C))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Read in Reels", color = Color.White) },
                                    onClick = {
                                        menuExpanded = false
                                        val first = book.chapters.firstOrNull()
                                        onDismiss()
                                        if (first != null) onOpenChapterInReels(first.id) else onOpenReels()
                                    }
                                )
                                if (book.hasAudio) {
                                    DropdownMenuItem(
                                        text = { Text("Open Player", color = Color.White) },
                                        onClick = {
                                            menuExpanded = false
                                            val firstAudio = book.chapters.firstOrNull { !it.audioUrl.isNullOrBlank() }
                                            if (firstAudio?.audioUrl != null && player != null) {
                                                player.playUrl(
                                                    url = firstAudio.audioUrl,
                                                    title = firstAudio.title,
                                                    queue = audioPlaylist,
                                                    isBookPlayback = true,
                                                    coverUrl = book.coverUrl,
                                                    author = book.author,
                                                    bookTitle = book.title
                                                )
                                            }
                                            onDismiss()
                                            onOpenAudioPlayer()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .padding(bottom = if (hasAudio) 96.dp else 24.dp)
                ) {
                    // Hero Section: Cover & Book Meta
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Large Cover with Ambient Glow Shadow
                        Box(
                            modifier = Modifier
                                .width(132.dp)
                                .height(198.dp)
                                .shadow(
                                    elevation = 20.dp,
                                    shape = RoundedCornerShape(14.dp),
                                    spotColor = animatedDominantColor.copy(alpha = 0.85f)
                                )
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF18181A))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                        ) {
                            if (!book.coverUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(book.coverUrl)
                                        .crossfade(true)
                                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .networkCachePolicy(coil.request.CachePolicy.ENABLED)
                                        .build(),
                                    contentDescription = book.title,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(ReadingSurface)
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = book.title,
                                        fontFamily = Display,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Right Column Details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.title,
                                fontFamily = Display,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 26.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "By ${book.author}",
                                fontSize = 13.5.sp,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Spacer(Modifier.height(10.dp))

                            // Badges
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                BadgePill(text = "Public Domain")
                                if (book.hasAudio) {
                                    BadgePill(text = "🎧 Audio")
                                }
                            }
                            if (book.chapterCount > 0) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "${book.chapterCount} chapter${if (book.chapterCount != 1) "s" else ""} available",
                                    fontSize = 11.5.sp,
                                    color = Ember,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Action Buttons Row (Play Audio / Read in Reels)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                if (isCurrentBookPlaying && player != null) {
                                    if (audioUi?.playing == false) {
                                        player.toggle()
                                    }
                                    onDismiss()
                                    onOpenAudioPlayer()
                                } else {
                                    val firstAudioChapter = book.chapters.firstOrNull { !it.audioUrl.isNullOrBlank() }
                                    if (firstAudioChapter?.audioUrl != null && player != null) {
                                        player.playUrl(
                                            url = firstAudioChapter.audioUrl,
                                            title = firstAudioChapter.title,
                                            queue = audioPlaylist,
                                            isBookPlayback = true,
                                            coverUrl = book.coverUrl,
                                            author = book.author,
                                            bookTitle = book.title
                                        )
                                        onDismiss()
                                        onOpenAudioPlayer()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Starting audio for ${book.title}…",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Ember.copy(alpha = 0.35f)),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Ember,
                                contentColor = Color(0xFF141210)
                            )
                        ) {
                            if (isAudioBufferingNow) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color(0xFF141210),
                                    strokeWidth = 2.dp
                                )
                            } else if (isAudioPlayingNow) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_pause),
                                    contentDescription = "Pause Audio",
                                    tint = Color(0xFF141210),
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play Audio",
                                    tint = Color(0xFF141210),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isCurrentBookPlaying) {
                                    if (isAudioBufferingNow) "Buffering…"
                                    else if (isAudioPlayingNow) "Playing"
                                    else "Resume"
                                } else "Play Audio",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF141210)
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                onDismiss()
                                val firstChapter = book.chapters.firstOrNull()
                                if (firstChapter != null) {
                                    onOpenChapterInReels(firstChapter.id)
                                } else {
                                    onOpenReels()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0x55FFFFFF)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_book_filled_24),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Read in Reels",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }

                    // Chapter List Section
                    Spacer(Modifier.height(28.dp))

                    Text(
                        text = "Chapters & Recordings",
                        fontFamily = Display,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Listen to narration or read card summaries",
                        fontSize = 11.5.sp,
                        color = Color.White.copy(alpha = 0.70f)
                    )

                    Spacer(Modifier.height(14.dp))

                    if (book.chapters.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x331A1817))
                                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(10.dp))
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Full chapter summaries and audio being formatted.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.70f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        book.chapters.forEachIndexed { index, chapter ->
                            ChapterRowItem(
                                index = index + 1,
                                chapter = chapter,
                                bookTitle = book.title,
                                author = book.author,
                                coverUrl = book.coverUrl,
                                player = player,
                                playlist = audioPlaylist,
                                onDismiss = onDismiss,
                                onOpenAudioPlayer = onOpenAudioPlayer,
                                onOpenChapterInReels = onOpenChapterInReels
                            )
                            Spacer(Modifier.height(10.dp))
                        }
                    }

                    Spacer(Modifier.height(40.dp))
                }
            }

            // Floating Mini Audio Player when audio is active
            if (hasAudio && player != null) {
                MiniAudioPlayer(
                    player = player,
                    onOpen = {
                        onDismiss()
                        onOpenAudioPlayer()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun ChapterRowItem(
    index: Int,
    chapter: BookChapterDto,
    bookTitle: String,
    author: String,
    coverUrl: String?,
    player: AudioPlayerBox?,
    playlist: List<com.spark.app.audio.PlaylistItem> = emptyList(),
    onDismiss: () -> Unit,
    onOpenAudioPlayer: () -> Unit,
    onOpenChapterInReels: (String) -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    val audioUi by player?.state?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(null) }
    val isThisChapter = remember(audioUi, chapter) {
        val currentUrl = audioUi?.url ?: ""
        val chUrl = chapter.audioUrl ?: ""
        if (currentUrl.isBlank() || chUrl.isBlank()) false
        else {
            currentUrl == chUrl ||
            currentUrl.endsWith(chUrl) ||
            currentUrl.substringAfterLast("/").substringBefore("?") == chUrl.substringAfterLast("/").substringBefore("?")
        }
    }
    val isThisChapterPlaying = isThisChapter && (audioUi?.playing == true)
    val isThisChapterBuffering = isThisChapter && (audioUi?.buffering == true)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x351A1817))
            .border(1.dp, Color(0x28FFFFFF), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Chapter Number Box
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x44000000))
                    .border(1.dp, Color(0x33FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = String.format("%02d", index),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ember
                )
            }

            // Chapter Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (chapter.actionLine.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = chapter.actionLine,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Buttons for this Chapter
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Play Chapter Audio Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isThisChapterPlaying || isThisChapterBuffering) Ember else Color(0xFF242C38))
                        .border(
                            1.dp,
                            if (isThisChapterPlaying || isThisChapterBuffering) Ember.copy(alpha = 0.8f) else Color(0x33FFFFFF),
                            CircleShape
                        )
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            if (isThisChapter && player != null) {
                                if (!isThisChapterPlaying) {
                                    player.toggle()
                                }
                                onDismiss()
                                onOpenAudioPlayer()
                            } else if (!chapter.audioUrl.isNullOrBlank() && player != null) {
                                player.playUrl(
                                    url = chapter.audioUrl,
                                    title = chapter.title,
                                    queue = playlist,
                                    isBookPlayback = true,
                                    coverUrl = coverUrl,
                                    author = author,
                                    bookTitle = bookTitle
                                )
                                onDismiss()
                                onOpenAudioPlayer()
                            } else {
                                Toast.makeText(
                                    context,
                                    "Playing audio for ${chapter.title}…",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isThisChapterBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF141210),
                            strokeWidth = 2.dp
                        )
                    } else if (isThisChapterPlaying) {
                        Icon(
                            painter = painterResource(R.drawable.ic_pause),
                            contentDescription = "Pause",
                            tint = Color(0xFF141210),
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play chapter",
                            tint = if (isThisChapter) Ember else Color.White,
                            modifier = Modifier.size(18.dp).offset(x = 1.dp)
                        )
                    }
                }

                // Read Chapter in Reels Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x44000000))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            onDismiss()
                            onOpenChapterInReels(chapter.id)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_book_filled_24),
                        contentDescription = "Read chapter",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(PaperDeep)
            .border(0.5.dp, Line, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = InkSoft,
            fontWeight = FontWeight.Medium
        )
    }
}
