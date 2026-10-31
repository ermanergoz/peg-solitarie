package com.app.pegsolitaire.ui.backdrop

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import com.app.pegsolitaire.testing.AppNavHostWithStandIns
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
}

private const val REDUCED_MOTION_ROBOLECTRIC_SDK = 34
private const val IDLE_WATCH_MS = 5_000L
