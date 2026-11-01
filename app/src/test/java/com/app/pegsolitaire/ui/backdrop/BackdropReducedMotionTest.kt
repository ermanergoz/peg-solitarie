package com.app.pegsolitaire.ui.backdrop

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import com.app.pegsolitaire.testing.AppNavHostWithStandIns
import com.app.pegsolitaire.testing.BackdropClockProbe
import com.app.pegsolitaire.testing.CountingFrames
import com.app.pegsolitaire.testing.SystemMotionScale
import com.app.pegsolitaire.testing.backdropSemantics
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
            .isAtMost(IDLE_WATCH_MS)
        assertWithMessage("static").that(clock.isStatic).isFalse()
        val motion = rule.backdropSemantics()[BackdropMotionKey]
        assertWithMessage("motion").that(motion).isEqualTo(BackdropMotionState.Running)
    }
}

private const val REDUCED_MOTION_ROBOLECTRIC_SDK = 34
private const val IDLE_WATCH_MS = 5_000L

/** "Remove animations" stays on for this many frames: the auditor's probe, and more than one. */
private const val FRAMES_AT_SCALE_ZERO = 2

/** Frames for the recomposition to restart the loop once animations are back on. */
private const val FRAMES_TO_RESTART = 2

/** The frames around the toggle may add at most this much time: a few 16 ms frames. */
private const val RESUME_SLACK_MS = 100L
