package com.app.pegsolitaire.ui.backdrop

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import com.app.pegsolitaire.testing.AppNavHostWithStandIns
import com.app.pegsolitaire.testing.CountingFrames
import com.app.pegsolitaire.testing.backdropSemantics
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * BD-23 (a) on the real host, as the tech-lead's A6 makes it exact: after Home's first frame the
 * backdrop draws the sky alone, is at rest and has awaited no frame; within two frames the waves and
 * the lights are there.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [FIRST_FRAME_ROBOLECTRIC_SDK])
class BackdropFirstFrameTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun homesFirstFrameDrawsOnlyTheSky() {
        val frames = CountingFrames()
        rule.mainClock.autoAdvance = false
        rule.setContent { AppNavHostWithStandIns(rememberNavController(), frames) }
        val first = rule.backdropSemantics()
        assertWithMessage("layers on frame 1").that(first[BackdropLayersKey]).containsExactly(BackdropLayer.Sky)
        assertWithMessage("motion on frame 1").that(first[BackdropMotionKey]).isEqualTo(BackdropMotionState.AtRest)
        assertWithMessage("frames awaited on frame 1").that(frames.awaited).isEqualTo(0)
        repeat(FRAMES_TO_SCENE) { rule.mainClock.advanceTimeByFrame() }
        assertWithMessage("layers within two frames")
            .that(rule.backdropSemantics()[BackdropLayersKey])
            .containsExactly(BackdropLayer.Sky, BackdropLayer.Waves, BackdropLayer.Lights)
    }
}

private const val FIRST_FRAME_ROBOLECTRIC_SDK = 34
private const val FRAMES_TO_SCENE = 2
