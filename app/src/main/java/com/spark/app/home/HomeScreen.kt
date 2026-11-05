package com.spark.app.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

// Design tokens matching SPARK Golden Dark Editorial palette
private val SparkBg = Color(0xFF0E0D0C)
private val SparkCard = Color(0xFF181614)
private val SparkCardBorder = Color(0xFF342D24)
private val SparkGold = Color(0xFFF5A844)
private val SparkGoldLight = Color(0xFFFFBF6B)
private val SparkGoldDark = Color(0xFFE58B24)
private val SparkText = Color(0xFFECE6DF)
private val SparkMuted = Color(0xFF9E948A)
private val SparkPillBg = Color(0xFF262018)
private val SparkPillBorder = Color(0xFF3F3324)

enum class TimePhase {
    MORNING,    // 5..11
    AFTERNOON,  // 12..16
    EVENING,    // 17..21
    NIGHT       // 22..4
}

@Composable
fun HomeScreen(
    seenCount: Int,
    savedCount: Int,
    cardLimit: Int,
    minuteLimit: Int,
    onOpenReels: () -> Unit,
    onOpenSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val (timePhase, greetingText) = remember {
        val hour = LocalTime.now().hour
        when (hour) {
            in 5..11 -> TimePhase.MORNING to "Good Morning,"
            in 12..16 -> TimePhase.AFTERNOON to "Good Afternoon,"
            in 17..21 -> TimePhase.EVENING to "Good Evening,"
            else -> TimePhase.NIGHT to "Late Night,"
        }
    }

    val todayFormatted = remember {
        val today = LocalDate.now()
        val dayOfWeek = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        "$dayOfWeek, ${today.dayOfMonth}"
    }

    val pctCompleted = remember(seenCount, cardLimit) {
        if (cardLimit > 0) {
            ((seenCount.toFloat() / cardLimit) * 100).toInt().coerceIn(0, 100)
        } else 0
    }

    val cardsRemaining = remember(seenCount, cardLimit) {
        (cardLimit - seenCount).coerceAtLeast(0)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SparkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.Top
        ) {
            // ---- Top Header: Brand ----
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SPARK",
                    fontFamily = FontFamily.Serif,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    color = SparkText
                )
            }

            // ---- Ambient Hero: Date pill, Greeting & Dynamic Time Art ----
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
            ) {
                // Subtle ambient atmospheric glow matching the time of day
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(width = 210.dp, height = 152.dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    when (timePhase) {
                                        TimePhase.MORNING -> Color(0x22FF9800)
                                        TimePhase.AFTERNOON -> Color(0x30F5A844)
                                        TimePhase.EVENING -> Color(0x35E65100)
                                        TimePhase.NIGHT -> Color(0x1C5C6BC0)
                                    },
                                    Color.Transparent
                                ),
                                center = Offset(320f, 120f),
                                radius = 340f
                            )
                        )
                )

                // Background dynamic time-aware visual (Sun / Sunset / Moon & Mountain Art)
                TimeAtmosphereCanvas(
                    timePhase = timePhase,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(width = 195.dp, height = 150.dp)
                )

                // Left text block (Date Pill & Greeting)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 4.dp)
                ) {
                    // Date Pill
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF1C1712))
                            .border(1.dp, Color(0xFF382C1E), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = todayFormatted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD6CEBE),
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = greetingText,
                        fontFamily = FontFamily.Serif,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 36.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Satyam.",
                            fontFamily = FontFamily.Serif,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = SparkGold,
                            lineHeight = 36.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Daily Cadence Card ----
            DailyCadenceCard(
                pctCompleted = pctCompleted,
                seenCount = seenCount,
                cardLimit = cardLimit,
                cardsRemaining = cardsRemaining
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Action CTAs: Open Reels & Review Saved ----
            ActionCtas(
                savedCount = savedCount,
                onOpenReels = onOpenReels,
                onOpenSaved = onOpenSaved
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ---- 3-Column Quick Stats Row ----
            QuickStatsRow(
                seenCount = seenCount,
                savedCount = savedCount,
                minuteLimit = minuteLimit
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ---- Up Next in Shelf Section ----
            UpNextShelfCard(onOpenReels = onOpenReels)

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * Creative, dynamic time-aware atmosphere canvas:
 * Morning: Rising dawn sun with upward rays breaking over eastern ridge crests.
 * Afternoon: High radiant golden sun orb with solar flare rays, lens glow & sun-drenched peaks.
 * Evening: Setting amber sun dipping between ridge clefts with twilight crimson gradients & Venus star.
 * Night: Glowing golden crescent moon with starry night sky & midnight ridge silhouettes.
 */
@Composable
private fun TimeAtmosphereCanvas(
    timePhase: TimePhase,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (timePhase) {
            TimePhase.AFTERNOON -> {
                // High radiant afternoon golden sun & sun-drenched mountain peaks
                val sunCenter = Offset(w * 0.74f, h * 0.34f)
                val sunRadius = w * 0.17f

                // Wide ambient warmth corona
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x66F5A844),
                            Color(0x33DC6E1E),
                            Color(0x11FFD27D),
                            Color.Transparent
                        ),
                        center = sunCenter,
                        radius = w * 0.58f
                    ),
                    center = sunCenter,
                    radius = w * 0.58f
                )

                // Solar flare rays (12 rays with alternating lengths)
                val rayCount = 12
                for (i in 0 until rayCount) {
                    val angle = (i * (360f / rayCount) + 15f) * (Math.PI / 180f).toFloat()
                    val rayLength = if (i % 2 == 0) w * 0.33f else w * 0.24f
                    val startX = sunCenter.x + kotlin.math.cos(angle) * (sunRadius * 1.15f)
                    val startY = sunCenter.y + kotlin.math.sin(angle) * (sunRadius * 1.15f)
                    val endX = sunCenter.x + kotlin.math.cos(angle) * rayLength
                    val endY = sunCenter.y + kotlin.math.sin(angle) * rayLength

                    drawLine(
                        color = Color(0x77FFD27D),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = if (i % 2 == 0) 2.5f else 1.5f,
                        cap = StrokeCap.Round
                    )
                }

                // Golden lens glow ring
                drawCircle(
                    color = Color(0x35FFB454),
                    radius = sunRadius * 1.45f,
                    center = sunCenter,
                    style = Stroke(width = 1.5f)
                )

                // Sun Orb disk with warm radiant gradient
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFFDF8),
                            Color(0xFFFFB454),
                            Color(0xFFE58B24),
                            Color(0xFFC57E22)
                        ),
                        center = Offset(sunCenter.x - sunRadius * 0.2f, sunCenter.y - sunRadius * 0.2f),
                        radius = sunRadius
                    ),
                    radius = sunRadius,
                    center = sunCenter
                )

                // Distant Mountain Ridge
                val distantMountain = Path().apply {
                    moveTo(0f, h)
                    lineTo(w * 0.15f, h * 0.72f)
                    lineTo(w * 0.45f, h * 0.78f)
                    lineTo(w * 0.76f, h * 0.62f)
                    lineTo(w, h * 0.74f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = distantMountain, color = Color(0xFF281F17))

                val distantRim = Path().apply {
                    moveTo(0f, h * 0.76f)
                    lineTo(w * 0.15f, h * 0.72f)
                    lineTo(w * 0.45f, h * 0.78f)
                    lineTo(w * 0.76f, h * 0.62f)
                    lineTo(w, h * 0.74f)
                }
                drawPath(path = distantRim, color = Color(0x55FFB454), style = Stroke(width = 2f))

                // Middle Mountain Ridge
                val midMountain = Path().apply {
                    moveTo(0f, h)
                    lineTo(w * 0.28f, h * 0.82f)
                    lineTo(w * 0.58f, h * 0.85f)
                    lineTo(w * 0.82f, h * 0.71f)
                    lineTo(w, h * 0.80f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = midMountain, color = Color(0xFF1D1712))

                val midRim = Path().apply {
                    moveTo(0f, h * 0.85f)
                    lineTo(w * 0.28f, h * 0.82f)
                    lineTo(w * 0.58f, h * 0.85f)
                    lineTo(w * 0.82f, h * 0.71f)
                    lineTo(w, h * 0.80f)
                }
                drawPath(path = midRim, color = Color(0x88FFC473), style = Stroke(width = 2.5f))

                // Foreground Mountain Ridge
                val frontMountain = Path().apply {
                    moveTo(0f, h)
                    lineTo(w * 0.22f, h * 0.88f)
                    lineTo(w * 0.50f, h * 0.91f)
                    lineTo(w * 0.72f, h * 0.79f)
                    lineTo(w, h * 0.87f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = frontMountain, color = Color(0xFF120E0A))

                val frontRidgeRim = Path().apply {
                    moveTo(0f, h * 0.90f)
                    lineTo(w * 0.22f, h * 0.88f)
                    lineTo(w * 0.50f, h * 0.91f)
                    lineTo(w * 0.72f, h * 0.79f)
                    lineTo(w, h * 0.87f)
                }
                drawPath(path = frontRidgeRim, color = Color(0xCCFFD27D), style = Stroke(width = 3f))

                // Soaring bird silhouettes in the afternoon sky
                val bird1 = Path().apply {
                    val bx = w * 0.26f
                    val by = h * 0.30f
                    val s = w * 0.045f
                    moveTo(bx - s, by + s * 0.25f)
                    cubicTo(bx - s * 0.5f, by - s * 0.35f, bx - s * 0.15f, by - s * 0.1f, bx, by)
                    cubicTo(bx + s * 0.15f, by - s * 0.1f, bx + s * 0.5f, by - s * 0.35f, bx + s, by + s * 0.25f)
                }
                drawPath(path = bird1, color = Color(0xAA453324), style = Stroke(width = 1.8f, cap = StrokeCap.Round))

                val bird2 = Path().apply {
                    val bx = w * 0.42f
                    val by = h * 0.22f
                    val s = w * 0.035f
                    moveTo(bx - s, by + s * 0.25f)
                    cubicTo(bx - s * 0.5f, by - s * 0.35f, bx - s * 0.15f, by - s * 0.1f, bx, by)
                    cubicTo(bx + s * 0.15f, by - s * 0.1f, bx + s * 0.5f, by - s * 0.35f, bx + s, by + s * 0.25f)
                }
                drawPath(path = bird2, color = Color(0x88453324), style = Stroke(width = 1.5f, cap = StrokeCap.Round))

                // Golden sunlight dust particles
                drawCircle(color = Color(0xFFFFD27D), radius = 2.5f, center = Offset(w * 0.18f, h * 0.32f), alpha = 0.8f)
                drawCircle(color = Color(0xFFFFB454), radius = 3.5f, center = Offset(w * 0.38f, h * 0.18f), alpha = 0.7f)
                drawCircle(color = Color.White, radius = 2f, center = Offset(w * 0.52f, h * 0.40f), alpha = 0.85f)
                drawCircle(color = Color(0xFFFFBF6B), radius = 2.8f, center = Offset(w * 0.88f, h * 0.22f), alpha = 0.75f)
            }
            TimePhase.MORNING -> {
                // Rising morning sun breaking over the mountain horizon
                val sunCenter = Offset(w * 0.75f, h * 0.52f)
                val sunRadius = w * 0.18f

                // Dawn sky warm haze
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x66FFA726),
                            Color(0x33FF7043),
                            Color.Transparent
                        ),
                        center = sunCenter,
                        radius = w * 0.6f
                    ),
                    center = sunCenter,
                    radius = w * 0.6f
                )

                // Upward morning diagonal beams
                for (i in 0..6) {
                    val angle = (-40f - i * 15f) * (Math.PI / 180f).toFloat()
                    val startX = sunCenter.x + kotlin.math.cos(angle) * (sunRadius * 0.8f)
                    val startY = sunCenter.y + kotlin.math.sin(angle) * (sunRadius * 0.8f)
                    val endX = sunCenter.x + kotlin.math.cos(angle) * (w * 0.6f)
                    val endY = sunCenter.y + kotlin.math.sin(angle) * (w * 0.6f)

                    drawLine(
                        color = Color(0x44FFD580),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 3f,
                        cap = StrokeCap.Round
                    )
                }

                // Sun Orb
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFFF3E0), Color(0xFFFFB74D), Color(0xFFF57C00)),
                        start = Offset(sunCenter.x - sunRadius, sunCenter.y - sunRadius),
                        end = Offset(sunCenter.x + sunRadius, sunCenter.y + sunRadius)
                    ),
                    radius = sunRadius,
                    center = sunCenter
                )

                // Mountain ridges in dawn silhouette
                val backMountain = Path().apply {
                    moveTo(0f, h)
                    lineTo(w * 0.3f, h * 0.74f)
                    lineTo(w * 0.65f, h * 0.82f)
                    lineTo(w, h * 0.68f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = backMountain, color = Color(0xFF281E18))

                val frontMountain = Path().apply {
                    moveTo(0f, h)
                    lineTo(w * 0.2f, h * 0.86f)
                    lineTo(w * 0.55f, h * 0.88f)
                    lineTo(w * 0.8f, h * 0.78f)
                    lineTo(w, h * 0.84f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = frontMountain, color = Color(0xFF16110D))
            }
            TimePhase.EVENING -> {
                // Sunset golden hour: setting sun dipping into the mountain cleft
                val sunCenter = Offset(w * 0.68f, h * 0.54f)
                val sunRadius = w * 0.16f

                // Deep amber sunset radial aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x77FF8F00),
                            Color(0x44E64A19),
                            Color(0x228D2B11),
                            Color.Transparent
                        ),
                        center = sunCenter,
                        radius = w * 0.55f
                    ),
                    center = sunCenter,
                    radius = w * 0.55f
                )

                // Sun disk
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFFCC80), Color(0xFFFF8F00), Color(0xFFD84315)),
                        start = Offset(sunCenter.x - sunRadius, sunCenter.y - sunRadius),
                        end = Offset(sunCenter.x + sunRadius, sunCenter.y + sunRadius)
                    ),
                    radius = sunRadius,
                    center = sunCenter
                )

                // Sunset mountain silhouettes
                val backMountain = Path().apply {
                    moveTo(0f, h)
                    lineTo(w * 0.22f, h * 0.73f)
                    lineTo(w * 0.58f, h * 0.82f)
                    lineTo(w * 0.82f, h * 0.64f)
                    lineTo(w, h * 0.75f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = backMountain, color = Color(0xFF221712))

                // Evening star (Venus)
                val starPos = Offset(w * 0.28f, h * 0.26f)
                drawCircle(color = Color.White, radius = 2.5f, center = starPos)
                drawLine(
                    color = Color(0xAAFFDDB9),
                    start = Offset(starPos.x - 7f, starPos.y),
                    end = Offset(starPos.x + 7f, starPos.y),
                    strokeWidth = 1.2f
                )
                drawLine(
                    color = Color(0xAAFFDDB9),
                    start = Offset(starPos.x, starPos.y - 7f),
                    end = Offset(starPos.x, starPos.y + 7f),
                    strokeWidth = 1.2f
                )

                val frontMountain = Path().apply {
                    moveTo(0f, h)
                    lineTo(w * 0.28f, h * 0.85f)
                    lineTo(w * 0.52f, h * 0.89f)
                    lineTo(w * 0.74f, h * 0.76f)
                    lineTo(w, h * 0.86f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = frontMountain, color = Color(0xFF130E0B))
            }
            TimePhase.NIGHT -> {
                // Golden crescent moon with starry night sky
                val moonRadius = w * 0.16f
                val moonCenter = Offset(w * 0.72f, h * 0.38f)

                // Moon aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x55F5A844),
                            Color(0x22DC6E1E),
                            Color.Transparent
                        ),
                        center = moonCenter,
                        radius = w * 0.45f
                    ),
                    center = moonCenter,
                    radius = w * 0.45f
                )

                // Twinkling stars
                drawCircle(color = SparkGold, radius = 2f, center = Offset(w * 0.15f, h * 0.25f), alpha = 0.6f)
                drawCircle(color = Color.White, radius = 2.5f, center = Offset(w * 0.35f, h * 0.15f), alpha = 0.75f)
                drawCircle(color = SparkGoldLight, radius = 1.8f, center = Offset(w * 0.55f, h * 0.32f), alpha = 0.5f)
                drawCircle(color = Color.White, radius = 3f, center = Offset(w * 0.85f, h * 0.18f), alpha = 0.8f)

                // Crescent Moon Path
                val moonPath = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(center = moonCenter, radius = moonRadius))
                }
                val moonShadowPath = Path().apply {
                    addOval(
                        androidx.compose.ui.geometry.Rect(
                            center = Offset(moonCenter.x - moonRadius * 0.52f, moonCenter.y - moonRadius * 0.28f),
                            radius = moonRadius * 0.94f
                        )
                    )
                }
                val crescentPath = Path().apply {
                    op(moonPath, moonShadowPath, androidx.compose.ui.graphics.PathOperation.Difference)
                }

                drawPath(
                    path = crescentPath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFFDDB9), Color(0xFFFFB454), Color(0xFFD48325)),
                        start = Offset(moonCenter.x - moonRadius, moonCenter.y - moonRadius),
                        end = Offset(moonCenter.x + moonRadius, moonCenter.y + moonRadius)
                    )
                )

                // Night mountain ridges
                val backMountain = Path().apply {
                    moveTo(0f, h)
                    lineTo(w * 0.25f, h * 0.75f)
                    lineTo(w * 0.55f, h * 0.82f)
                    lineTo(w * 0.8f, h * 0.65f)
                    lineTo(w, h * 0.78f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = backMountain, color = Color(0xFF1E1A16))

                val frontMountain = Path().apply {
                    moveTo(0f, h)
                    lineTo(w * 0.2f, h * 0.84f)
                    lineTo(w * 0.45f, h * 0.88f)
                    lineTo(w * 0.7f, h * 0.76f)
                    lineTo(w, h * 0.85f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = frontMountain, color = Color(0xFF141210))
            }
        }
    }
}

