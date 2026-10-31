package com.app.pegsolitaire.ui.backdrop

import android.app.Application
import android.content.Intent
import android.os.PowerManager
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.app.pegsolitaire.testing.AppNavHostWithStandIns
import com.app.pegsolitaire.testing.CountingFrames
import com.app.pegsolitaire.testing.StandInTags
import com.app.pegsolitaire.testing.backdropSemantics
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * BD-16 (c) under the tech-lead's A5 ruling, on the real host with frames from the test clock:
 * idle motion settles after the density's delay plus the 2 s ease and then awaits no frame; a tap,
 * a navigation and a resume each wake it; the tap still reaches the board; battery saver rests it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SETTLE_ROBOLECTRIC_SDK])
class BackdropSettleTest {
    @get:Rule
    val rule = createComposeRule()

    private val frames = CountingFrames()
    private val owner = TestLifecycleOwner()
    private var boardTaps = 0

    @Test
    fun theBackdropSettlesAfterTheIdleDelayAndThenAwaitsNoFrame() {
        start()
        assertMotion(BackdropMotionState.Running, "running on Home")
        rule.mainClock.advanceTimeBy(BackdropMotion.BACKDROP_HOME_IDLE_MS + SETTLED_AFTER_MS)
        assertMotion(BackdropMotionState.AtRest, "settled on Home")
        val awaited = frames.awaited
        rule.mainClock.advanceTimeBy(QUIET_WATCH_MS)
        assertWithMessage("frames awaited while settled").that(frames.awaited).isEqualTo(awaited)
    }

    @Test
    fun aTapWakesItAndStillReachesTheBoard() {
        start()
        rule.onNodeWithTag(StandInTags.HOME_CHALLENGE).performClick()
        rule.mainClock.advanceTimeBy(BackdropMotion.BACKDROP_GAME_IDLE_MS + SETTLED_AFTER_MS)
        assertMotion(BackdropMotionState.AtRest, "settled on the board")
        rule.onNodeWithTag(StandInTags.BOARD).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        assertWithMessage("the tap reached the board").that(boardTaps).isEqualTo(1)
        assertMotion(BackdropMotionState.Settling, "waking")
        rule.mainClock.advanceTimeBy(BackdropMotion.BACKDROP_SETTLE_MS + FRAME_SLACK_MS)
        assertMotion(BackdropMotionState.Running, "awake")
    }

    @Test
    fun aNavigationWakesASettledBackdrop() {
        start()
        rule.mainClock.advanceTimeBy(BackdropMotion.BACKDROP_HOME_IDLE_MS + SETTLED_AFTER_MS)
        assertMotion(BackdropMotionState.AtRest, "settled on Home")
        rule.onNodeWithTag(StandInTags.HOME_CHALLENGE).performClick()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        assertWithMessage("not at rest after navigating")
            .that(rule.backdropSemantics()[BackdropMotionKey])
            .isNotEqualTo(BackdropMotionState.AtRest)
    }

    @Test
    fun aResumeWakesASettledBackdrop() {
        start()
        rule.mainClock.advanceTimeBy(BackdropMotion.BACKDROP_HOME_IDLE_MS + SETTLED_AFTER_MS)
        assertMotion(BackdropMotionState.AtRest, "settled on Home")
        rule.runOnIdle { owner.move(Lifecycle.State.STARTED) }
        rule.mainClock.advanceTimeByFrame()
        rule.runOnIdle { owner.move(Lifecycle.State.RESUMED) }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        assertMotion(BackdropMotionState.Settling, "waking on resume")
    }

    @Test
    fun batterySaverRestsTheBackdrop() {
        start()
        assertMotion(BackdropMotionState.Running, "running on Home")
        val context = ApplicationProvider.getApplicationContext<Application>()
        rule.runOnIdle {
            shadowOf(context.getSystemService(PowerManager::class.java)).setIsPowerSaveMode(true)
            context.sendBroadcast(Intent(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED))
        }
        rule.waitForIdle()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        assertMotion(BackdropMotionState.AtRest, "battery saver")
        val awaited = frames.awaited
        rule.mainClock.advanceTimeBy(QUIET_WATCH_MS)
        assertWithMessage("frames awaited in battery saver").that(frames.awaited).isEqualTo(awaited)
    }

    private fun start() {
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                AppNavHostWithStandIns(rememberNavController(), frames, onBoardTap = { boardTaps++ })
            }
        }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
    }

    private fun assertMotion(
        expected: BackdropMotionState,
        where: String,
    ) {
        assertWithMessage(where).that(rule.backdropSemantics()[BackdropMotionKey]).isEqualTo(expected)
    }

    /** A lifecycle the test moves, standing in for the activity's pause and resume. */
    private class TestLifecycleOwner : LifecycleOwner {
        private val registry =
            LifecycleRegistry.createUnsafe(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry

        fun move(state: Lifecycle.State) {
            registry.currentState = state
        }
    }
}

private const val SETTLE_ROBOLECTRIC_SDK = 34
private const val SETTLED_AFTER_MS = 2_500L
private const val QUIET_WATCH_MS = 10_000L
private const val FRAME_SLACK_MS = 100L
