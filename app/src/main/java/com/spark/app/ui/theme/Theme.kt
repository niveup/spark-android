package com.spark.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * SPARK dark theme — complete Material 3 token set.
 *
 * Design rules used here (keep them when adding colors later):
 *  1. One accent only: amber ("spark"). Everything else is neutral.
 *  2. Neutrals are WARM (slightly brown) so they sit next to amber without clashing.
 *  3. Elevation is expressed with surface*Container tones, never with shadows.
 *     Low = closer to background, Highest = a card popping above the page.
 *  4. Text: onSurface for body, onSurfaceVariant for supporting text,
 *     primary for a few short labels only (never for paragraphs).
 *  5. Contrast floor: body text is ~13:1 on background, secondary ~7:1.
 */

// ---- Accent: amber ------------------------------------------------------
private val Amber = Color(0xFFFFB454)          // primary text/button accent
private val OnAmber = Color(0xFF3A2100)        // text on top of amber
private val AmberContainer = Color(0xFF5A3200) // filled chip / selected state
private val OnAmberContainer = Color(0xFFFFDDB8)
private val AmberDim = Color(0xFF8F5B00)       // inversePrimary (snackbars)

// ---- Accent 2: sand -----------------------------------------------------
private val Sand = Color(0xFFDCC7AE)
private val OnSand = Color(0xFF3A2E22)
private val SandContainer = Color(0xFF524437)
private val OnSandContainer = Color(0xFFFFDDBF)

// ---- Accent 3: teal (secondary feedback, e.g. correct answers) ----------
private val Teal = Color(0xFFA6D8C4)
private val OnTeal = Color(0xFF00382C)
private val TealContainer = Color(0xFF1E4F42)
private val OnTealContainer = Color(0xFFC6F3DF)

// ---- Warm neutrals ------------------------------------------------------
private val Ink = Color(0xFF0E0D0C)            // window background
private val Page = Color(0xFF121110)           // background / surface
private val Raise1 = Color(0xFF1A1817)         // surfaceContainerLow
private val Raise2 = Color(0xFF1E1C1B)         // surfaceContainer
private val Raise3 = Color(0xFF242220)         // surfaceContainerHigh
private val Raise4 = Color(0xFF2F2C2A)         // surfaceContainerHighest
private val OnPage = Color(0xFFECE6E0)         // primary text
private val OnPageDim = Color(0xFFCFC7BE)      // secondary text
private val Variant = Color(0xFF49433C)        // surfaceVariant
private val Outline = Color(0xFF998F86)
private val OutlineFaint = Color(0xFF49433D)   // outlineVariant (dividers)

// ---- Error --------------------------------------------------------------
private val Err = Color(0xFFFFB4AB)
private val OnErr = Color(0xFF690005)
private val ErrContainer = Color(0xFF93000A)
private val OnErrContainer = Color(0xFFFFDAD6)

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

    // Fixed tones (never change between light/dark) — kept in the amber family
    // so no default Material purple can leak into a component.
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

    error = Err,
    onError = OnErr,
    errorContainer = ErrContainer,
    onErrorContainer = OnErrContainer,

    scrim = Color.Black,
)

@Composable
fun SparkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SparkColors,
        content = content,
    )
}
