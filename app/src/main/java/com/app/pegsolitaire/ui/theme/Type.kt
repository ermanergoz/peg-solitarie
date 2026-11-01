package com.app.pegsolitaire.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * "Candy Clay" type scale: Fredoka (headings/display, Medium–SemiBold) + Nunito (body/labels,
 * Regular–Bold), bundled as static font files in `res/font` ([AppFonts]; DESIGN-SYSTEM.md
 * §Typography).
 *
 * The faces load blocking on first use, so the first frame is drawn in them on every device,
 * online or offline, with or without Play services, and nothing lays out again when they arrive.
 * The only fallback left is the platform's own, glyph by glyph, for a character the faces lack
 * (Fredoka has no Cyrillic, for one). Sizes and line heights stay the stock M3 scale (all sp live
 * inside `Typography()`, never inline — QUAL-03).
 */
internal val AppTypography = clayTypography()

/**
 * Opts a value/timer style into tabular (fixed-width) figures so ticking digits never jitter
 * horizontally (DESIGN-SYSTEM.md: tabular figures for timer/score). Nunito's digits are tabular by
 * design; Fredoka has no `tnum` feature, so the timer sets its digits in fixed cells as well
 * (`TimerDigits`).
 */
internal fun TextStyle.withTabularFigures(): TextStyle = copy(fontFeatureSettings = TABULAR_FIGURES_FEATURE)

/** The OpenType feature tag enabling tabular figures. */
private const val TABULAR_FIGURES_FEATURE = "tnum"

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

private fun TextStyle.heading(weight: FontWeight): TextStyle = copy(fontFamily = AppFonts.Fredoka, fontWeight = weight)

private fun TextStyle.body(weight: FontWeight): TextStyle = copy(fontFamily = AppFonts.Nunito, fontWeight = weight)
