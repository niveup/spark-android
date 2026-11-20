package com.spark.app.audio

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.spark.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val PlayerPage = Color(0xFF121110)
private val PlayerSurface = Color(0xFF1A1817)
private val PlayerRaised = Color(0xFF242220)
private val PlayerInk = Color(0xFFECE6E0)
private val PlayerMuted = Color(0xFFCFC7BE)
private val PlayerLine = Color(0xFF49433D)
private val PlayerAccent = Color(0xFFFFB454)

@Composable
fun MiniAudioPlayer(
    player: AudioPlayerBox,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = { player.stopAndClear() }
) {
    val state by player.state.collectAsStateWithLifecycle()
    val title = state.title ?: "Audio chapter"
    val duration = state.durationMs
    val progress = if (duration > 0) {
        (state.positionMs.toFloat() / duration).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400, easing = LinearEasing),
        label = "mini audio progress"
    )
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val offsetY = remember { Animatable(0f) }
    val dismissThresholdPx = with(density) { 50.dp.toPx() }
    val dismissExitTargetPx = with(density) { 200.dp.toPx() }

    LaunchedEffect(state.url) {
        if (state.url != null) {
            offsetY.snapTo(0f)
        }
    }

    AnimatedVisibility(
        visible = state.url != null,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF13221A),
            border = BorderStroke(1.dp, Color(0xFF233B2E)),
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = offsetY.value
                    val dragProgress = (offsetY.value / dismissThresholdPx).coerceIn(0f, 1f)
                    alpha = 1f - (dragProgress * 0.45f)
                    val s = 1f - (dragProgress * 0.03f)
                    scaleX = s
                    scaleY = s
                }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            val current = offsetY.value
                            val next = if (delta > 0) {
                                current + delta
                            } else {
                                if (current > 0) {
                                    (current + delta).coerceAtLeast(0f)
                                } else {
                                    (current + delta * 0.15f).coerceAtLeast(-20f)
                                }
                            }
                            offsetY.snapTo(next)
                        }
                    },
                    onDragStopped = { velocity ->
                        coroutineScope.launch {
                            val shouldDismiss = (offsetY.value > dismissThresholdPx) ||
                                (velocity > 800f && offsetY.value > 10f)
                            if (shouldDismiss) {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                offsetY.animateTo(
                                    targetValue = dismissExitTargetPx,
                                    animationSpec = tween(durationMillis = 180, easing = FastOutLinearInEasing)
                                )
                                onDismiss()
                            } else {
                                offsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                    }
                )
                .clip(RoundedCornerShape(18.dp))
                .clickable { onOpen() }
                .semantics {
                    contentDescription = "Mini audio player for $title. Swipe down to close."
                    customActions = listOf(
                        CustomAccessibilityAction(label = "Close audio player") {
                            onDismiss()
                            true
                        }
                    )
                }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Subtle swipe handle affordance
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 32.dp, height = 3.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C4C3A))
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Album / Audio Glyph
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A3024))
                            .border(1.dp, Color(0xFF2C4C3A), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_headphones_16),
                            contentDescription = null,
                            tint = if (state.playing) PlayerAccent else PlayerMuted,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(Modifier.width(11.dp))

                    // Title & Status
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (state.buffering) PlayerAccent
                                        else if (state.playing) Color(0xFF4EE49B)
                                        else PlayerMuted
                                    )
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = when {
                                    state.buffering -> "CONNECTING"
                                    state.playing -> "PLAYING"
                                    else -> "PAUSED"
                                },
                                color = PlayerAccent,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp
                            )
                            if (state.autoplay && state.isBookPlayback) {
                                Text(
                                    text = " · AUTOPLAY",
                                    color = Color(0xFF8FA396),
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = title,
                            color = Color(0xFFF7F2EC),
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Play / Pause Action Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(PlayerAccent)
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                if (state.failed) {
                                    state.url?.let { player.playUrl(it, title, isBookPlayback = state.isBookPlayback) }
                                } else {
                                    player.toggle()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.playing && !state.failed) {
                            Text(
                                text = "Ⅱ",
                                color = Color(0xFF141210),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play",
                                tint = Color(0xFF141210),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Sleek integrated progress bar at the bottom edge
                if (state.buffering) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = PlayerAccent,
                        trackColor = Color(0xFF1B2B22)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(Color(0xFF1B2B22))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .height(2.5.dp)
                                .background(PlayerAccent)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AudioPlayerOverlay(
    player: AudioPlayerBox,
    onDismiss: () -> Unit
) {
    val state by player.state.collectAsStateWithLifecycle()
    val title = state.title ?: "Audio chapter"
    val duration = state.durationMs
    var scrubbing by remember { mutableStateOf(false) }
    var scrubPosition by remember { mutableFloatStateOf(0f) }
    val animatedPosition by animateFloatAsState(
        targetValue = state.positionMs.toFloat(),
        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
        label = "audio position"
    )
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // Dynamic dominant color extracted from the book cover
    var dominantColor by remember { mutableStateOf(Color(0xFF1B3B5F)) }

    LaunchedEffect(state.coverUrl, state.title) {
        val cover = state.coverUrl
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
                    if (drawable is BitmapDrawable) {
                        dominantColor = extractDominantColor(drawable.bitmap)
                    } else {
                        dominantColor = generateFallbackColor(state.title ?: "Audio")
                    }
                } catch (_: Exception) {
                    dominantColor = generateFallbackColor(state.title ?: "Audio")
                }
            }
        } else {
            dominantColor = generateFallbackColor(state.title ?: "Audio")
        }
    }

    val animatedDominantColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 650),
        label = "ambient glow color"
    )

    val playlist = player.getPlaylist()
    var showChaptersSheet by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(220)) +
                scaleIn(initialScale = 0.98f, animationSpec = tween(220)),
            modifier = Modifier.fillMaxSize()
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
                                    animatedDominantColor.copy(alpha = 0.70f),
                                    animatedDominantColor.copy(alpha = 0.32f),
                                    Color.Transparent
                                ),
                                center = Offset(
                                    with(LocalDensity.current) { (screenWidth / 2).toPx() },
                                    with(LocalDensity.current) { (screenHeight * 0.28f).toPx() }
                                ),
                                radius = with(LocalDensity.current) { (screenWidth * 1.15f).toPx() }
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
                                0.38f to animatedDominantColor.copy(alpha = 0.20f),
                                0.68f to Color(0xFF090D11).copy(alpha = 0.88f),
                                1.0f to Color(0xFF090D11)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // --- Top App Bar ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Minimize Chevron (left)
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0x33000000))
                                .border(1.dp, Color(0x22FFFFFF), CircleShape)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Minimize player",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        // Triple Dot Menu (right)
                        var menuExpanded by remember { mutableStateOf(false) }
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x33000000))
                                    .border(1.dp, Color(0x22FFFFFF), CircleShape)
                                    .clickable { menuExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = "Options",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(Color(0xFF1B222C))
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Stop Audio", color = Color.White) },
                                    onClick = {
                                        menuExpanded = false
                                        player.stopAndClear()
                                        onDismiss()
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (state.autoplay) "Disable Autoplay" else "Enable Autoplay",
                                            color = Color.White
                                        )
                                    },
                                    onClick = {
                                        menuExpanded = false
                                        player.toggleAutoplay()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.weight(0.10f))

                    // --- Book Cover Artwork with Ambient Drop Shadow ---
                    val coverSize = (screenWidth * 0.72f).coerceIn(210.dp, 280.dp)
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        shadowElevation = 24.dp,
                        color = Color(0xFF141920),
                        border = BorderStroke(1.dp, Color(0x28FFFFFF)),
                        modifier = Modifier
                            .size(coverSize)
                            .shadow(
                                elevation = 24.dp,
                                shape = RoundedCornerShape(22.dp),
                                spotColor = animatedDominantColor.copy(alpha = 0.65f)
                            )
                            .clip(RoundedCornerShape(22.dp))
                    ) {
                        if (!state.coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(state.coverUrl)
                                    .crossfade(true)
                                    .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                                    .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                                    .build(),
                                contentDescription = title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Stylized fallback cover
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                animatedDominantColor,
                                                Color(0xFF10151C)
                                            )
                                        )
                                    )
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_headphones_16),
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        text = state.bookTitle ?: title,
                                        color = Color.White,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    state.author?.let { author ->
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            text = "By $author",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(0.12f))

                    // --- Chapter Title & Book / Author ---
                    val displayChapterTitle = remember(state.title, state.bookTitle) {
                        val raw = state.title ?: "Audio chapter"
                        val bt = state.bookTitle
                        if (!bt.isNullOrBlank() && raw.startsWith("$bt: ")) {
                            raw.removePrefix("$bt: ")
                        } else raw
                    }
                    val displaySubtitle = remember(state.bookTitle, state.author) {
                        listOfNotNull(state.bookTitle, state.author).joinToString(" · ")
                            .ifBlank { "Audiobook · SPARK" }
                    }

                    Text(
                        text = displayChapterTitle,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { heading() }
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = displaySubtitle,
                        color = Color(0xFFBAC5D0),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.weight(0.08f))

                    // --- Chapter List Pill Button ---
                    if (playlist.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0x33000000),
                                border = BorderStroke(1.dp, Color(0x2BFFFFFF)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { showChaptersSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.List,
                                        contentDescription = "Chapter list",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "Chapters (${playlist.size})",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(0.08f))

                    // --- Scrubber & 3-Column Time Row ---
                    val currentMs = if (scrubbing) scrubPosition.toLong() else state.positionMs
                    val remainingMs = (duration - currentMs).coerceAtLeast(0L)

                    Slider(
                        value = (if (scrubbing) scrubPosition else animatedPosition)
                            .coerceIn(0f, duration.toFloat().coerceAtLeast(1f)),
                        onValueChange = {
                            scrubbing = true
                            scrubPosition = it
                        },
                        onValueChangeFinished = {
                            player.seekTo(scrubPosition.toLong())
                            scrubbing = false
                        },
                        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFFFFB454),
                            activeTrackColor = Color(0xFFFFB454),
                            inactiveTrackColor = Color(0x33FFFFFF)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatMinSec(currentMs),
                            color = Color(0xFF98A6B5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (duration > 0) "${formatMinSec(remainingMs)} left" else if (state.buffering) "Connecting" else "Streaming",
                            color = Color(0xFF98A6B5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (duration > 0) "-${formatMinSec(remainingMs)}" else "--:--",
                            color = Color(0xFF98A6B5),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(Modifier.weight(0.12f))

                    // --- Playback Controls Row (Audible 5-Button Cluster) ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Previous Chapter (|◀)
                        ChapterSkipButton(
                            isNext = false,
                            onClick = {
                                if (state.positionMs > 3000L) {
                                    player.seekTo(0L)
                                } else {
                                    val didSkip = player.playPreviousChapter()
                                    if (!didSkip) player.seekTo(0L)
                                }
                            }
                        )

                        // 2. Rewind 30s (⟲ 30)
                        CircularSeekButton(
                            seconds = 30,
                            isForward = false,
                            onClick = { player.seekBy(-30_000L) }
                        )

                        // 3. Central White Circular Play/Pause Button
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .shadow(8.dp, CircleShape)
                                .clickable {
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                    if (state.failed) {
                                        state.url?.let {
                                            player.playUrl(
                                                url = it,
                                                title = state.title ?: "Audio",
                                                isBookPlayback = state.isBookPlayback,
                                                coverUrl = state.coverUrl,
                                                author = state.author,
                                                bookTitle = state.bookTitle
                                            )
                                        }
                                    } else {
                                        player.toggle()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.playing && !state.failed) {
                                Text(
                                    text = "Ⅱ",
                                    color = Color(0xFF0F141A),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color(0xFF0F141A),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        // 4. Fast Forward 30s (⟳ 30)
                        CircularSeekButton(
                            seconds = 30,
                            isForward = true,
                            onClick = { player.seekBy(30_000L) }
                        )

                        // 5. Next Chapter (▶|)
                        ChapterSkipButton(
                            isNext = true,
                            onClick = {
                                player.playNextChapter()
                            }
                        )
                    }

                    Spacer(Modifier.weight(0.14f))

                    // --- Bottom Utility Bar ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AudibleSpeedControl(
                            selectedSpeed = state.playbackSpeed,
                            onSelectSpeed = player::setPlaybackSpeed
                        )
                        AudibleTimerControl(
                            remainingMs = state.sleepTimerRemainingMs,
                            onSetTimer = player::setSleepTimer
                        )
                        AudibleAutoplayControl(
                            autoplay = state.autoplay,
                            onToggle = player::toggleAutoplay
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }

                // --- Chapter Selection Modal Sheet ---
                if (showChaptersSheet && playlist.isNotEmpty()) {
                    Dialog(onDismissRequest = { showChaptersSheet = false }) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF141920),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Chapters",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(onClick = { showChaptersSheet = false }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                                    itemsIndexed(playlist) { index, item ->
                                        val isCurrent = item.url == state.url
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isCurrent) Color(0x22FFB454) else Color.Transparent)
                                                .clickable {
                                                    showChaptersSheet = false
                                                    player.playUrl(
                                                        url = item.url,
                                                        title = item.title,
                                                        queue = playlist,
                                                        isBookPlayback = true,
                                                        coverUrl = item.coverUrl ?: state.coverUrl,
                                                        author = item.author ?: state.author,
                                                        bookTitle = item.bookTitle ?: state.bookTitle
                                                    )
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${index + 1}".padStart(2, '0'),
                                                color = if (isCurrent) Color(0xFFFFB454) else Color(0xFF7A8A9A),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(28.dp)
                                            )
                                            Text(
                                                text = item.title,
                                                color = if (isCurrent) Color(0xFFFFB454) else Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (isCurrent && state.playing) {
                                                Text(
                                                    text = "Playing",
                                                    color = Color(0xFFFFB454),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CircularSeekButton(
    seconds: Int,
    isForward: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .size(54.dp)
            .clip(CircleShape)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(38.dp)) {
            val strokeWidth = 2.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            val arcSize = Size(diameter, diameter)

            if (isForward) {
                // Clockwise: 290 deg sweep
                drawArc(
                    color = Color.White,
                    startAngle = 290f,
                    sweepAngle = 290f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                val arrowHead = Path().apply {
                    val tipX = size.width / 2f + 4.dp.toPx()
                    val tipY = strokeWidth / 2f
                    moveTo(tipX, tipY)
                    lineTo(tipX - 4.dp.toPx(), tipY - 3.5.dp.toPx())
                    lineTo(tipX - 4.dp.toPx(), tipY + 3.5.dp.toPx())
                    close()
                }
                drawPath(arrowHead, color = Color.White)
            } else {
                // Counter-clockwise: -290 deg sweep
                drawArc(
                    color = Color.White,
                    startAngle = 250f,
                    sweepAngle = -290f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                val arrowHead = Path().apply {
                    val tipX = size.width / 2f - 4.dp.toPx()
                    val tipY = strokeWidth / 2f
                    moveTo(tipX, tipY)
                    lineTo(tipX + 4.dp.toPx(), tipY - 3.5.dp.toPx())
                    lineTo(tipX + 4.dp.toPx(), tipY + 3.5.dp.toPx())
                    close()
                }
                drawPath(arrowHead, color = Color.White)
            }
        }

        Text(
            text = "$seconds",
            color = Color.White,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ChapterSkipButton(
    isNext: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(20.dp)) {
            val barWidth = 2.5.dp.toPx()
            if (isNext) {
                val triangle = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width - barWidth - 2.dp.toPx(), size.height / 2f)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(triangle, color = Color.White)
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(size.width - barWidth, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(1.dp.toPx())
                )
            } else {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(0f, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(1.dp.toPx())
                )
                val triangle = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(barWidth + 2.dp.toPx(), size.height / 2f)
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(triangle, color = Color.White)
            }
        }
    }
}

@Composable
private fun AudibleSpeedControl(
    selectedSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${selectedSpeed}x",
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = "Speed",
                color = Color(0xFF98A6B5),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1B222C))
        ) {
            listOf(0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { speed ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${speed}x",
                            color = if (speed == selectedSpeed) Color(0xFFFFB454) else Color.White,
                            fontWeight = if (speed == selectedSpeed) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelectSpeed(speed)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AudibleTimerControl(
    remainingMs: Long,
    onSetTimer: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (remainingMs > 0) formatMinSec(remainingMs) else "Timer"
    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_headphones_16),
                contentDescription = null,
                tint = if (remainingMs > 0) Color(0xFFFFB454) else Color.White,
                modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = label,
                color = if (remainingMs > 0) Color(0xFFFFB454) else Color(0xFF98A6B5),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1B222C))
        ) {
            listOf(
                null to "Off",
                5 to "5 minutes",
                10 to "10 minutes",
                15 to "15 minutes",
                30 to "30 minutes",
                45 to "45 minutes",
                60 to "60 minutes"
            ).forEach { (minutes, text) ->
                DropdownMenuItem(
                    text = { Text(text, color = Color.White) },
                    onClick = {
                        onSetTimer(minutes)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AudibleAutoplayControl(
    autoplay: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onToggle()
            }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = if (autoplay) "Auto On" else "Auto Off",
            color = if (autoplay) Color(0xFFFFB454) else Color.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = "Chapters",
            color = Color(0xFF98A6B5),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

internal fun extractDominantColor(bitmap: Bitmap): Color {
    val scaled = Bitmap.createScaledBitmap(bitmap, 28, 28, false)
    val width = scaled.width
    val height = scaled.height
    val pixels = IntArray(width * height)
    scaled.getPixels(pixels, 0, width, 0, 0, width, height)

    var bestPixel = 0
    var bestScore = -1f
    var totalR = 0L
    var totalG = 0L
    var totalB = 0L
    var validCount = 0

    val hsv = FloatArray(3)

    for (pixel in pixels) {
        val alpha = android.graphics.Color.alpha(pixel)
        if (alpha < 128) continue

        val r = android.graphics.Color.red(pixel)
        val g = android.graphics.Color.green(pixel)
        val b = android.graphics.Color.blue(pixel)

        android.graphics.Color.RGBToHSV(r, g, b, hsv)
        val sat = hsv[1]
        val value = hsv[2]

        // Ignore near-blacks and near-whites
        if (value in 0.15f..0.94f && (sat > 0.10f || value < 0.85f)) {
            totalR += r
            totalG += g
            totalB += b
            validCount++

            // Weight towards vibrant tones with healthy saturation
            val score = sat * 0.7f + (1f - kotlin.math.abs(value - 0.55f)) * 0.3f
            if (score > bestScore) {
                bestScore = score
                bestPixel = pixel
            }
        }
    }

    if (scaled != bitmap) {
        scaled.recycle()
    }

    val chosen = if (bestScore > 0f) {
        bestPixel
    } else if (validCount > 0) {
        android.graphics.Color.rgb(
            (totalR / validCount).toInt(),
            (totalG / validCount).toInt(),
            (totalB / validCount).toInt()
        )
    } else {
        android.graphics.Color.rgb(27, 59, 95)
    }

    return Color(chosen)
}

internal fun generateFallbackColor(key: String): Color {
    val palettes = listOf(
        Color(0xFF1B3B5F), // Deep cerulean / navy
        Color(0xFF1A4636), // Deep emerald
        Color(0xFF5E3018), // Warm burnt sienna / amber
        Color(0xFF4C1D38), // Deep wine / plum
        Color(0xFF2C3E50), // Slate midnight
        Color(0xFF18424B)  // Deep teal
    )
    val index = kotlin.math.abs(key.hashCode()) % palettes.size
    return palettes[index]
}
