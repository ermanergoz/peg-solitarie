package com.app.pegsolitaire.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontWeight
import com.app.pegsolitaire.R

/**
 * The app's two type families, bundled as static OFL font files in `res/font` (DESIGN-SYSTEM.md
 * §Typography; tech-lead font ruling, 2026-09-29).
 *
 * ## Bundled, and loaded before the first frame
 *
 * Every font is a packaged file loaded [FontLoadingStrategy.Blocking] the first time a role asks
 * for it, so the first frame is already drawn in the real faces on every device, online or not,
 * with or without Play services, and nothing lays out again when a face arrives. The default
 * strategy is Blocking; it is written out so `AppTypographyTest` can hold every font to it.
 *
 * Only the five weights the type scale uses are shipped: Fredoka Medium and SemiBold, and Nunito
 * Regular, SemiBold and Bold. Each role asks for one of them exactly, so no weight is matched to a
 * neighbour or synthesised. The notices of both families ship in `assets/licenses/`.
 *
 * ## Why an object in its own file
 *
 * An object is initialised on first access, and a reference to it from another file has no
 * declaration order to get wrong. The type scale used to be declared above its two families in
 * one file, so it was built while both were still `null`, and every role fell back to the
 * platform face on every device.
 */
internal object AppFonts {
    /** Fredoka — the rounded, chunky face of the display, headline and title roles. */
    val Fredoka: FontFamily =
        FontFamily(
            Font(R.font.fredoka_medium, FontWeight.Medium, loadingStrategy = FontLoadingStrategy.Blocking),
            Font(R.font.fredoka_semibold, FontWeight.SemiBold, loadingStrategy = FontLoadingStrategy.Blocking),
        )

    /** Nunito — the friendly rounded face of the body and label roles. */
    val Nunito: FontFamily =
        FontFamily(
            Font(R.font.nunito_regular, FontWeight.Normal, loadingStrategy = FontLoadingStrategy.Blocking),
            Font(R.font.nunito_semibold, FontWeight.SemiBold, loadingStrategy = FontLoadingStrategy.Blocking),
            Font(R.font.nunito_bold, FontWeight.Bold, loadingStrategy = FontLoadingStrategy.Blocking),
        )
}
