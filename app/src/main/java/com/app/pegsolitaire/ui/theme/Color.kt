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

/**
 * `TitleGradient` (colour-vision.md TG-01): the Home title's own vertical gradient, one step
 * deeper along the violet ramp than the CTA's, so both stops read ≥ 4.5:1 on the background.
 */
internal val LightTitleGradientColors = listOf(Color(0xFF7C3AED), Color(0xFF5B21B6))
internal val DarkTitleGradientColors = listOf(Color(0xFFC4B5FD), Color(0xFFA78BFA))

// ---------------------------------------------------------------------------
// The backdrop, direction B "Deep gloss" (docs/ux/candy-backdrop.md §4.1). Consumed by
// `ui/theme/BackdropColors.kt`. `BackdropSkyTop` is the theme's `background` (BD-09).
// ---------------------------------------------------------------------------

internal val LightBackdropSkyTop = Color(0xFFF4F1FA)
internal val LightBackdropSkyMid = Color(0xFFF6E3F6)
internal val LightBackdropSkyLow = Color(0xFFF0C9E7)
internal val LightBackdropSkyDeep = Color(0xFFE3C3F6)
internal val LightBackdropSparseLow = Color(0xFFFBF0FA)
internal val LightBackdropWaveCrest = Color(0xFFF6B3DD)
internal val LightBackdropWaveBody = Color(0xFFDD8BE6)
internal val LightBackdropWaveDeep = Color(0xFFB98AF2)
internal val LightBackdropWaveLip = Color(0xB3FFFFFF)
internal val LightBackdropWaveSheen = Color(0xA6FFFFFF)
internal val LightBackdropLightPearlCore = Color(0xCCFFFFFF)
internal val LightBackdropLightPearlRim = Color(0xF2FFFFFF)
internal val LightBackdropLightPinkCore = Color(0x4DF472B6)
internal val LightBackdropLightPinkRim = Color(0x8CF472B6)
internal val LightBackdropLightVioletCore = Color(0x4DA78BFA)
internal val LightBackdropLightVioletRim = Color(0x8CA78BFA)

internal val DarkBackdropSkyTop = Color(0xFF1B1626)
internal val DarkBackdropSkyMid = Color(0xFF1C0E38)
internal val DarkBackdropSkyLow = Color(0xFF2A1150)
internal val DarkBackdropSkyDeep = Color(0xFF3A0F48)
internal val DarkBackdropSparseLow = Color(0xFF1C0E38)
internal val DarkBackdropWaveCrest = Color(0xFF9B6BF7)
internal val DarkBackdropWaveBody = Color(0xFF7C2DB8)
internal val DarkBackdropWaveDeep = Color(0xFF4A1158)
internal val DarkBackdropWaveLip = Color(0x59FFFFFF)
internal val DarkBackdropWaveSheen = Color(0x4DFFFFFF)
internal val DarkBackdropLightPearlCore = Color(0x24C4B5FD)
internal val DarkBackdropLightPearlRim = Color(0x3DC4B5FD)
internal val DarkBackdropLightPinkCore = Color(0x24F472B6)
internal val DarkBackdropLightPinkRim = Color(0x3DF472B6)
internal val DarkBackdropLightVioletCore = Color(0x298B5CF6)
internal val DarkBackdropLightVioletRim = Color(0x478B5CF6)

// ---------------------------------------------------------------------------
// The gloss finish on Home's and the game screen's containers (candy-backdrop.md §3.5.1).
// Consumed by `ui/theme/FinishColors.kt`.
// ---------------------------------------------------------------------------

internal val LightBackdropPanelRim = Color(0xE6FFFFFF)
internal val LightBackdropPanelLip = Color(0xF2FFFFFF)
internal val LightBackdropPanelGloss = Color(0xB3FFFFFF)
internal val LightBackdropPanelGlow = Color(0x4DDB2777)

internal val DarkBackdropPanelRim = Color(0x29FFFFFF)
internal val DarkBackdropPanelLip = Color(0x2EFFFFFF)
internal val DarkBackdropPanelGloss = Color(0x0FFFFFFF)
internal val DarkBackdropPanelGlow = Color(0x738B5CF6)

// ---------------------------------------------------------------------------
// The gel mode tiles (candy-backdrop.md §3.5.2): one value in both themes.
// Consumed by `ui/theme/FinishColors.kt`'s `GelColors`.
// ---------------------------------------------------------------------------

internal val GelVioletTop = Color(0xFF9566F2)
internal val GelVioletMid = Color(0xFF7C3AED)
internal val GelVioletBottom = Color(0xFF5B21B6)
internal val GelPinkTop = Color(0xFFE44593)
internal val GelPinkMid = Color(0xFFD61F6F)
internal val GelPinkBottom = Color(0xFFBE185D)
internal val OnGel = Color(0xFFFFFFFF)
internal val GelBadge = Color(0x38FFFFFF)
internal val GelChip = Color(0xF2FFFFFF)
internal val GelChipText = Color(0xFF831843)
internal val GelArtPeg = Color(0xE6FFFFFF)
internal val GelArtHole = Color(0x38FFFFFF)
internal val GelRim = Color(0x4DFFFFFF)
internal val GelLip = Color(0x61FFFFFF)
internal val GelSheen = Color(0x4DFFFFFF)
internal val GelGlowViolet = Color(0x8C5B21B6)
internal val GelGlowPink = Color(0x80BE185D)
