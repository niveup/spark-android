package com.spark.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// SPARK opens in its quiet, warm dark theme. Amber carries the main actions;
// forest and teal support the reading surfaces and positive feedback.
private val Amber = Color(0xFFFFB454)
private val OnAmber = Color(0xFF3A2100)
private val AmberContainer = Color(0xFF5A3200)
private val OnAmberContainer = Color(0xFFFFDDB8)
private val AmberDim = Color(0xFF8F5B00)

private val Sand = Color(0xFFDCC7AE)
private val OnSand = Color(0xFF3A2E22)
private val SandContainer = Color(0xFF524437)
private val OnSandContainer = Color(0xFFFFDDBF)

private val Teal = Color(0xFFA6D8C4)
private val OnTeal = Color(0xFF00382C)
private val TealContainer = Color(0xFF1E4F42)
private val OnTealContainer = Color(0xFFC6F3DF)

private val Ink = Color(0xFF0E0D0C)
private val Page = Color(0xFF121110)
private val Raise1 = Color(0xFF1A1817)
private val Raise2 = Color(0xFF1E1C1B)
private val Raise3 = Color(0xFF242220)
private val Raise4 = Color(0xFF2F2C2A)
private val OnPage = Color(0xFFECE6E0)
private val OnPageDim = Color(0xFFCFC7BE)
private val Variant = Color(0xFF49433C)
private val Outline = Color(0xFF998F86)
private val OutlineFaint = Color(0xFF49433D)

private val Error = Color(0xFFFFB4AB)
private val OnError = Color(0xFF690005)
private val ErrorContainer = Color(0xFF93000A)
private val OnErrorContainer = Color(0xFFFFDAD6)

private val SparkColors = darkColorScheme(
    primary = Amber,
    onPrimary = OnAmber,
    primaryContainer = AmberContainer,
    onPrimaryContainer = OnAmberContainer,
    inversePrimary = AmberDim,

    secondary = Sand,
    onSecondary = OnSand,
    secondaryContainer = SandContainer,
    onSecondaryContainer = OnSandContainer,

    tertiary = Teal,
    onTertiary = OnTeal,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = OnTealContainer,

    background = Page,
    onBackground = OnPage,
    surface = Page,
    onSurface = OnPage,
    surfaceVariant = Variant,
    onSurfaceVariant = OnPageDim,
    surfaceTint = Amber,

    surfaceContainerLowest = Ink,
    surfaceContainerLow = Raise1,
    surfaceContainer = Raise2,
    surfaceContainerHigh = Raise3,
    surfaceContainerHighest = Raise4,
    surfaceDim = Ink,
    surfaceBright = Raise4,

    outline = Outline,
    outlineVariant = OutlineFaint,
    inverseSurface = OnPage,
    inverseOnSurface = Ink,

    primaryFixed = OnAmberContainer,
    primaryFixedDim = Amber,
    onPrimaryFixed = OnAmber,
    onPrimaryFixedVariant = AmberContainer,
    secondaryFixed = OnSandContainer,
    secondaryFixedDim = Sand,
    onSecondaryFixed = OnSand,
    onSecondaryFixedVariant = SandContainer,
    tertiaryFixed = OnTealContainer,
    tertiaryFixedDim = Teal,
    onTertiaryFixed = OnTeal,
    onTertiaryFixedVariant = TealContainer,

    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    scrim = Color.Black,
)

/** Shared page rhythm for Home, Saved, and Me. */
object SparkSpacing {
    val pageGutter = 20.dp
    val screenTop = 8.dp
    val sectionGap = 16.dp
    val cardInset = 16.dp
    val contentGap = 8.dp
    val rowGap = 12.dp
    val labelGap = 4.dp
}

/** Shared editorial type styles used by the dashboard and library screens. */
object SparkType {
    val kicker = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 1.1.sp
    )
    val pageTitle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 30.sp,
        letterSpacing = (-1).sp
    )
    val homeHeadline = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 31.sp,
        letterSpacing = (-1.05).sp
    )
    val displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp
    )
    val cardTitle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.5).sp
    )
    val quote = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 18.5.sp,
        lineHeight = 24.5.sp,
        letterSpacing = (-0.2).sp
    )
    val quoteSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 19.sp
    )
    val body = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    )
    val meta = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
    val compactMeta = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
}

@Composable
fun SparkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SparkColors,
        content = content,
    )
}
