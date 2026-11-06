package com.app.pegsolitaire.navigation

import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.app.pegsolitaire.ui.window.RecordingSystemBars
import com.app.pegsolitaire.ui.window.SystemBarsState.Hidden
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Criterion 30b (docs/ux-spec.md D-05g part 2; docs/tech-design.md §3.8 (b)): whenever the activity
 * resumes while the board or Results is current — after an ad's own activity, a Play Games UI, the
 * launcher, the lock screen, the shade — the host asks for hidden bars again, at the resume itself,
 * before any frame. Every destination is immersive, so Home's resume asks for hidden bars too.
 *
 * The activity's lifecycle is a registry the test moves, provided as the composition's lifecycle
 * owner, as `BackdropSettleTest` does; the host's adapter sits outside the `NavHost`'s per-entry
 * content, so this owner is the one it observes. The test clock is held, so a request that appears
 * is the resume's and no frame's.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BARS_RESUME_ROBOLECTRIC_SDK])
class SystemBarsResumeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val bars = RecordingSystemBars()
    private val owner = TestLifecycleOwner()
    private lateinit var navController: NavHostController
    private val hostWindow: Window get() = composeRule.activity.window

    @Test
    fun aResumeOnTheBoardAsksForHiddenBarsAgainBeforeAnyFrame() {
        host()
        go(Gameplay(challengeLevel = CHALLENGE_LEVEL))

        assertTheResumeAsksForHiddenBars("the board")
    }

    @Test
    fun aResumeOnResultsAsksForHiddenBarsAgainBeforeAnyFrame() {
        host()
        go(Gameplay(challengeLevel = CHALLENGE_LEVEL))
        go(
            Results(
                levelId = "",
                stars = SAMPLE_STARS,
                pegsRemaining = SAMPLE_PEGS,
                elapsedSeconds = SAMPLE_SECONDS,
                score = SAMPLE_SCORE,
                challengeLevel = CHALLENGE_LEVEL,
            ),
        )

        assertTheResumeAsksForHiddenBars("Results")
    }

    @Test
    fun aResumeOnHomeAsksForHiddenBarsAgain() {
        host()

        assertTheResumeAsksForHiddenBars("Home")
    }

    /**
     * The other half of the re-assert: a destination composed while the activity is paused — the
     * next level composed behind an interstitial's own activity, say — asks for its bars in that
     * composition, not only at the next resume.
     */
    @Test
    fun aDestinationComposedWhileTheActivityIsPausedAsksForItsBarsAtOnce() {
        host()
        composeRule.runOnUiThread { owner.move(Lifecycle.State.STARTED) }
        composeRule.waitForIdle()
        bars.clear()

        go(Gameplay(challengeLevel = CHALLENGE_LEVEL))

        assertWithMessage("the bar requests made on the board while the activity was paused")
            .that(bars.records.map { it.state })
            .containsExactly(Hidden)
    }

    private fun assertTheResumeAsksForHiddenBars(where: String) {
        bars.clear()
        composeRule.mainClock.autoAdvance = false

        composeRule.runOnUiThread { owner.move(Lifecycle.State.STARTED) }
        assertWithMessage("bar requests made by the pause on $where").that(bars.records).isEmpty()
        composeRule.runOnUiThread { owner.move(Lifecycle.State.RESUMED) }

        assertWithMessage("the bar requests the resume on $where made, before any frame")
            .that(bars.records.map { it.state })
            .containsExactly(Hidden)
        assertWithMessage("the window the resume's request named, on $where")
            .that(bars.records.single().window)
            .isSameInstanceAs(hostWindow)
    }

    private fun host() {
        composeRule.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                navController = rememberNavController()
                AppNavHostWithBarStandIns(navController = navController, hostWindow = hostWindow, bars = bars)
            }
        }
        composeRule.waitForIdle()
    }

    private fun go(destination: Any) {
        composeRule.runOnUiThread { navController.navigate(destination) }
        composeRule.waitForIdle()
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

/** Robolectric has no android-all jar for Android 16 (SDK 36) on this machine; emulate SDK 34. */
private const val BARS_RESUME_ROBOLECTRIC_SDK = 34

private const val CHALLENGE_LEVEL = 7
private const val SAMPLE_STARS = 2
private const val SAMPLE_PEGS = 3
private const val SAMPLE_SECONDS = 75
private const val SAMPLE_SCORE = 1280
