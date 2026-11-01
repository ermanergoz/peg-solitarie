package com.app.pegsolitaire.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsPropertyKey

/**
 * The two looks of the board marks, the stars and the tutorial's done pip
 * (docs/ux/colour-vision.md CV-01): today's [Standard], and the opt-in [Colorblind] palette, whose
 * every distinction holds in luminance and pattern.
 */
enum class BoardMarkPalette { Standard, Colorblind }

/**
 * The colours of one palette in one theme, provided by `AppTheme` through [LocalBoardMarks] and read
 * by the palette's only consumers (§3.3): the interactive board, the Results star row, the
 * catalogue's star pips, and the tutorial's done pip and completion star. The geometry, alphas and
 * patterns that go with these colours are the palette's `BoardMarkRecipe`.
 *
 * @property palette which palette these colours belong to.
 * @property selectionRing the ring around the selected peg (`primary` in both palettes).
 * @property selectionHalo the band inside the selection ring (`surfaceContainerLowest`), or `null`
 *   when the palette draws none.
 * @property legalTarget the legal-target mark (`primary`; its alpha is the recipe's).
 * @property hintRing both hint rings and the connector (its alpha is the recipe's).
 * @property earnedStarFill the earned star's gradient stops.
 * @property earnedStarOutline the outline drawn over an earned star, or `null` for none.
 * @property unearnedStarOutline an unearned star's colour: the hollow star's band
 *   (`onSurfaceVariant` in both palettes, Q-027).
 * @property donePipFill the tutorial's done step pip's gradient stops.
 */
@Immutable
data class BoardMarks(
    val palette: BoardMarkPalette,
    val selectionRing: Color,
    val selectionHalo: Color?,
    val legalTarget: Color,
    val hintRing: Color,
    val earnedStarFill: List<Color>,
    val earnedStarOutline: Color?,
    val unearnedStarOutline: Color,
    val donePipFill: List<Color>,
)

/**
 * The palette in use, provided by `AppTheme`. **Dynamic** (`compositionLocalOf`), unlike
 * [LocalClayColors]: the palette changes at runtime from a Settings switch, and a dynamic local
 * recomposes only its readers on a flip — a static one would recompose everything under the theme.
 * Outside `AppTheme` it reads the standard light marks.
 */
val LocalBoardMarks = compositionLocalOf { boardMarksFor(BoardMarkPalette.Standard, darkTheme = false) }

/**
 * The marks of [palette] in the light or dark theme. Pure: it reads the colour schemes and the clay
 * values of `Color.kt` and `Clay.kt` and composes nothing, so a JVM test can call it. Composables
 * read the result through [LocalBoardMarks], never by calling this.
 *
 * The Standard marks are today's tokens but one: the unearned star takes the Colorblind palette's
 * `onSurfaceVariant`, the founder's Q-027 (option 2, "stars only"; the board is unchanged). The
 * Colorblind marks are §3.4's table, and its dark column adds no hex.
 */
internal fun boardMarksFor(
    palette: BoardMarkPalette,
    darkTheme: Boolean,
): BoardMarks {
    val scheme = if (darkTheme) DarkColors else LightColors
    val gold = (if (darkTheme) DarkClayColors else LightClayColors).goldGradient
    // §3.4's unearned-star colour, which the standard palette shares (Q-027): its hollow star in
    // `outlineVariant` read 1.20–1.50:1 and nearly vanished at 16dp (§12.6, F-CV4).
    val unearnedStar = scheme.onSurfaceVariant
    return when (palette) {
        BoardMarkPalette.Standard ->
            BoardMarks(
                palette = palette,
                selectionRing = scheme.primary,
                selectionHalo = null,
                legalTarget = scheme.primary,
                hintRing = scheme.tertiary,
                earnedStarFill = gold,
                earnedStarOutline = null,
                unearnedStarOutline = unearnedStar,
                donePipFill = gold,
            )
        BoardMarkPalette.Colorblind -> {
            val amberFill = if (darkTheme) gold else LightColorblindStarFillColors
            BoardMarks(
                palette = palette,
                selectionRing = scheme.primary,
                selectionHalo = scheme.surfaceContainerLowest,
                legalTarget = scheme.primary,
                hintRing = if (darkTheme) scheme.tertiary else LightColorblindHint,
                earnedStarFill = amberFill,
                earnedStarOutline = scheme.onTertiaryContainer,
                unearnedStarOutline = unearnedStar,
                donePipFill = amberFill,
            )
        }
    }
}

/**
 * Semantics: the palette the gameplay board draws in (CV-AC03) — a test hook in the `PegCountKey`
 * idiom, carried by the board-state node. TalkBack does not read a custom key.
 */
val BoardMarkPaletteKey = SemanticsPropertyKey<BoardMarkPalette>("BoardMarkPalette")