/**
 * Daily Cadence Hero Card with glowing progress ring, due categories, and quote callout.
 */
@Composable
private fun DailyCadenceCard(
    pctCompleted: Int,
    seenCount: Int,
    cardLimit: Int,
    cardsRemaining: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SparkCard)
            .border(1.dp, SparkCardBorder, RoundedCornerShape(24.dp))
            .padding(20.dp)
    ) {
        Column {
            // Header Row: Lotus/Sprout icon + "Daily Cadence"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = SparkGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Daily Cadence",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Cards Remaining Pill
                Text(
                    text = if (cardsRemaining == 0) "Complete" else "$cardsRemaining left",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SparkGold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SparkPillBg)
                        .border(1.dp, SparkPillBorder, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Progress Ring & Counts
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Progress Gauge
                Box(
                    modifier = Modifier.size(92.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW = 8.dp.toPx()
                        val diameter = size.minDimension - strokeW
                        val radius = diameter / 2f
                        val centerOffset = Offset(size.width / 2f, size.height / 2f)

                        // Background track
                        drawCircle(
                            color = Color(0xFF332B22),
                            radius = radius,
                            center = centerOffset,
                            style = Stroke(width = strokeW)
                        )

                        // Progress Arc
                        val sweep = (pctCompleted / 100f) * 360f
                        drawArc(
                            brush = Brush.linearGradient(
                                colors = listOf(SparkGoldLight, SparkGoldDark)
                            ),
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                            size = Size(diameter, diameter),
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$pctCompleted%",
                            fontFamily = FontFamily.Serif,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "DONE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA69989),
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Stats text
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$seenCount",
                            fontFamily = FontFamily.Serif,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = " / $cardLimit Cards",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = SparkMuted,
                            modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (cardsRemaining == 0) "All daily cards cleared!"
                        else "Steady progress. $cardsRemaining cards left today.",
                        fontSize = 12.sp,
                        color = Color(0xFFB8ADA0),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Topic due pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TopicDueChip(label = "Habits (5 due)")
                TopicDueChip(label = "+2 Philosophy")
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom callout quote
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141210))
                    .border(1.dp, Color(0xFF29221A), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = "“",
                        fontFamily = FontFamily.Serif,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = SparkGold,
                        lineHeight = 24.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Steady progress. Compound your daily insights.",
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.sp,
                            color = Color(0xFFDFD6CB)
                        )
                        Text(
                            text = "— Atomic Habits",
                            fontSize = 10.sp,
                            color = Color(0xFF8F8274),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicDueChip(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SparkPillBg)
            .border(1.dp, SparkPillBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(SparkGold)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFDDD3C6)
        )
    }
}

/**
 * Primary CTAs: Radiant Golden "Open Reels" button + Outlined "Review Saved" button.
 */
@Composable
private fun ActionCtas(
    savedCount: Int,
    onOpenReels: () -> Unit,
    onOpenSaved: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Solid Golden Pill Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(50),
                    ambientColor = SparkGold,
                    spotColor = SparkGold
                )
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFFF5A844), Color(0xFFFBB454), Color(0xFFF39C2D))
                    )
                )
                .clickable(role = Role.Button, onClick = onOpenReels),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF120F0B),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Open Reels",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF120F0B)
                )
            }
        }

        // Secondary Outlined Frosted Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1714))
                .border(1.dp, Color(0xFF342D24), RoundedCornerShape(16.dp))
                .clickable(role = Role.Button, onClick = onOpenSaved),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFA89C8E),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Review Saved ($savedCount)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE0D7CD)
                )
            }
        }
    }
}

