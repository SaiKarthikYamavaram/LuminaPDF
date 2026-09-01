package com.lumina.pdf.viewer.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * "Paper & Ink" — the app's design tokens. Warm paper surfaces and ink-brown text with a single
 * amber "Lumina" accent, shared by every screen (previously the home screen and the PDF reading
 * pane used unrelated palettes). Mirrors the token set defined in the Figma design file.
 */
object LuminaColors {
    // Light mode
    val surfaceBaseLight = Color(0xFFF5EFE4)
    val surfaceRaisedLight = Color(0xFFFFFFFF)
    val surfaceSunkenLight = Color(0xFFECE4D6)
    val inkPrimaryLight = Color(0xFF2A2420)
    val inkMutedLight = Color(0xFF6B6055)
    val inkFaintLight = Color(0xFF9A8E7E)
    val accentLuminaLight = Color(0xFFB8720A)
    val accentLuminaSoftLight = Color(0xFFF3E3C6)
    val accentOnLuminaLight = Color(0xFFFFFFFF)
    val borderLight = Color(0xFFDFD3BE)
    val statusSuccessLight = Color(0xFF3D8361)
    val statusDangerLight = Color(0xFFB23A2E)
    val statusNotesLight = Color(0xFFC9962B)

    // Dark mode
    val surfaceBaseDark = Color(0xFF221D18)
    val surfaceRaisedDark = Color(0xFF2E2721)
    val surfaceSunkenDark = Color(0xFF3A322A)
    val inkPrimaryDark = Color(0xFFF1EAE0)
    val inkMutedDark = Color(0xFFB5A896)
    val inkFaintDark = Color(0xFF7C7264)
    val accentLuminaDark = Color(0xFFF2B84B)
    val accentLuminaSoftDark = Color(0xFF4A3B1E)
    val accentOnLuminaDark = Color(0xFF2A1B00)
    val borderDark = Color(0xFF4A4038)
    val statusSuccessDark = Color(0xFF5FAE85)
    val statusDangerDark = Color(0xFFE0685A)
    val statusNotesDark = Color(0xFFE8C468)
}

/** 4dp-based spacing scale. */
object LuminaSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    val xxl: Dp = 24.dp
    val xxxl: Dp = 32.dp
    val xxxxl: Dp = 40.dp
}

/** Corner radius scale. */
object LuminaRadius {
    val sm: Dp = 6.dp
    val md: Dp = 10.dp
    val lg: Dp = 16.dp
    val pill: Dp = 999.dp
}

// Status colors with no Material3 role of their own (sync state, notes indicator, file-type
// badge) — still theme-aware, unlike the old "deliberately theme-independent" literals they
// replace, since Paper & Ink's dark mode needs brighter variants for contrast.
@Composable
fun luminaSuccessColor(): Color = if (isSystemInDarkTheme()) LuminaColors.statusSuccessDark else LuminaColors.statusSuccessLight

@Composable
fun luminaDangerColor(): Color = if (isSystemInDarkTheme()) LuminaColors.statusDangerDark else LuminaColors.statusDangerLight

@Composable
fun luminaNotesColor(): Color = if (isSystemInDarkTheme()) LuminaColors.statusNotesDark else LuminaColors.statusNotesLight

/**
 * Builds a Material3 [ColorScheme] from the Paper & Ink tokens so every existing
 * `MaterialTheme.colorScheme.*` reference (background, surfaceContainer tiers, primary,
 * outlineVariant, etc.) resolves to the new palette without call-site changes.
 *
 * Role mapping (see also the Figma file's variable names):
 * - background/surface -> surface/base
 * - surfaceContainerLow/surfaceContainer -> surface/raised (sidebar, cards, panels)
 * - surfaceContainerHigh/surfaceContainerLowest -> surface/sunken (inputs, thumbnails, insets)
 * - primary/onPrimary -> accent/lumina, accent/on-lumina
 * - primaryContainer -> accent/lumina-soft (selected nav item, active chips)
 * - onSurface/onSurfaceVariant -> ink/primary, ink/muted
 * - outline/outlineVariant -> border/default
 */
fun luminaColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        darkColorScheme(
            primary = LuminaColors.accentLuminaDark,
            onPrimary = LuminaColors.accentOnLuminaDark,
            primaryContainer = LuminaColors.accentLuminaSoftDark,
            onPrimaryContainer = LuminaColors.accentLuminaDark,
            secondary = LuminaColors.accentLuminaDark,
            background = LuminaColors.surfaceBaseDark,
            onBackground = LuminaColors.inkPrimaryDark,
            surface = LuminaColors.surfaceBaseDark,
            onSurface = LuminaColors.inkPrimaryDark,
            surfaceVariant = LuminaColors.surfaceSunkenDark,
            onSurfaceVariant = LuminaColors.inkMutedDark,
            outline = LuminaColors.borderDark,
            outlineVariant = LuminaColors.borderDark,
            surfaceContainerLowest = LuminaColors.surfaceSunkenDark,
            surfaceContainerLow = LuminaColors.surfaceRaisedDark,
            surfaceContainer = LuminaColors.surfaceRaisedDark,
            surfaceContainerHigh = LuminaColors.surfaceSunkenDark,
            surfaceContainerHighest = LuminaColors.surfaceSunkenDark,
            error = LuminaColors.statusDangerDark,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = LuminaColors.accentLuminaLight,
            onPrimary = LuminaColors.accentOnLuminaLight,
            primaryContainer = LuminaColors.accentLuminaSoftLight,
            onPrimaryContainer = LuminaColors.accentLuminaLight,
            secondary = LuminaColors.accentLuminaLight,
            background = LuminaColors.surfaceBaseLight,
            onBackground = LuminaColors.inkPrimaryLight,
            surface = LuminaColors.surfaceBaseLight,
            onSurface = LuminaColors.inkPrimaryLight,
            surfaceVariant = LuminaColors.surfaceSunkenLight,
            onSurfaceVariant = LuminaColors.inkMutedLight,
            outline = LuminaColors.borderLight,
            outlineVariant = LuminaColors.borderLight,
            surfaceContainerLowest = LuminaColors.surfaceSunkenLight,
            surfaceContainerLow = LuminaColors.surfaceRaisedLight,
            surfaceContainer = LuminaColors.surfaceRaisedLight,
            surfaceContainerHigh = LuminaColors.surfaceSunkenLight,
            surfaceContainerHighest = LuminaColors.surfaceSunkenLight,
            error = LuminaColors.statusDangerLight,
            onError = Color.White
        )
    }
}
