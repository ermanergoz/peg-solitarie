package com.app.pegsolitaire.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.app.pegsolitaire.R

/**
 * "Candy Clay" type scale: Fredoka (headings/display, Medium–SemiBold) + Nunito
 * (body/labels, Regular–Bold) via downloadable Google Fonts (DESIGN-SYSTEM.md
 * §Typography).
 *
 * The fonts are resolved asynchronously through the GMS font provider
 * ([FontProviderSpec]); Compose's async font pipeline renders with
 * [FontFamily.Default] until a font arrives and **falls back to it permanently
 * when resolution fails** (offline, no GMS, Robolectric) — a failed fetch can
 * therefore restyle text but never crash. Sizes/line heights stay the stock M3
 * scale (all sp live inside `Typography()`, never inline — QUAL-03).
 */
internal val AppTypography = clayTypography()

/** Fredoka — rounded, chunky display face for headings and titles. */
internal val FredokaFamily = googleFontFamily(FREDOKA_FONT, FontWeight.Medium, FontWeight.SemiBold)

/** Nunito — friendly rounded body face for body copy and labels. */
internal val NunitoFamily =
    googleFontFamily(
        NUNITO_FONT,
        FontWeight.Normal,
        FontWeight.Medium,
        FontWeight.SemiBold,
        FontWeight.Bold,
    )

/**
 * Opts a value/timer style into tabular (fixed-width) figures so ticking digits
 * never jitter horizontally (DESIGN-SYSTEM.md: tabular figures for timer/score).
 */
internal fun TextStyle.withTabularFigures(): TextStyle = copy(fontFeatureSettings = TABULAR_FIGURES_FEATURE)

/** The OpenType feature tag enabling tabular figures. */
private const val TABULAR_FIGURES_FEATURE = "tnum"

/** Downloadable-font names served by the GMS provider. */
private const val FREDOKA_FONT = "Fredoka"
private const val NUNITO_FONT = "Nunito"

/** The Google Play services downloadable-font provider (certs via `R.array`). */
private object FontProviderSpec {
    const val AUTHORITY = "com.google.android.gms.fonts"
    const val PACKAGE = "com.google.android.gms"

    val provider =
        GoogleFont.Provider(
            providerAuthority = AUTHORITY,
            providerPackage = PACKAGE,
            certificates = R.array.com_google_android_gms_fonts_certs,
        )
}

/** A [FontFamily] of one downloadable [GoogleFont] at each requested weight. */
private fun googleFontFamily(
    name: String,
    vararg weights: FontWeight,
): FontFamily =
    FontFamily(
        weights.map { weight ->
            Font(googleFont = GoogleFont(name), fontProvider = FontProviderSpec.provider, weight = weight)
        },
    )

/**
 * Maps the two families onto the M3 roles: display/headline/title = Fredoka
 * (playful clay headers), body/label = Nunito. Weights follow the contract's
 * Fredoka 500–600 / Nunito 400–700 bands.
 */
private fun clayTypography(): Typography {
    val defaults = Typography()
    return Typography(
        displayLarge = defaults.displayLarge.heading(FontWeight.SemiBold),
        displayMedium = defaults.displayMedium.heading(FontWeight.SemiBold),
        displaySmall = defaults.displaySmall.heading(FontWeight.SemiBold),
        headlineLarge = defaults.headlineLarge.heading(FontWeight.SemiBold),
        headlineMedium = defaults.headlineMedium.heading(FontWeight.SemiBold),
        headlineSmall = defaults.headlineSmall.heading(FontWeight.Medium),
        titleLarge = defaults.titleLarge.heading(FontWeight.Medium),
        titleMedium = defaults.titleMedium.heading(FontWeight.Medium),
        titleSmall = defaults.titleSmall.heading(FontWeight.Medium),
        bodyLarge = defaults.bodyLarge.body(FontWeight.Normal),
        bodyMedium = defaults.bodyMedium.body(FontWeight.Normal),
        bodySmall = defaults.bodySmall.body(FontWeight.Normal),
        labelLarge = defaults.labelLarge.body(FontWeight.Bold),
        labelMedium = defaults.labelMedium.body(FontWeight.SemiBold),
        labelSmall = defaults.labelSmall.body(FontWeight.SemiBold),
    )
}

private fun TextStyle.heading(weight: FontWeight): TextStyle = copy(fontFamily = FredokaFamily, fontWeight = weight)

private fun TextStyle.body(weight: FontWeight): TextStyle = copy(fontFamily = NunitoFamily, fontWeight = weight)
