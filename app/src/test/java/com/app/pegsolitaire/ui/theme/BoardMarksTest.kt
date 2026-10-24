package com.app.pegsolitaire.ui.theme

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The colours of the board marks, the stars and the done pip in each palette and theme
 * (docs/ux/colour-vision.md §3.4; CV-AC05, CV-13).
 *
 * Every expected colour is written here as the hex the spec names, not read back from
 * `Color.kt`, so a token that drifts fails this test rather than redefining what it checks.
 * A pure JVM test: [boardMarksFor] reads the colour schemes and clay values and composes nothing.
 */
class BoardMarksTest {
    /** CV-13: the standard palette is today's tokens — `primary`, `tertiary`, the gold gradient, `outlineVariant`. */
    @Test
    fun theStandardMarksAreTodaysTokensInTheLightTheme() {
        assertThat(boardMarksFor(BoardMarkPalette.Standard, darkTheme = false))
            .isEqualTo(
                BoardMarks(
                    palette = BoardMarkPalette.Standard,
                    selectionRing = Color(0xFF7C3AED),
                    selectionHalo = null,
                    legalTarget = Color(0xFF7C3AED),
                    hintRing = Color(0xFFF59E0B),
                    earnedStarFill = listOf(Color(0xFFFDE68A), Color(0xFFF59E0B)),
                    earnedStarOutline = null,
                    unearnedStarOutline = Color(0xFFDCD3EC),
                    donePipFill = listOf(Color(0xFFFDE68A), Color(0xFFF59E0B)),
                ),
            )
    }

    @Test
    fun theStandardMarksAreTodaysTokensInTheDarkTheme() {
        assertThat(boardMarksFor(BoardMarkPalette.Standard, darkTheme = true))
            .isEqualTo(
                BoardMarks(
                    palette = BoardMarkPalette.Standard,
                    selectionRing = Color(0xFFA78BFA),
                    selectionHalo = null,
                    legalTarget = Color(0xFFA78BFA),
                    hintRing = Color(0xFFFBBF24),
                    earnedStarFill = listOf(Color(0xFFFFF3C4), Color(0xFFFBBF24)),
                    earnedStarOutline = null,
                    unearnedStarOutline = Color(0xFF3A3350),
                    donePipFill = listOf(Color(0xFFFFF3C4), Color(0xFFFBBF24)),
                ),
            )
    }

    /** §3.4's light column: the halo, deep amber `#B45309`, the amber star fill, two dark outlines. */
    @Test
    fun theColourBlindMarksAreSection34sTableInTheLightTheme() {
        assertThat(boardMarksFor(BoardMarkPalette.Colorblind, darkTheme = false))
            .isEqualTo(
                BoardMarks(
                    palette = BoardMarkPalette.Colorblind,
                    selectionRing = Color(0xFF7C3AED),
                    selectionHalo = Color(0xFFFDFBFF),
                    legalTarget = Color(0xFF7C3AED),
                    hintRing = Color(0xFFB45309),
                    earnedStarFill = listOf(Color(0xFFC05A08), Color(0xFFB45309)),
                    earnedStarOutline = Color(0xFF6B4E00),
                    unearnedStarOutline = Color(0xFF635F69),
                    donePipFill = listOf(Color(0xFFC05A08), Color(0xFFB45309)),
                ),
            )
    }

    /** §3.4's dark column adds no hex: `tertiary`, the dark gold gradient and three existing roles. */
    @Test
    fun theColourBlindMarksAreSection34sTableInTheDarkTheme() {
        assertThat(boardMarksFor(BoardMarkPalette.Colorblind, darkTheme = true))
            .isEqualTo(
                BoardMarks(
                    palette = BoardMarkPalette.Colorblind,
                    selectionRing = Color(0xFFA78BFA),
                    selectionHalo = Color(0xFF15101F),
                    legalTarget = Color(0xFFA78BFA),
                    hintRing = Color(0xFFFBBF24),
                    earnedStarFill = listOf(Color(0xFFFFF3C4), Color(0xFFFBBF24)),
                    earnedStarOutline = Color(0xFFFFEFC7),
                    unearnedStarOutline = Color(0xFFA79FB5),
                    donePipFill = listOf(Color(0xFFFFF3C4), Color(0xFFFBBF24)),
                ),
            )
    }
}
