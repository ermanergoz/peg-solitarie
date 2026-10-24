package com.app.pegsolitaire.ui.game

import com.app.pegsolitaire.ui.theme.BoardMarkPalette
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * The board marks of each palette as **data** (docs/ux/colour-vision.md §3.4; CV-AC05, CV-AC08).
 *
 * The `DockLookRecipeTest` idiom: the board and the star glyph take every mark's geometry, alpha
 * and pattern from [BoardMarkPalette.recipe] and nothing else, so a recipe that passes here is a
 * board whose marks differ in something other than hue. A pure JVM test — no composition, no
 * Robolectric — because the property is about the mapping, not about any one frame of it.
 */
class BoardMarkRecipeTest {
    /** CV-13: with the option off the board draws exactly what it drew before the option existed. */
    @Test
    fun theStandardRecipeIsTodaysBoardMetrics() {
        val standard = BoardMarkPalette.Standard.recipe

        assertThat(standard.selectedRingRatio).isEqualTo(BoardMetrics.SELECTED_RING_RATIO)
        assertThat(standard.hintSourceRingRatio).isEqualTo(BoardMetrics.HINT_SOURCE_RING_RATIO)
        assertThat(standard.hintTargetRingRatio).isEqualTo(BoardMetrics.HINT_TARGET_RING_RATIO)
        assertThat(standard.destinationDotAlpha).isEqualTo(BoardMetrics.DESTINATION_DOT_ALPHA)
        assertThat(standard.hintRingAlpha).isEqualTo(BoardMetrics.HINT_HIGHLIGHT_ALPHA)
        assertThat(standard.hintConnectorAlpha).isEqualTo(BoardMetrics.HINT_ARROW_ALPHA)
        assertThat(standard.hintRingDashes).isEqualTo(SOLID)
        assertThat(standard.halo).isFalse()
        // CV-13a: the unearned star is the hollow star in both palettes, so earned and unearned
        // stars never differ by colour alone; it is the standard palette's only change of form.
        assertThat(standard.unearnedStarHollow).isTrue()
    }

    /** CV-07 to CV-10: every value of §3.4's colour-blind column, written here rather than read back. */
    @Test
    fun theColourBlindRecipeIsSection34() {
        val colorblind = BoardMarkPalette.Colorblind.recipe

        assertThat(colorblind.selectedRingRatio).isEqualTo(0.42f)
        assertThat(colorblind.hintSourceRingRatio).isEqualTo(0.44f)
        assertThat(colorblind.hintTargetRingRatio).isEqualTo(0.30f)
        assertThat(colorblind.destinationDotAlpha).isEqualTo(1f)
        assertThat(colorblind.hintRingAlpha).isEqualTo(1f)
        assertThat(colorblind.hintConnectorAlpha).isEqualTo(0.5f)
        assertThat(colorblind.hintRingDashes).isEqualTo(8)
        assertThat(colorblind.halo).isTrue()
        assertThat(colorblind.unearnedStarHollow).isTrue()
    }

    /**
     * CV-AC08, on the board: each of the ten pairs of the five marks that can share a board differs
     * in form, radius, dashes, halo or fill — never in hue alone — in both palettes. It proves that
     * some channel other than hue separates each pair, not which one: a pair that still differs in
     * another channel passes. A colour-blind hint ring that lost its dashes still differs from the
     * selection ring by radius and halo here, so [theColourBlindRecipeIsSection34]'s dash count and
     * `BoardCanvasPaletteTest`'s gap samples are what catch that (code-auditor F-4). What this test
     * catches is a pair left with hue alone: the standard hint source ring at the selection ring's
     * radius, say, or the wrong-tap mark without its dashes.
     */
    @Test
    fun everyPairOfMarksThatCanShareABoardDiffersInAChannelThatIsNotHue() {
        val pairs = MARKS_ON_THE_BOARD.pairs()

        assertThat(pairs).hasSize(TEN_PAIRS)
        BoardMarkPalette.entries.forEach { palette ->
            pairs.forEach { (first, second) ->
                assertWithMessage("$palette: $first against $second")
                    .that(palette.recipe.look(first))
                    .isNotEqualTo(palette.recipe.look(second))
            }
        }
    }

    /**
     * CV-AC08 as amended 2026-10-24, on the star rows: earned (filled) against unearned (hollow), in
     * both palettes. CV-13a makes the standard palette's unearned star hollow too, so neither
     * palette's stars differ by colour alone.
     */
    @Test
    fun earnedAndUnearnedStarsDifferInAChannelThatIsNotHueInBothPalettes() {
        BoardMarkPalette.entries.forEach { palette ->
            assertWithMessage("$palette: the earned star")
                .that(palette.recipe.look(BoardMark.EarnedStar).filled)
                .isTrue()
            assertWithMessage("$palette: the unearned star")
                .that(palette.recipe.look(BoardMark.UnearnedStar).filled)
                .isFalse()
        }
    }

    /** §3.4: the hint source ring stays strictly larger than the selection ring, as `BoardCanvasTest` asks today. */
    @Test
    fun aHintRingIsLargerThanTheSelectionRingInBothPalettes() {
        BoardMarkPalette.entries.forEach { palette ->
            assertWithMessage("$palette")
                .that(palette.recipe.hintSourceRingRatio)
                .isGreaterThan(palette.recipe.selectedRingRatio)
        }
    }

    private fun List<BoardMark>.pairs(): List<Pair<BoardMark, BoardMark>> =
        flatMapIndexed { index, first -> drop(index + 1).map { second -> first to second } }
}

/** The marks drawn on the board itself; the two stars live on the star rows instead. */
private val MARKS_ON_THE_BOARD =
    listOf(
        BoardMark.SelectionRing,
        BoardMark.LegalTarget,
        BoardMark.HintSourceRing,
        BoardMark.HintTargetRing,
        BoardMark.WrongTapMark,
    )

/** Five board marks taken two at a time. */
private const val TEN_PAIRS = 10

/** A ring with no dash pattern. */
private const val SOLID = 0
