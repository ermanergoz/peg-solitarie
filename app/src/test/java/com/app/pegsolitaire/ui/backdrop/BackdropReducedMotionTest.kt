package com.app.pegsolitaire.ui.backdrop

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.rememberNavController
import com.app.pegsolitaire.testing.AppNavHostWithStandIns
import com.app.pegsolitaire.testing.BackdropClockProbe
import com.app.pegsolitaire.testing.CountingFrames
import com.app.pegsolitaire.testing.StandInTags
import com.app.pegsolitaire.testing.SystemMotionScale
import com.app.pegsolitaire.testing.advanceOneFrame
import com.app.pegsolitaire.testing.backdropSemantics
import com.app.pegsolitaire.testing.framesToSceneOpen
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.testing.sceneAlphas
import com.app.pegsolitaire.ui.theme.ClayAnim
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * BD-16 (a): with "Remove animations" on, the backdrop draws its rest pose, reports itself at rest
 * and awaits no frame at all — the same picture, still (B-05a rule 4). The scale is turned through
 * [SystemMotionScale], the idiom every reduced-motion suite on `develop` uses.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [REDUCED_MOTION_ROBOLECTRIC_SDK])
class BackdropReducedMotionTest {
    private val motionScale = SystemMotionScale(SystemMotionScale.OFF)

    @get:Rule
    val rule = createComposeRule(effectContext = motionScale)

    @Test
    fun withAnimationsOffEveryElementIsAtRestAndNoClockRuns() {
        val frames = CountingFrames()
        rule.mainClock.autoAdvance = false
        rule.setContent { AppNavHostWithStandIns(rememberNavController(), frames) }
        rule.mainClock.advanceTimeBy(IDLE_WATCH_MS)
        val config = rule.backdropSemantics()
        assertWithMessage("layers").that(config[BackdropLayersKey]).contains(BackdropLayer.Waves)
        assertWithMessage("motion").that(config[BackdropMotionKey]).isEqualTo(BackdropMotionState.AtRest)
        assertWithMessage("frames awaited").that(frames.awaited).isEqualTo(0)
    }

    @Test
    fun turningAnimationsOnWithTheAppResidentStartsTheClock() {
        val frames = CountingFrames()
        rule.mainClock.autoAdvance = false
        rule.setContent { AppNavHostWithStandIns(rememberNavController(), frames) }
        rule.mainClock.advanceTimeBy(IDLE_WATCH_MS)
        motionScale.turnAnimationsOn()
        rule.waitForIdle()
        rule.mainClock.advanceTimeBy(IDLE_WATCH_MS)
        val motion = rule.backdropSemantics()[BackdropMotionKey]
        assertWithMessage("motion").that(motion).isEqualTo(BackdropMotionState.Running)
        assertWithMessage("frames awaited").that(frames.awaited).isGreaterThan(0)
    }

    /**
     * The order a resident player takes (the code-auditor's F1): animations on, "Remove animations"
     * on while the backdrop moves, then off again. The frames that run at scale 0 before the
     * recomposition stops the loop must not touch the clock's time, so it resumes from where it
     * stopped, finite, and moves again — and the backdrop that reports Running really is moving.
     */
    @Test
    fun turningAnimationsOffAndOnAgainWhileTheBackdropMovesResumesItsClock() {
        motionScale.turnAnimationsOn()
        val frames = CountingFrames()
        var clock: BackdropClock = StaticBackdropClock
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AppNavHostWithStandIns(rememberNavController(), frames, home = { BackdropClockProbe { clock = it } })
        }
        rule.mainClock.advanceTimeBy(IDLE_WATCH_MS)
        val before = clock.timeMs
        assertWithMessage("time while moving, before the toggle").that(before).isGreaterThan(0L)

        motionScale.turnAnimationsOff()
        repeat(FRAMES_AT_SCALE_ZERO) { rule.mainClock.advanceTimeByFrame() }
        motionScale.turnAnimationsOn()
        rule.waitForIdle()
        repeat(FRAMES_TO_RESTART) { rule.mainClock.advanceTimeByFrame() }
        val resumed = clock.timeMs
        rule.mainClock.advanceTimeBy(IDLE_WATCH_MS)
        val after = clock.timeMs

