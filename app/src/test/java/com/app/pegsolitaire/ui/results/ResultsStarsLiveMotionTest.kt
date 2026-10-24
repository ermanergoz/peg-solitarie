package com.app.pegsolitaire.ui.results

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.testing.SystemMotionScale
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * "Remove animations" turned on and off with the app resident (tech-lead-signoff.md §3): the next
 * Results takes the path the system scale says now. With animations off its earned stars are
 * settled from the first frame; with them on they pop in from nothing.
 *
 * Read off the pixels of the star row in the frame Results is first composed in: three earned
 * stars at scale 0 draw nothing over the screen's flat background, and settled they draw three
 * gold stars. The scale is turned live by [SystemMotionScale].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [RESULTS_LIVE_MOTION_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ResultsStarsLiveMotionTest {
    private val motion = SystemMotionScale()

    @get:Rule
    val composeRule = createComposeRule(effectContext = motion)

    private val resultsShown = mutableStateOf(false)

    @Test
    fun theNextResultsShowsItsStarsSettledWhileAnimationsAreOffAndPoppingOnceTheyAreBackOn() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppTheme {
                if (resultsShown.value) {
                    ResultsScreen(
                        state = threeStarResult,
                        onReplay = {},
                        onCatalog = {},
                        onHome = {},
                        onNextLevel = {},
                        onRetry = {},
                        onNavigateBack = {},
                    )
                }
            }
        }
        frame()

        assertWithMessage("star pixels in Results' first frame, animations on")
            .that(starPixelsInFirstFrame())
            .isEqualTo(0)

        motion.turnAnimationsOff()
        frame()
        assertWithMessage("star pixels in Results' first frame, animations off")
            .that(starPixelsInFirstFrame())
            .isAtLeast(SETTLED_STARS_MIN_PIXELS)

        motion.turnAnimationsOn()
        frame()
        assertWithMessage("star pixels in Results' first frame, animations back on")
            .that(starPixelsInFirstFrame())
            .isEqualTo(0)
    }

    /** Shows a fresh Results, counts what its star row draws in that first frame, and leaves it. */
    private fun starPixelsInFirstFrame(): Int {
        resultsShown.value = true
        frame()
        val pixels = drawnPixels(composeRule.renderedWindow())
        resultsShown.value = false
        frame()
        return pixels
    }

    /** Pixels of the star row that differ from the background at its corner. */
    private fun drawnPixels(window: Bitmap): Int {
        val row = composeRule.onNodeWithTag(RESULTS_STAR_ROW_TEST_TAG).fetchSemanticsNode().boundsInRoot
        val left = row.left.roundToInt()
        val top = row.top.roundToInt()
        val background = window.getPixel(left, top)
        var drawn = 0
        for (x in left until row.right.roundToInt()) {
            for (y in top until row.bottom.roundToInt()) {
                if (distance(window.getPixel(x, y), background) > PIXEL_NOISE) drawn++
            }
        }
        return drawn
    }

    private fun distance(
        first: Int,
        second: Int,
    ): Int = CHANNEL_SHIFTS.sumOf { shift -> abs((first shr shift and CHANNEL) - (second shr shift and CHANNEL)) }

    /** One frame, after the main thread has idled, so a state written since the last is composed. */
    private fun frame() {
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeByFrame()
    }

    private companion object {
        val threeStarResult =
            ResultsUiState(
                stars = 3,
                pegsRemaining = 1,
                elapsedSeconds = 75,
                score = 1280,
                headlineTier = ResultsHeadlineTier.PERFECT,
                replayId = ClassicLevelIds.ENGLISH33,
            )

        /** Three settled stars cover far more than this; nothing at scale 0 covers any. */
        const val SETTLED_STARS_MIN_PIXELS = 300

        /** Summed per-channel distance below which two pixels count as the same colour. */
        const val PIXEL_NOISE = 30
        val CHANNEL_SHIFTS = listOf(16, 8, 0)
        const val CHANNEL = 0xFF
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val RESULTS_LIVE_MOTION_SDK = 34
