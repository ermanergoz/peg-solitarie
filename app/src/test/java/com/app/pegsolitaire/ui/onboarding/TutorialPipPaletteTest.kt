package com.app.pegsolitaire.ui.onboarding

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import com.app.pegsolitaire.R
import com.app.pegsolitaire.testing.SystemMotionScale
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.components.BOARD_CANVAS_TEST_TAG
import com.app.pegsolitaire.ui.components.isBetweenStops
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.BoardMarkPalette
import com.app.pegsolitaire.ui.theme.boardMarksFor
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

/**
 * What the tutorial's done step pip and its completion star draw in each palette
 * (docs/ux/colour-vision.md §3.3, §3.4; CV-AC05, CV-AC10; code-auditor F-2).
 *
 * The real `TutorialScreen`, rendered under the standard palette and then under the colour-blind
 * one **in the same run**, no stored golden:
 * - on step 2 the first pip is done, and its centre is the palette's done-pip fill: gold in the
 *   standard palette, amber in the light colour-blind one, gold again in dark (§3.4's dark column
 *   keeps `GoldGradient`); the pip row's "Step 2 of 3" does not move with the palette;
 * - once the third step is cleared, the completion star below the board is the one thing there the
 *   palette changes, and its centre is gold in the standard palette and amber in the light
 *   colour-blind one. A star that stopped reading the palette changes nothing below the board, and
 *   fails here.
 *
 * Animations are off, so the star has popped in and every frame is still.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [TUTORIAL_PALETTE_ROBOLECTRIC_SDK], qualifiers = "w400dp-h800dp-xxhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TutorialPipPaletteTest {
    private val motion = SystemMotionScale()

    @get:Rule
    val composeRule = createComposeRule(effectContext = motion)

    private val palette = mutableStateOf(BoardMarkPalette.Standard)

    @Before
    fun animationsOff() {
        motion.turnAnimationsOff()
    }

    @Test
    fun theDonePipDrawsTheActivePaletteInTheLightTheme() = assertTheDonePipDrawsBothPalettes(dark = false)

    @Test
    fun theDonePipDrawsTheActivePaletteInTheDarkTheme() = assertTheDonePipDrawsBothPalettes(dark = true)

    @Test
    fun theCompletionStarDrawsTheActivePaletteInTheLightTheme() {
        var starPx = 0f
        show(TutorialUiState.forStep(LAST_STEP, HAPTICS_OFF, NO_WRONG_TAP).completed(), dark = false) {
            starPx = it.starPx
        }
        val standard = frameIn(BoardMarkPalette.Standard)
        val colorblind = frameIn(BoardMarkPalette.Colorblind)
        val board = composeRule.onNodeWithTag(BOARD_CANVAS_TEST_TAG).fetchSemanticsNode().boundsInRoot

        val star = differingBox(standard, colorblind, fromY = board.bottom.roundToInt())
        assertWithMessage("pixels below the board that the palette changed (the completion star)")
            .that(star)
            .isNotNull()
        checkNotNull(star)
        assertWithMessage("the changed region's width, against the star's").that(star.width).isAtMost(starPx)
        assertWithMessage("the changed region's height, against the star's").that(star.height).isAtMost(starPx)
        assertWithMessage("the changed region's width, against half the star's")
            .that(star.width)
            .isAtLeast(starPx / 2)
        val x = star.center.x.roundToInt()
        val y = star.center.y.roundToInt()
        val standardFill = boardMarksFor(BoardMarkPalette.Standard, darkTheme = false).earnedStarFill
        val colourBlindFill = boardMarksFor(BoardMarkPalette.Colorblind, darkTheme = false).earnedStarFill
        assertWithMessage("standard: the completion star's centre shows its fill")
            .that(isBetweenStops(standard.getPixel(x, y), standardFill))
            .isTrue()
        assertWithMessage("colour-blind: the completion star's centre shows its fill")
            .that(isBetweenStops(colorblind.getPixel(x, y), colourBlindFill))
            .isTrue()
    }

    private fun assertTheDonePipDrawsBothPalettes(dark: Boolean) {
        var pipPx = 0f
        var gapPx = 0f
        val state = TutorialUiState.forStep(SECOND_STEP, HAPTICS_OFF, NO_WRONG_TAP)
        show(state, dark) {
            pipPx = it.pipPx
            gapPx = it.pipGapPx
        }
        val standard = frameIn(BoardMarkPalette.Standard)
        val spokenUnderStandard = pipRow(state).config[SemanticsProperties.ContentDescription]
        val colorblind = frameIn(BoardMarkPalette.Colorblind)
        val spokenUnderColorblind = pipRow(state).config[SemanticsProperties.ContentDescription]
        val row = pipRow(state).boundsInRoot
        val pips = TutorialUiState.STEP_COUNT
        assertWithMessage("the pip row's width, as its pips and gaps add up")
            .that(pipPx * pips + gapPx * (pips - 1))
            .isWithin(ONE_PIXEL)
            .of(row.width)
        // The first pip is the done one on step 2; its centre is half a pip in from the row's corner.
        val x = (row.left + pipPx / 2).roundToInt()
        val y = (row.top + pipPx / 2).roundToInt()

        assertWithMessage("the pip row's description across the flip")
            .that(spokenUnderColorblind)
            .isEqualTo(spokenUnderStandard)
        BoardMarkPalette.entries.zip(listOf(standard, colorblind)).forEach { (shown, frame) ->
            assertWithMessage("$shown: the done pip's centre shows the palette's done-pip fill")
                .that(isBetweenStops(frame.getPixel(x, y), boardMarksFor(shown, dark).donePipFill))
                .isTrue()
        }
    }

    /** The tutorial in [state], in the light or dark theme, with the sizes [sizes] reports read in composition. */
    private fun show(
        state: TutorialUiState,
        dark: Boolean,
        sizes: (Sizes) -> Unit,
    ) {
        composeRule.setContent {
            with(LocalDensity.current) {
                sizes(
                    Sizes(
                        pipPx = dimensionResource(R.dimen.tutorial_step_pip_size).toPx(),
                        pipGapPx = dimensionResource(R.dimen.spacing_sm).toPx(),
                        starPx = dimensionResource(R.dimen.results_star_size).toPx(),
                    ),
                )
            }
            AppTheme(darkTheme = dark, boardMarkPalette = palette.value) {
                TutorialScreen(state = state, onCellTap = {}, onAnimationFinished = {}, onSkip = {}, onDone = {})
            }
        }
    }

    /** The frame drawn with [shown] in effect. */
    private fun frameIn(shown: BoardMarkPalette): Bitmap {
        composeRule.runOnUiThread { palette.value = shown }
        composeRule.waitForIdle()
        return composeRule.renderedWindow()
    }

    /** The pip row, found by what it speaks on [state]'s step ("Step 2 of 3"). */
    private fun pipRow(state: TutorialUiState): SemanticsNode =
        composeRule
            .onNodeWithContentDescription(
                RuntimeEnvironment
                    .getApplication()
                    .getString(R.string.tutorial_step_progress, state.stepNumber, TutorialUiState.STEP_COUNT),
                useUnmergedTree = true,
            ).fetchSemanticsNode()

    /** The smallest box holding every pixel at or below [fromY] where [first] and [second] differ, or `null`. */
    private fun differingBox(
        first: Bitmap,
        second: Bitmap,
        fromY: Int,
    ): Rect? {
        var left = Int.MAX_VALUE
        var top = Int.MAX_VALUE
        var right = Int.MIN_VALUE
        var bottom = Int.MIN_VALUE
        for (y in fromY until first.height) {
            for (x in 0 until first.width) {
                if (first.getPixel(x, y) != second.getPixel(x, y)) {
                    left = minOf(left, x)
                    top = minOf(top, y)
                    right = maxOf(right, x + 1)
                    bottom = maxOf(bottom, y + 1)
                }
            }
        }
        return if (right == Int.MIN_VALUE) {
            null
        } else {
            Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
        }
    }

    /** The sizes the tests read off the resources, in pixels at the test's density. */
    private class Sizes(
        val pipPx: Float,
        val pipGapPx: Float,
        val starPx: Float,
    )
}

/** Step 2 (0-based 1): its first pip is done. */
private const val SECOND_STEP = 1

/** Step 3 (0-based 2), the last; cleared, it raises the completion star. */
private const val LAST_STEP = 2

private const val HAPTICS_OFF = false
private const val NO_WRONG_TAP = 0

/** Layout positions are whole pixels at xxhdpi; the row's measured width may round by one. */
private const val ONE_PIXEL = 1f

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val TUTORIAL_PALETTE_ROBOLECTRIC_SDK = 34
