package com.app.pegsolitaire.ui.results

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.testing.SystemMotionScale
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.components.STAR_BAND_UNITS
import com.app.pegsolitaire.ui.components.STAR_CENTRE_UNITS
import com.app.pegsolitaire.ui.components.isBetweenStops
import com.app.pegsolitaire.ui.components.isColour
import com.app.pegsolitaire.ui.components.starPixel
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.BoardMarkPalette
import com.app.pegsolitaire.ui.theme.DarkColors
import com.app.pegsolitaire.ui.theme.LightColors
import com.app.pegsolitaire.ui.theme.boardMarksFor
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * What the Results star row draws in each palette (docs/ux/colour-vision.md §3.4, §12; CV-AC05 as
 * amended 2026-10-24, CV-AC10; code-auditor F-2).
 *
 * One `ResultsScreen` with two of three stars, rendered under the standard palette and then under
 * the colour-blind one **in the same run**, no stored golden (tech design §8.5). In both palettes
 * and both themes:
 * - an unearned star is the hollow star, so its centre shows the screen's background (CV-13a);
 * - an earned star's centre shows its fill: gold in the standard palette and in dark, amber in the
 *   light colour-blind palette;
 * - only the colour-blind palette outlines the earned star (§12.2: the standard earned star gets
 *   no outline);
 * - the row's `EarnedStarCountKey` does not move with the palette.
 *
 * A row that stopped drawing the active palette, or went back to today's filled unearned glyph,
 * fails here. Animations are off, so the stars have popped in and every frame is still.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [RESULTS_PALETTE_ROBOLECTRIC_SDK], qualifiers = "w400dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ResultsStarPaletteTest {
    private val motion = SystemMotionScale()

    @get:Rule
    val composeRule = createComposeRule(effectContext = motion)

    private val palette = mutableStateOf(BoardMarkPalette.Standard)

    @Before
    fun animationsOff() {
        motion.turnAnimationsOff()
    }

    @Test
    fun theStarRowDrawsTheActivePaletteInTheLightTheme() = assertTheRowDrawsBothPalettes(dark = false)

    @Test
    fun theStarRowDrawsTheActivePaletteInTheDarkTheme() = assertTheRowDrawsBothPalettes(dark = true)

    private fun assertTheRowDrawsBothPalettes(dark: Boolean) {
        var layout = RowLayout(starPx = 0f, glowPx = 0f, gapPx = 0f)
        composeRule.setContent {
            with(LocalDensity.current) {
                layout =
                    RowLayout(
                        starPx = dimensionResource(R.dimen.results_star_size).toPx(),
                        glowPx = dimensionResource(R.dimen.results_star_glow_size).toPx(),
                        gapPx = dimensionResource(R.dimen.spacing_xs).toPx(),
                    )
            }
            AppTheme(darkTheme = dark, boardMarkPalette = palette.value) {
                ResultsScreen(
                    state = TWO_OF_THREE_STARS,
                    onReplay = {},
                    onCatalog = {},
                    onHome = {},
                    onNextLevel = {},
                    onRetry = {},
                    onNavigateBack = {},
                )
            }
        }
        val standard = frameIn(BoardMarkPalette.Standard)
        val countUnderStandard = starRow().config[EarnedStarCountKey]
        val colorblind = frameIn(BoardMarkPalette.Colorblind)
        val countUnderColorblind = starRow().config[EarnedStarCountKey]
        val stars = layout.starBoxes(starRow().boundsInRoot)

        assertWithMessage("EarnedStarCountKey under the standard palette").that(countUnderStandard).isEqualTo(EARNED)
        assertWithMessage("EarnedStarCountKey across the flip").that(countUnderColorblind).isEqualTo(countUnderStandard)
        assertTheStars(colorblind, BoardMarkPalette.Colorblind, dark, stars)
        assertTheStars(standard, BoardMarkPalette.Standard, dark, stars)
    }

    /** The frame drawn with [shown] in effect. */
    private fun frameIn(shown: BoardMarkPalette): Bitmap {
        composeRule.runOnUiThread { palette.value = shown }
        composeRule.waitForIdle()
        return composeRule.renderedWindow()
    }

    private fun starRow(): SemanticsNode =
        composeRule.onNodeWithTag(RESULTS_STAR_ROW_TEST_TAG, useUnmergedTree = true).fetchSemanticsNode()

    private fun assertTheStars(
        frame: Bitmap,
        shown: BoardMarkPalette,
        dark: Boolean,
        stars: List<Rect>,
    ) {
        val marks = boardMarksFor(shown, dark)
        val background = (if (dark) DarkColors else LightColors).background
        val colourBlindOutline = checkNotNull(boardMarksFor(BoardMarkPalette.Colorblind, dark).earnedStarOutline)
        stars.forEachIndexed { index, box ->
            val centre = frame.starPixel(box, STAR_CENTRE_UNITS, STAR_CENTRE_UNITS)
            val band = frame.starPixel(box, STAR_CENTRE_UNITS, STAR_BAND_UNITS)
            if (index < EARNED) {
                assertWithMessage("$shown: earned star $index's centre shows its fill")
                    .that(isBetweenStops(centre, marks.earnedStarFill))
                    .isTrue()
                assertWithMessage("$shown: earned star $index is outlined only in the colour-blind palette")
                    .that(isColour(band, colourBlindOutline))
                    .isEqualTo(shown == BoardMarkPalette.Colorblind)
            } else {
                assertWithMessage("$shown: the unearned star's centre shows the background beneath it")
                    .that(isColour(centre, background))
                    .isTrue()
                assertWithMessage("$shown: the unearned star's band below its top tip")
                    .that(isColour(band, marks.unearnedStarOutline))
                    .isTrue()
            }
        }
    }

    /**
     * Where the row's stars draw their glyphs. An earned star is centred in its glow, which is wider
     * than the star; an unearned one is the star alone; the row centres them vertically.
     */
    private class RowLayout(
        val starPx: Float,
        val glowPx: Float,
        val gapPx: Float,
    ) {
        fun starBoxes(row: Rect): List<Rect> {
            val cells = List(TOTAL_STARS) { index -> if (index < EARNED) glowPx else starPx }
            assertWithMessage("the star row's width, as its cells and gaps add up")
                .that(cells.sum() + gapPx * (TOTAL_STARS - 1))
                .isWithin(ONE_PIXEL)
                .of(row.width)
            var left = row.left
            return cells.map { cell ->
                val inset = (cell - starPx) / 2
                val top = row.top + (row.height - cell) / 2 + inset
                Rect(left + inset, top, left + inset + starPx, top + starPx).also { left += cell + gapPx }
            }
        }
    }
}

/** A 2-star Classic result: two earned stars, then one unearned. */
private val TWO_OF_THREE_STARS =
    ResultsUiState(
        stars = EARNED,
        pegsRemaining = 5,
        elapsedSeconds = 75,
        score = 1280,
        headlineTier = ResultsHeadlineTier.SOLVED,
        replayId = ClassicLevelIds.ENGLISH33,
    )

private const val EARNED = 2
private const val TOTAL_STARS = 3

/** Layout positions are whole pixels at xxhdpi; the row's measured width may round by one. */
private const val ONE_PIXEL = 1f

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val RESULTS_PALETTE_ROBOLECTRIC_SDK = 34
