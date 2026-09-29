package com.app.pegsolitaire.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/*
 * The type scale as DESIGN-SYSTEM.md §Typography and the tech-lead's font ruling give it, typed
 * here once for the tests that hold the app to it: every M3 role, the bundled face it is set in,
 * and that face's measured widths. Typed from the ruling, never read from the code under test.
 */

/** Fredoka sets the display, headline and title roles; Nunito the body and label roles. */
internal enum class TypeFamily { Fredoka, Nunito }

/**
 * The five bundled faces, and the advance width in each of every [PROBE_TEXTS] string, in units
 * per 1000 em. They are the ruling's T2 oracle: HarfBuzz 14.4.0 (`hb-shape --font-size=1000`) on
 * the files in `res/font`, kerning applied, re-derived on the committed files.
 */
internal enum class BundledFace(
    val family: TypeFamily,
    val weight: FontWeight,
    val probeWidthsPerMille: List<Int>,
) {
    FredokaMedium(TypeFamily.Fredoka, FontWeight.Medium, listOf(1552, 5349, 5686)),
    FredokaSemiBold(TypeFamily.Fredoka, FontWeight.SemiBold, listOf(1532, 5286, 5715)),
    NunitoRegular(TypeFamily.Nunito, FontWeight.Normal, listOf(2400, 6000, 5682)),
    NunitoSemiBold(TypeFamily.Nunito, FontWeight.SemiBold, listOf(2400, 6000, 5797)),
    NunitoBold(TypeFamily.Nunito, FontWeight.Bold, listOf(2400, 6000, 5937)),
}

/**
 * The three strings a face is told apart by. "1111" separates Fredoka from Roboto by about 70px
 * at 100sp; the ten digits separate Fredoka's two weights; the app's name separates Nunito's three.
 */
internal val PROBE_TEXTS = listOf("1111", "0123456789", "Peg Solitaire")

/** One M3 role: its name, the bundled face it is set in, and how to read it off a [Typography]. */
internal class TypeRole(
    val name: String,
    val face: BundledFace,
    val style: (Typography) -> TextStyle,
)

/** All fifteen M3 roles, each with the face and weight DESIGN-SYSTEM.md §Typography gives it. */
internal val TYPE_ROLES =
    listOf(
        TypeRole("displayLarge", BundledFace.FredokaSemiBold) { it.displayLarge },
        TypeRole("displayMedium", BundledFace.FredokaSemiBold) { it.displayMedium },
        TypeRole("displaySmall", BundledFace.FredokaSemiBold) { it.displaySmall },
        TypeRole("headlineLarge", BundledFace.FredokaSemiBold) { it.headlineLarge },
        TypeRole("headlineMedium", BundledFace.FredokaSemiBold) { it.headlineMedium },
        TypeRole("headlineSmall", BundledFace.FredokaMedium) { it.headlineSmall },
        TypeRole("titleLarge", BundledFace.FredokaMedium) { it.titleLarge },
        TypeRole("titleMedium", BundledFace.FredokaMedium) { it.titleMedium },
        TypeRole("titleSmall", BundledFace.FredokaMedium) { it.titleSmall },
        TypeRole("bodyLarge", BundledFace.NunitoRegular) { it.bodyLarge },
        TypeRole("bodyMedium", BundledFace.NunitoRegular) { it.bodyMedium },
        TypeRole("bodySmall", BundledFace.NunitoRegular) { it.bodySmall },
        TypeRole("labelLarge", BundledFace.NunitoBold) { it.labelLarge },
        TypeRole("labelMedium", BundledFace.NunitoSemiBold) { it.labelMedium },
        TypeRole("labelSmall", BundledFace.NunitoSemiBold) { it.labelSmall },
    )
