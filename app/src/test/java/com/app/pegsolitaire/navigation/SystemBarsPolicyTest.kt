package com.app.pegsolitaire.navigation

import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.pegsolitaire.ui.window.LocalSystemBars
import com.app.pegsolitaire.ui.window.RecordingSystemBars
import com.app.pegsolitaire.ui.window.SystemBarsState
import com.app.pegsolitaire.ui.window.SystemBarsState.Hidden
import com.app.pegsolitaire.ui.window.SystemBarsState.Shown
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Criterion 30a (docs/ux-spec.md D-05g; docs/tech-design.md §3.8 (a)): one bar policy for the whole
 * graph, applied by the host to its window in the frame a destination is first composed. The whole app
 * is immersive (founder 2026-11-05): every destination, and any destination added later, asks for
 * [Hidden]; a restored back stack whose top is the board asks for [Hidden] first.
 *
 * The host is the real `AppNavHost` over stand-in destinations ([AppNavHostWithBarStandIns]); the
 * seam is the recording fake, so what is read is what the host asked of its window, in order.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [BARS_POLICY_ROBOLECTRIC_SDK])
class SystemBarsPolicyTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val bars = RecordingSystemBars()
    private lateinit var navController: NavHostController
    private val hostWindow: Window get() = composeRule.activity.window

    @Test
    fun homeAsksForHiddenBarsOnTheHostsFirstComposition() {
        host()

        assertWithMessage("the host's first bar request").that(bars.records.firstOrNull()?.state).isEqualTo(Hidden)
        assertWithMessage("every bar request on Home")
            .that(bars.records.map { it.state }.toSet())
            .containsExactly(Hidden)
        assertWithMessage("the window every request named")
            .that(bars.records.map { it.window }.toSet())
            .containsExactly(hostWindow)
    }

    @Test
    fun gameplayResultsAndHomeAllHideTheBars() {
        host()

        go(Gameplay(levelId = CLASSIC_BOARD))
        assertLastRequest(Hidden, "a Classic board")
        go(sampleResults(challengeLevel = null))
        assertLastRequest(Hidden, "Results")
        go(Gameplay(challengeLevel = CHALLENGE_LEVEL))
        assertLastRequest(Hidden, "a Challenge level")
        composeRule.runOnUiThread { navController.popBackStack(Home, inclusive = false) }
        composeRule.waitForIdle()
        assertLastRequest(Hidden, "Home, back from the board")
    }

    @Test
    fun theBarsAreNeverAskedBackBetweenAChallengeLevelAndItsResults() {
        host()
        go(Gameplay(challengeLevel = CHALLENGE_LEVEL))
        bars.clear()

        go(sampleResults(challengeLevel = CHALLENGE_LEVEL))
        go(Gameplay(challengeLevel = CHALLENGE_LEVEL + 1))
        go(sampleResults(challengeLevel = CHALLENGE_LEVEL + 1))

        assertWithMessage("the bar requests from the board to Results to the next level and its Results")
            .that(bars.records.map { it.state })
            .doesNotContain(Shown)
    }

    /**
     * The founder's report (2026-11-05): Settings and the Classic catalogue showed the status bar.
     * Each destination is entered from the board and asks for [Hidden] itself, and no request on the
     * way asks for [Shown].
     */
    @Test
    fun everyOtherDestinationHidesTheBars() {
        host()
        val standard =
            listOf(
                ClassicCatalog,
                Settings,
                Tutorial,
                Licenses,
                LicenseText(entryId = LICENCE_ENTRY),
                GoogleNotices,
                GoogleNoticeText(key = NOTICE_KEY),
            )

        standard.forEach { destination ->
            go(Gameplay(levelId = CLASSIC_BOARD))
            go(destination)
            assertLastRequest(Hidden, "$destination")
        }
        assertWithMessage("every bar request across the destinations")
            .that(bars.records.map { it.state })
            .doesNotContain(Shown)
    }

    /**
     * BD-06's restore case for the bars: the host restores the saved back stack while it composes, so
     * the adapter composed after it reads the board on the restored first frame. An adapter composed
     * before the `NavHost` would read no destination there and ask for [Shown] first.
     */
    @Test
    fun aRestoredBackStackWhoseTopIsTheBoardAsksForHiddenBarsFirst() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent {
            navController = rememberNavController()
            AppNavHostWithBarStandIns(navController = navController, hostWindow = hostWindow, bars = bars)
        }
        go(Gameplay(challengeLevel = CHALLENGE_LEVEL))
        bars.clear()

        restoration.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        assertWithMessage("the restored host's first bar request")
            .that(bars.records.firstOrNull()?.state)
            .isEqualTo(Hidden)
    }

    /** A destination added later is immersive too: the policy names no exception. */
    @Test
    fun aDestinationThePolicyDoesNotNameHidesTheBars() {
        composeRule.setContent {
            CompositionLocalProvider(LocalSystemBars provides bars) {
                navController = rememberNavController()
                val barsState = remember { mutableStateOf(Shown) }
                Box {
                    NavHost(navController = navController, startDestination = Gameplay(levelId = CLASSIC_BOARD)) {
                        composable<Gameplay> {}
                        composable<UnnamedDestination> {}
                    }
                    NavSystemBars(navController = navController, hostWindow = hostWindow, barsState = barsState)
                }
            }
        }
        composeRule.waitForIdle()
        assertLastRequest(Hidden, "the board")

        go(UnnamedDestination)

        assertLastRequest(Hidden, "a destination the policy does not name")
    }

    /**
     * The dialogs' half (§3.8 (a), (c)): what the host hosts reads the current destination's bar
     * state through `LocalSystemBarsState`, so a dialog opened over the board can mirror it.
     */
    @Test
    fun theHostProvidesTheBoardsHiddenStateToWhatItHosts() {
        var provided: SystemBarsState? = null
        composeRule.setContent {
            navController = rememberNavController()
            AppNavHostWithBarStandIns(
                navController = navController,
                hostWindow = hostWindow,
                bars = bars,
                onBoardReadsBarsState = { provided = it },
            )
        }
        composeRule.waitForIdle()

        go(Gameplay(levelId = CLASSIC_BOARD))

        assertWithMessage("the bar state the board's content reads, once settled").that(provided).isEqualTo(Hidden)
    }

    /** Previews and the other host tests hand no window: the host asks nothing of any window. */
    @Test
    fun withNoHostWindowTheHostAsksNothing() {
        composeRule.setContent {
            navController = rememberNavController()
            AppNavHostWithBarStandIns(navController = navController, hostWindow = null, bars = bars)
        }
        composeRule.waitForIdle()
        go(Gameplay(levelId = CLASSIC_BOARD))

        assertWithMessage("bar requests made with no host window").that(bars.records).isEmpty()
    }

    /**
     * "Recorded in the frame the destination is first composed and before its first draw": with the
     * test clock held, the frame that first composes the board is the frame whose composition asks
     * for hidden bars. A composition's effects run before that frame's layout and draw, so the
     * request precedes the board's first draw. (Robolectric's paused looper never draws a frame on
     * its own, so the draw itself cannot be watched here; the device does, criterion 30g.)
     */
    @Test
    fun theHiddenBarsAreAskedForInTheFrameTheBoardIsFirstComposed() {
        host()
        composeRule.mainClock.autoAdvance = false
        composeRule.runOnUiThread { navController.navigate(Gameplay(levelId = CLASSIC_BOARD)) }
        assertWithMessage("whether the board is composed before any frame").that(boardComposed()).isFalse()
        bars.clear()

        var frames = 0
        while (!boardComposed() && frames < MAX_FRAMES_TO_THE_BOARD) {
            composeRule.mainClock.advanceTimeByFrame()
            frames++
        }

        assertWithMessage("whether the board was composed within $MAX_FRAMES_TO_THE_BOARD frames")
            .that(boardComposed())
            .isTrue()
        assertLastRequest(Hidden, "the frame that first composed the board (frame $frames)")
    }

    private fun host() {
        composeRule.setContent {
            navController = rememberNavController()
            AppNavHostWithBarStandIns(navController = navController, hostWindow = hostWindow, bars = bars)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BarStandInTags.HOME).assertExists()
    }

    private fun boardComposed(): Boolean =
        composeRule.onAllNodesWithTag(BarStandInTags.GAMEPLAY).fetchSemanticsNodes().isNotEmpty()

    private fun go(destination: Any) {
        composeRule.runOnUiThread { navController.navigate(destination) }
        composeRule.waitForIdle()
    }

    private fun assertLastRequest(
        state: SystemBarsState,
        where: String,
    ) {
        val last = bars.records.lastOrNull()
        assertWithMessage("the last bar request, on $where").that(last?.state).isEqualTo(state)
        assertWithMessage("the window the last bar request named, on $where")
            .that(last?.window)
            .isSameInstanceAs(hostWindow)
    }

    private fun sampleResults(challengeLevel: Int?): Results =
        Results(
            levelId = if (challengeLevel == null) CLASSIC_BOARD else "",
            stars = SAMPLE_STARS,
            pegsRemaining = SAMPLE_PEGS,
            elapsedSeconds = SAMPLE_SECONDS,
            score = SAMPLE_SCORE,
            challengeLevel = challengeLevel,
        )
}

/**
 * A destination no test above names, standing in for one added later. Internal, not
 * private: kotlinx.serialization reaches a route's serializer reflectively.
 */
@Serializable
internal data object UnnamedDestination

/** Robolectric has no android-all jar for Android 16 (SDK 36) on this machine; emulate SDK 34. */
private const val BARS_POLICY_ROBOLECTRIC_SDK = 34

/** Far more frames than a navigation needs to compose its destination; the loop stops at the first. */
private const val MAX_FRAMES_TO_THE_BOARD = 10

private const val CLASSIC_BOARD = "english33"
private const val CHALLENGE_LEVEL = 7
private const val LICENCE_ENTRY = "stand_in_entry"
private const val NOTICE_KEY = "stand_in_key"
private const val SAMPLE_STARS = 2
private const val SAMPLE_PEGS = 3
private const val SAMPLE_SECONDS = 75
private const val SAMPLE_SCORE = 1280
