package com.app.pegsolitaire.ui.backdrop

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.app.pegsolitaire.testing.AppNavHostWithStandIns
import com.app.pegsolitaire.testing.StandInTags
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.theme.ClayAnim
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * BD-21 in the convex-blend form the tech-lead's R5 and D6 give it: on Home → catalogue → board →
 * Results → Home, and Home → board → Home,
 * every frame of every transition is rendered, and at each sample point — the calm
 * band, and the sky between the band and the back crest, away from every wave and light — the
 * colour lies on the segment between that pixel's outgoing and incoming colours. Only a blend is
 * allowed: the sky never dips or lightens toward the background mid-transition.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SKY_ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SkyContinuityTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun aDestinationChangeOnlyEverBlendsTheSky() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            navController = rememberNavController()
            AppNavHostWithStandIns(navController, InfiniteBackdropFrames)
        }
        rule.mainClock.advanceTimeBy(SETTLE_MS)
        listOf(StandInTags.HOME_CATALOG, StandInTags.CATALOG, StandInTags.BOARD_FINISH, StandInTags.RESULTS_HOME)
            .forEach { tag -> assertBlendsOnly(tag) { rule.onNodeWithTag(tag).performClick() } }
        // Home and the board share their sky, so only a blend leaves it untouched: the one pair on which a
        // dip toward the background would show however small it is.
        assertBlendsOnly("Home to the board") { rule.onNodeWithTag(StandInTags.HOME_CHALLENGE).performClick() }
        assertBlendsOnly("the board back to Home") { rule.runOnIdle { navController.popBackStack() } }
    }

    private lateinit var navController: NavHostController

    private fun assertBlendsOnly(
        tag: String,
        navigate: () -> Unit,
    ) {
        val before = samples(rule.renderedWindow())
        navigate()
        val frames = mutableListOf<IntArray>()
        repeat(TRANSITION_FRAMES) {
            rule.mainClock.advanceTimeByFrame()
            frames += samples(rule.renderedWindow())
        }
        rule.mainClock.advanceTimeBy(SETTLE_MS)
        val after = samples(rule.renderedWindow())
        frames.forEachIndexed { frame, colours ->
            colours.indices.forEach { i ->
                val blended = onSegment(before[i], after[i], colours[i])
                assertWithMessage("$tag frame $frame point $i").that(blended).isTrue()
            }
        }
    }

    private fun samples(window: Bitmap): IntArray =
        POINTS
            .map { (fx, fy) -> window.getPixel((fx * (window.width - 1)).toInt(), (fy * (window.height - 1)).toInt()) }
            .toIntArray()

    /** Whether [mid] is a blend of [from] and [to], within [TOLERANCE] per channel. */
    private fun onSegment(
        from: Int,
        to: Int,
        mid: Int,
    ): Boolean {
        val a = channels(from)
        val b = channels(to)
        val m = channels(mid)
        val widest = a.indices.maxBy { abs(b[it] - a[it]) }
        val span = b[widest] - a[widest]
        val t = if (span == 0) 0f else ((m[widest] - a[widest]).toFloat() / span).coerceIn(0f, 1f)
        return a.indices.all { abs(a[it] + (b[it] - a[it]) * t - m[it]) <= TOLERANCE }
    }

    private fun channels(argb: Int): IntArray =
        intArrayOf((argb shr RED) and BYTE, (argb shr GREEN) and BYTE, argb and BYTE)

    private companion object {
        /** The calm band, and the sky between it and the back crest, in a lane gap (see the spec's §3.3 extents). */
        val POINTS = listOf(0.50f to 0.05f, 0.33f to 0.25f, 0.33f to 0.40f)
        const val SETTLE_MS = 1_500L
        val TRANSITION_FRAMES = ClayAnim.NAV_TRANSITION_MS / FRAME_MS + 2
        const val TOLERANCE = 2f
        const val RED = 16
        const val GREEN = 8
        const val BYTE = 0xFF
    }
}

private const val SKY_ROBOLECTRIC_SDK = 34
private const val FRAME_MS = 16