/**
 * 3-Column Metrics Card: Seen Today, Insights Saved, Daily Limit.
 */
@Composable
private fun QuickStatsRow(
    seenCount: Int,
    savedCount: Int,
    minuteLimit: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF151311))
            .border(1.dp, Color(0xFF2D2720), RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Col 1: Seen Today
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "$seenCount",
                fontFamily = FontFamily.Serif,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SparkGold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "SEEN TODAY",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8A7E72),
                letterSpacing = 0.5.sp
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(30.dp)
                .background(Color(0xFF2A231B))
        )

        // Col 2: Insights Saved
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "$savedCount",
                fontFamily = FontFamily.Serif,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "SAVED",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8A7E72),
                letterSpacing = 0.5.sp
            )
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(30.dp)
                .background(Color(0xFF2A231B))
        )

        // Col 3: Daily Limit
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${minuteLimit}m",
                fontFamily = FontFamily.Serif,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "DAILY LIMIT",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8A7E72),
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Up Next Shelf preview card.
 */
@Composable
private fun UpNextShelfCard(onOpenReels: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Up Next in Shelf",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Queue ›",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SparkGold,
                modifier = Modifier.clickable(onClick = onOpenReels)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1714))
                .border(1.dp, Color(0xFF302921), RoundedCornerShape(16.dp))
                .clickable(onClick = onOpenReels)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Book badge icon box
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF221C16))
                        .border(1.dp, Color(0xFF3E3325), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = null,
                        tint = SparkGold,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "2 MIN READ",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = SparkGold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF352B1E))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FOCUS ESSENTIALS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8A7E70)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Deep Focus & Cognitive Depth",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Deep Work by Cal Newport",
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 11.sp,
                        color = Color(0xFF9E9182)
                    )
                }
            }

            // Arrow button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2A231B))
                    .border(1.dp, Color(0xFF3F3324), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Open Next Insight",
                    tint = SparkGold,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
