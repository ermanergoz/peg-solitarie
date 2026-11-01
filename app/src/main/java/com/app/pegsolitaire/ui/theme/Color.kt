package com.app.pegsolitaire.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * "Candy Clay" Material 3 color schemes (docs/ui/DESIGN-SYSTEM.md).
 *
 * Every hex literal in the app lives ONLY in this file: components consume
 * `MaterialTheme.colorScheme` roles, and the clay gradient/accent colors below are
 * assembled into [ClayColors] palettes by `ui/theme/Clay.kt` — never referenced as
 * raw hex at a call site. The dark scheme is a desaturated tonal variant (not an
 * inversion); every body-text pair was verified ≥ 4.5:1 in both modes.
 */
internal val LightColors =
    lightColorScheme(
        primary = Color(0xFF7C3AED),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEDE4FF),
        onPrimaryContainer = Color(0xFF3B1D77),
        secondary = Color(0xFFEC4899),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFE1EF),
        onSecondaryContainer = Color(0xFF831843),
        tertiary = Color(0xFFF59E0B),
        onTertiary = Color(0xFF241A00),
        tertiaryContainer = Color(0xFFFFEFC7),
        onTertiaryContainer = Color(0xFF6B4E00),
        background = Color(0xFFF4F1FA),
        onBackground = Color(0xFF332F3A),
        surface = Color(0xFFFBF9FF),
        onSurface = Color(0xFF332F3A),
        surfaceVariant = Color(0xFFF1EAFB),
        onSurfaceVariant = Color(0xFF635F69),
        surfaceContainerLowest = Color(0xFFFDFBFF),
        surfaceContainerLow = Color(0xFFF7F3FD),
        surfaceContainer = Color(0xFFF4EEFC),
        surfaceContainerHigh = Color(0xFFF1EAFB),
        surfaceContainerHighest = Color(0xFFECE4F8),
        outline = Color(0xFFC9BEE0),
        outlineVariant = Color(0xFFDCD3EC),
        error = Color(0xFFDC2626),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFEE2E2),
        onErrorContainer = Color(0xFF7F1D1D),
    )

internal val DarkColors =
    darkColorScheme(
        primary = Color(0xFFA78BFA),
        onPrimary = Color(0xFF2E1065),
        primaryContainer = Color(0xFF4C3A85),
        onPrimaryContainer = Color(0xFFEDE4FF),
        secondary = Color(0xFFF472B6),
        onSecondary = Color(0xFF500724),
        secondaryContainer = Color(0xFF7A2352),
        onSecondaryContainer = Color(0xFFFFE1EF),
        tertiary = Color(0xFFFBBF24),
        onTertiary = Color(0xFF422006),
        tertiaryContainer = Color(0xFF5C4300),
        onTertiaryContainer = Color(0xFFFFEFC7),
        background = Color(0xFF1B1626),
        onBackground = Color(0xFFEDE9F5),
        surface = Color(0xFF241E31),
        onSurface = Color(0xFFEDE9F5),
        surfaceVariant = Color(0xFF2E2740),
        onSurfaceVariant = Color(0xFFA79FB5),
        surfaceContainerLowest = Color(0xFF15101F),
        surfaceContainerLow = Color(0xFF211B2D),
        surfaceContainer = Color(0xFF2A2339),
        surfaceContainerHigh = Color(0xFF2E2740),
        surfaceContainerHighest = Color(0xFF362E4B),
        outline = Color(0xFF4A4160),
        outlineVariant = Color(0xFF3A3350),
        error = Color(0xFFF87171),
        onError = Color(0xFF450A0A),
        errorContainer = Color(0xFF5C1D1D),
        onErrorContainer = Color(0xFFFECACA),
    )

// ---------------------------------------------------------------------------
// Clay gradient + accent colors (DESIGN-SYSTEM.md §Gradients / §Elevation).
// Consumed by `ui/theme/Clay.kt`, which packages them into per-theme ClayColors.
// ---------------------------------------------------------------------------

/** `CtaGradient` (vertical): #A78BFA → #7C3AED; brighter variant for dark mode. */
internal val LightCtaGradientColors = listOf(Color(0xFFA78BFA), Color(0xFF7C3AED))
internal val DarkCtaGradientColors = listOf(Color(0xFFB6A0FC), Color(0xFF8B5CF6))

/** `PegGradient` (radial): highlight → body → rim candy-pink sphere shading. */
internal val LightPegGradientColors =
    listOf(Color(0xFFF9A8D4), Color(0xFFEC4899), Color(0xFFBE185D))
internal val DarkPegGradientColors =
    listOf(Color(0xFFFBC5E2), Color(0xFFF472B6), Color(0xFFBE185D))

/** `GoldGradient`: #FDE68A → #F59E0B stars/celebration; brighter end in dark mode. */
internal val LightGoldGradientColors = listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))
internal val DarkGoldGradientColors = listOf(Color(0xFFFFF3C4), Color(0xFFFBBF24))

/**
 * Colour-blind palette, light theme (docs/ux/colour-vision.md §3.4, CV-09): amber-700 for the hint
 * rings and the hint connector. The dark theme's hint stays `tertiary`, so it has no token here.
 */
internal val LightColorblindHint = Color(0xFFB45309)

/**
 * Colour-blind palette, light theme (§3.4, CV-10): the earned star's and the done pip's fill —
 * `GoldGradient`'s linear geometry in amber, light stop → deep stop. Dark keeps the gold gradient.
 */
internal val LightColorblindStarFillColors = listOf(Color(0xFFC05A08), Color(0xFFB45309))

/** Small specular highlight dot on a candy peg sphere. */
internal val LightPegHighlight = Color(0xCCFFFFFF)
internal val DarkPegHighlight = Color(0xB3FFFFFF)

/** Two-tone inset hole dimple: darker top shadow ring + softer inner fill. */
internal val LightDimpleShadow = Color(0xFFCFC2E8)
internal val DarkDimpleShadow = Color(0xFF15101F)
internal val LightDimpleFill = Color(0xFFE4DBF4)
internal val DarkDimpleFill = Color(0xFF201A2F)

/** Violet-tinted soft shadow (never harsh black), ~0.25 alpha per the contract. */
internal val LightClayShadowTint = Color(0x407C3AED)
internal val DarkClayShadowTint = Color(0x66120826)