        assertWithMessage("time once animations are back on, against before the toggle ($before ms)")
            .that(resumed)
            .isAtLeast(before)
        assertWithMessage("time once animations are back on, no jump past before the toggle ($before ms)")
            .that(resumed)
            .isAtMost(before + RESUME_SLACK_MS)
        assertWithMessage("time moved over the next $IDLE_WATCH_MS ms (from $resumed ms)")
            .that(after - resumed)
            .isGreaterThan(0L)
        assertWithMessage("time moved over the next $IDLE_WATCH_MS ms, at most real time (from $resumed ms)")
            .that(after - resumed)
            .isAtMost(IDLE_WATCH_MS + TEST_FRAME_MS)
        assertWithMessage("static").that(clock.isStatic).isFalse()
        val motion = rule.backdropSemantics()[BackdropMotionKey]
        assertWithMessage("motion").that(motion).isEqualTo(BackdropMotionState.Running)
    }

    /**
     * The tech-lead's J-4b rider R-1 (tech design §16 #28): "Remove animations" turned on in the
     * middle of a density cross-fade, Home to the catalogue, settles it within two frames. Two frames
     * after the switch the window is pixel-identical, in the same run, to the catalogue a whole
     * cross-fade later; then the landscape slot beneath, the only one with a scene, is gone, so the
     * catalogue's wash was promoted and nothing is stranded under it.
     */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun turningAnimationsOffMidCrossFadeSettlesItWithinTwoFrames() {
        motionScale.turnAnimationsOn()
        rule.mainClock.autoAdvance = false
        rule.setContent { AppNavHostWithStandIns(rememberNavController(), CountingFrames()) }
        val frameMs = rule.mainClock.advanceOneFrame()
        rule.mainClock.advanceTimeBy(
            frameMs * (framesToSceneOpen(frameMs) + FRAMES_TO_SETTLE) + BackdropMotion.BACKDROP_SCENE_FADE_MS,
        )
        rule.onNodeWithTag(StandInTags.HOME_CATALOG).performClick()
        repeat(ClayAnim.NAV_TRANSITION_MS / HALVES / frameMs.toInt()) { rule.mainClock.advanceTimeByFrame() }
        val halfWay = rule.renderedWindow()

        motionScale.turnAnimationsOff()
        repeat(FRAMES_TO_SETTLE) { rule.mainClock.advanceTimeByFrame() }
        val twoFramesAfter = rule.renderedWindow()
        rule.mainClock.advanceTimeBy(ClayAnim.NAV_TRANSITION_MS.toLong())
        val settled = rule.renderedWindow()

        assertWithMessage("pixels half-way through the cross-fade that differ from the settled catalogue")
            .that(halfWay.differingPixels(settled))
            .isGreaterThan(0)
        assertWithMessage("pixels two frames after the switch that differ from the settled catalogue")
            .that(twoFramesAfter.differingPixels(settled))
            .isEqualTo(0)
        assertWithMessage("scenes still composed a cross-fade's length later").that(rule.sceneAlphas()).isEmpty()
        assertWithMessage("layers on the catalogue")
            .that(rule.backdropSemantics()[BackdropLayersKey])
            .containsExactly(BackdropLayer.Sky)
    }
}

private const val REDUCED_MOTION_ROBOLECTRIC_SDK = 34
private const val IDLE_WATCH_MS = 5_000L

/** "Remove animations" stays on for this many frames: the auditor's probe, and more than one. */
private const val FRAMES_AT_SCALE_ZERO = 2

/** Frames for the recomposition to restart the loop once animations are back on. */
private const val FRAMES_TO_RESTART = 2

/** One frame of the Compose test clock: a watch of N ms spans up to N plus one frame of the frame grid. */
private const val TEST_FRAME_MS = 16L

/** The frames around the toggle may add at most this much time: a few 16 ms frames. */
private const val RESUME_SLACK_MS = 100L

/** A running animation takes a new scale from its next frame, so a switch settles within two (§16 #28). */
private const val FRAMES_TO_SETTLE = 2

/** Half of the cross-fade: the switch lands in its middle. */
private const val HALVES = 2

/** How many pixels differ between two renders of the same window. */
private fun Bitmap.differingPixels(other: Bitmap): Int {
    val mine = IntArray(width * height).also { getPixels(it, 0, width, 0, 0, width, height) }
    val theirs = IntArray(width * height).also { other.getPixels(it, 0, width, 0, 0, width, height) }
    return mine.indices.count { mine[it] != theirs[it] }
}
