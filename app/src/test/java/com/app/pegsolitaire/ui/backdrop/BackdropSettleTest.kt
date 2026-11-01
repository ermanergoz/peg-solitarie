package com.app.pegsolitaire.ui.backdrop

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.PowerManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.IntRect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.app.pegsolitaire.R
import com.app.pegsolitaire.testing.AppNavHostWithStandIns
import com.app.pegsolitaire.testing.BackdropClockProbe
import com.app.pegsolitaire.testing.CountingFrames
import com.app.pegsolitaire.testing.StandInTags
import com.app.pegsolitaire.testing.backdropSemantics
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.home.HomeScreen
import com.app.pegsolitaire.ui.home.HomeUiState
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.ceil
import kotlin.math.floor

/**
 * BD-16 (c) under the tech-lead's A5 ruling, on the real host with frames from the test clock:
 * idle motion settles after the density's delay plus the 2 s ease and then awaits no frame; a tap,
 * a navigation and a resume each wake it; the tap still reaches the board; battery saver rests it;
 * and a settle that begins mid-pass ends the gel sheen (B-22a).
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

    /**
     * B-22a (BD-16 (c)): on the host's live clock, a settle that begins inside a gel pass ends that
     * pass at once. Home's Classic tile is brought inside its pass while Running, where its lip
     * differs from the same tile drawn with [StaticBackdropClock] (the control), and left idle
     * until the settle begins inside that same pass. From that frame on, through Settling and
     * AtRest, its lip is pixel-identical, in the same run, to the tile drawn with no sheen.
     */
    @Test
    @Config(qualifiers = SHEEN_WINDOW_QUALIFIER)
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun aSettleThatBeginsMidPassEndsTheGelSheen() {
        val withoutSheen = mutableStateOf(false)
        var clock: BackdropClock = StaticBackdropClock
        rule.mainClock.autoAdvance = false
        rule.setContent {
            AppTheme(darkTheme = false) {
                AppNavHostWithStandIns(rememberNavController(), frames, home = {
                    BackdropClockProbe { clock = it }
                    val live = LocalBackdropClock.current
                    CompositionLocalProvider(
                        LocalBackdropClock provides if (withoutSheen.value) StaticBackdropClock else live,
                    ) { RealHome() }
                })
            }
        }
        // The reference: the same tile, once Home's entrance has ended, drawn with no sheen.
        rule.mainClock.advanceTimeBy(HOME_ENTRANCE_DONE_MS)
        withoutSheen.value = true
        rule.mainClock.advanceTimeByFrame()
        val lip = classicLipRun()
        val reference = rule.renderedWindow().pixelsIn(lip)
        withoutSheen.value = false
        rule.mainClock.advanceTimeByFrame()

        // The player's last input, timed so that the settle 60 s later begins inside a pass.
        advanceClockTo(clock, SHEEN_INPUT_AT_MS)
        clock.onInput()
        rule.mainClock.advanceTimeBy(BackdropMotion.BACKDROP_HOME_IDLE_MS - CONTROL_LEAD_MS)

        // The control: still Running, inside the pass, and the passing sheen shows on the lip.
        assertWithMessage("the control's motion").that(clock.motion).isEqualTo(BackdropMotionState.Running)
        assertWithMessage("the control at ${clock.timeMs} ms is inside a pass")
            .that(classicSheen(clock))
            .isNotEqualTo(SHEEN_HIDDEN)
        assertWithMessage("the control: pixels of Classic's lip the passing sheen changes")
            .that(rule.renderedWindow().pixelsIn(lip).differingFrom(reference))
            .isGreaterThan(0)

        // The settle begins inside that pass; from that frame on the lip must show no sheen.
        stepFramesUntil(MAX_FRAMES_TO_SETTLE) { clock.motion != BackdropMotionState.Running }
        assertWithMessage("the frame the settle begins").that(clock.motion).isEqualTo(BackdropMotionState.Settling)
        assertWithMessage("the settle begins at ${clock.timeMs} ms, inside the pass")
            .that(classicSheen(clock))
            .isNotEqualTo(SHEEN_HIDDEN)
        val differing = linkedMapOf<String, Int>()
        differing["the frame the settle begins (${clock.timeMs} ms)"] = lipDifference(lip, reference)
        var settlingFrames = 0
        while (clock.motion == BackdropMotionState.Settling) {
            check(settlingFrames++ < MAX_FRAMES_TO_SETTLE) { "still settling after $settlingFrames frames" }
            rule.mainClock.advanceTimeByFrame()
            if (settlingFrames % SETTLE_SAMPLE_FRAMES == 0 && clock.motion == BackdropMotionState.Settling) {
                differing["settling, frame $settlingFrames (${clock.timeMs} ms)"] = lipDifference(lip, reference)
            }
        }
        assertWithMessage("after the ease").that(clock.motion).isEqualTo(BackdropMotionState.AtRest)
        assertWithMessage("at rest at ${clock.timeMs} ms, still inside the pass, so a frozen sheen would show")
            .that(classicSheen(clock))
            .isNotEqualTo(SHEEN_HIDDEN)
        differing["at rest (${clock.timeMs} ms)"] = lipDifference(lip, reference)
        rule.mainClock.advanceTimeBy(QUIET_WATCH_MS)
        differing["at rest, $QUIET_WATCH_MS ms later"] = lipDifference(lip, reference)

        assertWithMessage("pixels of Classic's lip that differ from the tile drawn with no sheen")
            .that(differing)
            .isEqualTo(differing.mapValues { 0 })
    }

    /** The real Home, in its first-frame state, as `MainActivity` draws it inside the host. */
    @Composable
    private fun RealHome() {
        HomeScreen(
            state = HomeUiState(),
            onPlayClassic = {},
            onPlayChallenge = {},
            onResumeClassic = {},
            onResumeChallenge = {},
            onAcceptPlayGamesPrompt = {},
            onDeclinePlayGamesPrompt = {},
            onKeepDeviceProgress = {},
            onUseCloudProgress = {},
            onPlayGamesSignInErrorShown = {},
            onOpenSettings = {},
        )
    }

    /**
     * The straight run of Classic's gel lip, in px: between the lip's rounded ends, so every pixel
     * lies inside both the lip and the tile's opaque body, never on the backdrop through a corner.
     */
    private fun classicLipRun(): IntRect {
        val resources = ApplicationProvider.getApplicationContext<Context>().resources
        val title = resources.getString(R.string.classic_tile_title)
        val tile = rule.onNode(hasText(title) and hasClickAction()).fetchSemanticsNode().boundsInRoot
        val across = resources.getDimension(R.dimen.gel_lip_inset) + resources.getDimension(R.dimen.gel_lip_corner)
        return IntRect(
            left = ceil(tile.left + across).toInt(),
            top = ceil(tile.top + resources.getDimension(R.dimen.gel_lip_top)).toInt(),
            right = floor(tile.right - across).toInt(),
            bottom = floor(tile.top + resources.getDimension(R.dimen.gel_lip_bottom)).toInt(),
        )
    }

    private fun lipDifference(
        lip: IntRect,
        reference: IntArray,
    ): Int = rule.renderedWindow().pixelsIn(lip).differingFrom(reference)

    private fun classicSheen(clock: BackdropClock): Float =
        gelSheenProgress(clock.timeMs, BackdropMotion.GEL_SHEEN_CLASSIC_DELAY_MS)

    private fun advanceClockTo(
        clock: BackdropClock,
        timeMs: Long,
    ) {
        rule.mainClock.advanceTimeBy((timeMs - clock.timeMs).coerceAtLeast(0L))
        stepFramesUntil(MAX_FRAMES_TO_SETTLE) { clock.timeMs >= timeMs }
    }

    private fun stepFramesUntil(
        maxFrames: Int,
        done: () -> Boolean,
    ) {
        var stepped = 0
        while (!done()) {
            check(stepped++ < maxFrames) { "the condition did not hold within $maxFrames frames" }
            rule.mainClock.advanceTimeByFrame()
        }
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

/** The 6.7-inch baseline window: Home's Classic tile is wholly in view. */
private const val SHEEN_WINDOW_QUALIFIER = "w412dp-h915dp"

/** Home's entrance cascade (240 ms bands, 40 ms apart) has ended well before this. */
private const val HOME_ENTRANCE_DONE_MS = 1_000L

/**
 * The clock time of the player's last input: 82% into Classic's 10 s sheen loop, whose pass starts
 * at 75%. The settle then begins 60 s later at the same point of the loop, inside a pass, and its
 * 2 s ease adds about 0.75 s of clock time, so the clock also comes to rest inside that pass.
 */
private const val SHEEN_INPUT_AT_MS = 8_200L

/** The control is read this long before the settle begins: ten frames, inside the same pass. */
private const val CONTROL_LEAD_MS = 160L

/** More frames than the control's lead and the 2 s ease (125 test frames) each take. */
private const val MAX_FRAMES_TO_SETTLE = 200

/** Through the ease, the lip is read every this many frames: every 160 ms. */
private const val SETTLE_SAMPLE_FRAMES = 10
private const val SETTLED_AFTER_MS = 2_500L
private const val QUIET_WATCH_MS = 10_000L
private const val FRAME_SLACK_MS = 100L

/** The pixels of [rect] in this frame, row by row. */
private fun Bitmap.pixelsIn(rect: IntRect): IntArray =
    IntArray(rect.width * rect.height).also {
        getPixels(it, 0, rect.width, rect.left, rect.top, rect.width, rect.height)
    }

/** How many pixels differ from [other]'s at the same place: 0 when the two are pixel-identical. */
private fun IntArray.differingFrom(other: IntArray): Int = indices.count { this[it] != other[it] }
