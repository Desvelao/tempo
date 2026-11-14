package app.trainer.ui.theme

import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import app.trainer.R

/** "Focus" design direction: near-black surfaces, one lime accent, a blue highlight, big numerals. */
private val Lime = Color(0xFFC6F432)
private val Ink = Color(0xFF0E1116)
private val Panel = Color(0xFF171C24)
private val Line = Color(0xFF2A3340)
private val Snow = Color(0xFFF2F5F7)
private val Muted = Color(0xFF9AA5B1)
private val Blue = Color(0xFF7AA2FF)
private val Coral = Color(0xFFFF6B6B)

val FocusDark: ColorScheme = darkColorScheme(
    primary = Lime, onPrimary = Ink,
    secondary = Blue, onSecondary = Ink,
    secondaryContainer = Color(0xFF2A3A12), onSecondaryContainer = Lime,
    tertiary = Blue, onTertiary = Ink,
    background = Ink, onBackground = Snow,
    surface = Ink, onSurface = Snow,
    // surfaceVariant must differ from surfaceContainer, or Material picks onSurfaceVariant as the default text colour on panels.
    surfaceVariant = Color(0xFF1A202A), onSurfaceVariant = Muted,
    surfaceContainerLowest = Ink, surfaceContainerLow = Panel, surfaceContainer = Panel,
    surfaceContainerHigh = Panel, surfaceContainerHighest = Panel,
    outline = Color(0xFF5B6776), outlineVariant = Line,
    error = Coral, onError = Ink,
)

val FocusLight: ColorScheme = lightColorScheme(
    primary = Color(0xFF4A6200), onPrimary = Color.White,
    secondary = Color(0xFF2F5BD1), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EEB9), onSecondaryContainer = Color(0xFF2E3D00),
    tertiary = Color(0xFF2F5BD1), onTertiary = Color.White,
    background = Color(0xFFF4F6F8), onBackground = Ink,
    surface = Color(0xFFF4F6F8), onSurface = Ink,
    surfaceVariant = Color(0xFFEDF0F4), onSurfaceVariant = Color(0xFF4A5563),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White, surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White, surfaceContainerHighest = Color.White,
    outline = Color(0xFF8A94A3), outlineVariant = Color(0xFFD5DBE3),
    error = Color(0xFFC62828), onError = Color.White,
)

/**
 * Colour-blind friendly variants. Success and error no longer rely on a red-green difference: they are blue and
 * orange, which stay far apart under red-green colour blindness (see ColorBlindPaletteTest for the numbers).
 */
val FocusDarkColorBlind: ColorScheme = FocusDark.copy(
    primary = Color(0xFF56B4E9), onPrimary = Ink,
    secondary = Color(0xFFF0E442), onSecondary = Ink,
    secondaryContainer = Color(0xFF12304A), onSecondaryContainer = Color(0xFF56B4E9),
    tertiary = Color(0xFFF0E442), onTertiary = Ink,
    error = Color(0xFFE69F00), onError = Ink,
)

val FocusLightColorBlind: ColorScheme = FocusLight.copy(
    primary = Color(0xFF0072B2), onPrimary = Color.White,
    secondary = Color(0xFF7A6200), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E9F6), onSecondaryContainer = Color(0xFF00334F),
    tertiary = Color(0xFF7A6200), onTertiary = Color.White,
    error = Color(0xFFB34700), onError = Color.White,
)

/** The scheme for a theme and palette choice. */
fun focusScheme(dark: Boolean, colorBlind: Boolean): ColorScheme = when {
    dark && colorBlind -> FocusDarkColorBlind
    dark -> FocusDark
    colorBlind -> FocusLightColorBlind
    else -> FocusLight
}

/** A warm and a cool accent for illustrations (game icons) that are not part of the colour scheme roles. */
@Immutable
class AccentColors(val warm: Color, val cool: Color)

fun accentColors(dark: Boolean, colorBlind: Boolean) = when {
    dark && colorBlind -> AccentColors(Color(0xFFE69F00), Color(0xFF56B4E9))
    dark -> AccentColors(Color(0xFFFF6B6B), Color(0xFF7AA2FF))
    colorBlind -> AccentColors(Color(0xFFB34700), Color(0xFF0072B2))
    else -> AccentColors(Color(0xFFE03E3E), Color(0xFF2F5BD1))
}

val LocalAccentColors = staticCompositionLocalOf { accentColors(dark = true, colorBlind = false) }

/** True when the colour-blind palette is on. Games with colour-coded content read this. */
val LocalColorBlindPalette = staticCompositionLocalOf { false }

/** True when decorative animation should be replaced by static or stepped visuals. */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * A display size that follows the user's font scale up to [maxScale]. Very large numerals would otherwise
 * overflow the screen at 200% font scale, while smaller text keeps scaling fully.
 */
@Composable
fun cappedSp(base: Int, maxScale: Float = 1.3f): TextUnit {
    val scale = LocalDensity.current.fontScale
    return (base * minOf(scale, maxScale) / scale).sp
}

private fun grotesk(weight: FontWeight) =
    Font(R.font.space_grotesk, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

val SpaceGrotesk = FontFamily(grotesk(FontWeight.Normal), grotesk(FontWeight.Medium), grotesk(FontWeight.Bold))

private fun focusTypography(): Typography {
    val t = Typography()
    val bold = FontWeight.Bold
    val medium = FontWeight.Medium
    return Typography(
        displayLarge = t.displayLarge.copy(fontFamily = SpaceGrotesk, fontWeight = bold),
        displayMedium = t.displayMedium.copy(fontFamily = SpaceGrotesk, fontWeight = bold),
        displaySmall = t.displaySmall.copy(fontFamily = SpaceGrotesk, fontWeight = bold),
        headlineLarge = t.headlineLarge.copy(fontFamily = SpaceGrotesk, fontWeight = bold),
        headlineMedium = t.headlineMedium.copy(fontFamily = SpaceGrotesk, fontWeight = bold),
        headlineSmall = t.headlineSmall.copy(fontFamily = SpaceGrotesk, fontWeight = bold),
        titleLarge = t.titleLarge.copy(fontFamily = SpaceGrotesk, fontWeight = bold),
        titleMedium = t.titleMedium.copy(fontFamily = SpaceGrotesk, fontWeight = bold),
        titleSmall = t.titleSmall.copy(fontFamily = SpaceGrotesk, fontWeight = medium),
        bodyLarge = t.bodyLarge.copy(fontFamily = SpaceGrotesk),
        bodyMedium = t.bodyMedium.copy(fontFamily = SpaceGrotesk),
        bodySmall = t.bodySmall.copy(fontFamily = SpaceGrotesk),
        labelLarge = t.labelLarge.copy(fontFamily = SpaceGrotesk, fontWeight = bold),
        labelMedium = t.labelMedium.copy(fontFamily = SpaceGrotesk, fontWeight = medium),
        labelSmall = t.labelSmall.copy(fontFamily = SpaceGrotesk, fontWeight = medium),
    )
}

private val FocusShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** True when the active colour scheme is the dark one. */
@Composable
fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
fun TrainerTheme(
    dark: Boolean,
    colorBlind: Boolean = false,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAccentColors provides accentColors(dark, colorBlind),
        LocalColorBlindPalette provides colorBlind,
        LocalReducedMotion provides reducedMotion,
    ) {
        MaterialTheme(
            colorScheme = focusScheme(dark, colorBlind),
            typography = focusTypography(),
            shapes = FocusShapes,
            content = content,
        )
    }
}
