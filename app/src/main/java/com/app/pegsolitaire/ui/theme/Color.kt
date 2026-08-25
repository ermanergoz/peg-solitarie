package com.app.pegsolitaire.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * Material 3 baseline color schemes (light + dark).
 *
 * The Phase-1 contract references only M3 **role** names (`surface`, `secondary`,
 * `primary`, `outlineVariant`, …), so the default baseline palettes are used
 * verbatim — no hand-authored hex literals leak into the theme. Bespoke brand
 * theming / dynamic color is deferred to Slice 2 (POLI-01).
 */
internal val LightColors = lightColorScheme()

internal val DarkColors = darkColorScheme()
