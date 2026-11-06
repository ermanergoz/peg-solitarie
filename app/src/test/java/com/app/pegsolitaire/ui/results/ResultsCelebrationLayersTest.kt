package com.app.pegsolitaire.ui.results

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.testing.SystemMotionScale
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The Results celebration layers on the Compose test clock (ux-spec D-RC, §4.7, §13's
 * `ResultsCelebrationLayersTest`; criteria RC-1, RC-4, RC-7, RC-8, RC-10): when the confetti exists,
 * when it never does, that a recreation ends it for good, that reduced motion omits it and stills the
 * rays, and that neither layer adds a description, a role, a click or a stop, nor blocks a tap.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [LAYERS_ROBOLECTRIC_SDK])
class ResultsCelebrationLayersTest {
    private val motion = SystemMotionScale()

    @get:Rule
    val composeRule = createComposeRule(effectContext = motion)

    private val context get() = RuntimeEnvironment.getApplication()

    // ---- RC-7: confetti at three stars only ----

    @Test
    fun aClassicThreeStarResultFallsFromTheThirdStarAndIsGoneAtTheEnd() = assertTheFallRuns(CLASSIC_PERFECT)

    @Test
    fun aChallengeThreeStarResultFallsFromTheThirdStarAndIsGoneAtTheEnd() = assertTheFallRuns(CHALLENGE_PERFECT)

    @Test
    fun noConfettiFallsBelowThreeStarsOnTheStuckTierOrWithoutTheLatch() {
        val states =
            listOf(
                CLASSIC_TWO_STARS,
                CHALLENGE_ONE_STAR,
                CHALLENGE_STUCK,
                CLASSIC_PERFECT.copy(playCelebration = false),
            )
        val shown = mutableIntStateOf(0)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { AppTheme { key(shown.intValue) { screen(states[shown.intValue]) } } }
        states.indices.forEach { index ->
            composeRule.runOnUiThread { shown.intValue = index }
            composeRule.mainClock.advanceTimeByFrame()
            assertNoConfettiThroughTheWholeFall("${states[index]}")
        }
    }

    @Test
    fun theColumnsSemanticsAreTheSameWithAndWithoutTheFall() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { AppTheme { screen(CLASSIC_PERFECT) } }
        advanceTo(MID_FALL_MS)
        confettiCount().let { assertWithMessage("mid-fall").that(it).isEqualTo(1) }
        val during = columnSemantics()
        advanceTo(AFTER_END_MS)
        confettiCount().let { assertWithMessage("after the end").that(it).isEqualTo(0) }
        assertWithMessage("the column's semantics").that(columnSemantics()).isEqualTo(during)
    }

    // ---- RC-8: once per result ----

    /**
     * The screen is disposed with its saved state and composed again from it, as a rotation, a theme
     * switch or a return onto the entry does through the `NavHost`'s `SaveableStateHolder`; the
     * ViewModel's latch stays `true` across all three, so only the screen's saved flag can stop a
     * replay. (`StateRestorationTester` needs the clock to advance between its steps, which would run
     * the fall to its end first.)
     */
    @Test
    fun aRecreationMidFallEndsItAndItNeverStartsAgain() {
        val shown = mutableStateOf(true)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            val holder = rememberSaveableStateHolder()
            AppTheme {
                if (shown.value) holder.SaveableStateProvider(RESULTS_ENTRY_KEY) { screen(CLASSIC_PERFECT) }
            }
        }
        advanceTo(MID_FALL_MS)
        assertWithMessage("falling before the recreation").that(confettiCount()).isEqualTo(1)
        composeRule.runOnUiThread { shown.value = false }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeByFrame()
        assertWithMessage("disposed").that(confettiCount()).isEqualTo(0)
        composeRule.runOnUiThread { shown.value = true }
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.mainClock.advanceTimeByFrame()
        assertWithMessage("recreated").that(raysPose()).isEqualTo(ResultsRaysPose.Live)
        var elapsed = 0L
        while (elapsed <= AFTER_END_MS) {
            assertWithMessage("after the restore, at +$elapsed ms").that(confettiCount()).isEqualTo(0)
            composeRule.mainClock.advanceTimeBy(STEP_MS)
            elapsed += STEP_MS
        }
    }

    // ---- RC-4: reduced motion ----

    @Test
    fun withAnimationsOffTheRaysRestAndNoConfettiEverFalls() {
        motion.turnAnimationsOff()
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { AppTheme { screen(CLASSIC_PERFECT) } }
        composeRule.mainClock.advanceTimeByFrame()
        assertWithMessage("rays").that(raysPose()).isEqualTo(ResultsRaysPose.Static)
        assertNoConfettiThroughTheWholeFall("animations off")
    }

    @Test
    fun aLiveSwitchOffEndsTheFallAndStillsTheRaysAndOnLeavesNoFall() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { AppTheme { screen(CLASSIC_PERFECT) } }
        advanceTo(MID_FALL_MS)
        assertWithMessage("rays before").that(raysPose()).isEqualTo(ResultsRaysPose.Live)
        assertWithMessage("falling before").that(confettiCount()).isEqualTo(1)
        motion.turnAnimationsOff()
        composeRule.mainClock.advanceTimeByFrame()
        assertWithMessage("rays after off").that(raysPose()).isEqualTo(ResultsRaysPose.Static)
        assertWithMessage("the fall ended on that frame").that(confettiCount()).isEqualTo(0)
        motion.turnAnimationsOn()
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeByFrame()
        // The clock's driver marks it live in a SideEffect; the reported pose follows a frame later.
        composeRule.mainClock.advanceTimeByFrame()
        assertWithMessage("rays after on").that(raysPose()).isEqualTo(ResultsRaysPose.Live)
        composeRule.mainClock.advanceTimeBy(AFTER_END_MS)
        assertWithMessage("none starts again").that(confettiCount()).isEqualTo(0)
    }

    // ---- RC-1, RC-10: the rays everywhere, decorative, nothing blocked ----

    @Test
    fun theRaysAreLiveOnEveryResultAndAddNoDescriptionRoleOrClick() {
        val states = listOf(CLASSIC_PERFECT, CLASSIC_TWO_STARS, CHALLENGE_ONE_STAR, CHALLENGE_PERFECT, CHALLENGE_STUCK)
        val shown = mutableIntStateOf(0)
        composeRule.setContent { AppTheme { key(shown.intValue) { screen(states[shown.intValue]) } } }
        states.forEachIndexed { index, state ->
            composeRule.runOnUiThread { shown.intValue = index }
            composeRule.waitForIdle()
            assertWithMessage("rays on $state").that(raysPose()).isEqualTo(ResultsRaysPose.Live)
            assertDecorative(raysNode(), "rays on $state")
            val parent = checkNotNull(raysNode().parent)
            assertWithMessage("the rays are composed first, before the Scaffold, on $state")
                .that(
                    parent.children
                        .first()
                        .config
                        .getOrNull(SemanticsProperties.TestTag),
                ).isEqualTo(RESULTS_RAYS_TEST_TAG)
        }
    }

    @Test
    fun theConfettiIsDecorativeAndCarriesItsSeed() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { AppTheme { screen(CHALLENGE_PERFECT) } }
        advanceTo(MID_FALL_MS)
        val node = composeRule.onNodeWithTag(RESULTS_CONFETTI_TEST_TAG, useUnmergedTree = true).fetchSemanticsNode()
        assertDecorative(node, "confetti")
        assertWithMessage("seed")
            .that(node.config[ResultsConfettiSeedKey])
            .isEqualTo(confettiSeed(CHALLENGE_PERFECT.replayId, CHALLENGE_PERFECT.challengeLevel))
    }

    /** A window tall enough that every action is on screen without a scroll, mid-fall. */
    @Test
    @Config(qualifiers = TALL_WINDOW)
    fun midFallTheActionsAndTheBackChipStillReachTheirCallbacks() {
        var replays = 0
        var catalogs = 0
        var homes = 0
        var backs = 0
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppTheme {
                ResultsScreen(
                    state = CLASSIC_PERFECT,
                    onReplay = { replays++ },
                    onCatalog = { catalogs++ },
                    onHome = { homes++ },
                    onNextLevel = {},
                    onRetry = {},
                    onNavigateBack = { backs++ },
                )
            }
        }
        advanceTo(MID_FALL_MS)
        assertWithMessage("falling").that(confettiCount()).isEqualTo(1)
        composeRule.onNodeWithText(context.getString(R.string.action_replay)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.action_catalog)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.action_home)).performClick()
        composeRule.onNodeWithContentDescription(context.getString(R.string.navigate_back)).performClick()
        composeRule.mainClock.advanceTimeByFrame()
        assertWithMessage("still falling while tapped").that(confettiCount()).isEqualTo(1)
        assertWithMessage("taps").that(listOf(replays, catalogs, homes, backs)).containsExactly(1, 1, 1, 1).inOrder()
    }

    // ---- helpers ----

    private fun assertTheFallRuns(state: ResultsUiState) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { AppTheme { screen(state) } }
        advanceTo(BEFORE_START_MS)
        assertWithMessage("before the third star").that(confettiCount()).isEqualTo(0)
        advanceTo(JUST_AFTER_START_MS)
        assertWithMessage("from the third star").that(confettiCount()).isEqualTo(1)
        advanceTo(BEFORE_END_MS)
        assertWithMessage("just before the end").that(confettiCount()).isEqualTo(1)
        advanceTo(AFTER_END_MS)
        assertWithMessage("after the end").that(confettiCount()).isEqualTo(0)
    }

    private fun assertNoConfettiThroughTheWholeFall(what: String) {
        var elapsed = 0L
        while (elapsed <= AFTER_END_MS) {
            assertWithMessage("$what at $elapsed ms").that(confettiCount()).isEqualTo(0)
            composeRule.mainClock.advanceTimeBy(STEP_MS)
            elapsed += STEP_MS
        }
    }

    private fun assertDecorative(
        node: SemanticsNode,
        what: String,
    ) {
        assertWithMessage("$what: description")
            .that(node.config.getOrNull(SemanticsProperties.ContentDescription))
            .isNull()
        assertWithMessage("$what: role").that(node.config.getOrNull(SemanticsProperties.Role)).isNull()
        assertWithMessage("$what: click").that(node.config.getOrNull(SemanticsActions.OnClick)).isNull()
        assertWithMessage("$what: children").that(node.children).isEmpty()
    }

    /** How far the frozen clock has been advanced since the content was set. */
    private var clockAt = 0L

    /** Advances the frozen clock to [ms] after the screen's first frame. */
    private fun advanceTo(ms: Long) {
        composeRule.mainClock.advanceTimeBy(ms - clockAt)
        clockAt = ms
    }

    private fun confettiCount(): Int =
        composeRule
            .onAllNodesWithTag(RESULTS_CONFETTI_TEST_TAG, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .size

    private fun raysNode(): SemanticsNode =
        composeRule.onNodeWithTag(RESULTS_RAYS_TEST_TAG, useUnmergedTree = true).fetchSemanticsNode()

    private fun raysPose(): ResultsRaysPose = raysNode().config[ResultsRaysPoseKey]

    /** Every text and description outside the two layers, in tree order. */
    private fun columnSemantics(): List<String> {
        val found = mutableListOf<String>()

        fun walk(node: SemanticsNode) {
            val tag = node.config.getOrNull(SemanticsProperties.TestTag)
            if (tag == RESULTS_CONFETTI_TEST_TAG || tag == RESULTS_RAYS_TEST_TAG) return
            node.config.getOrNull(SemanticsProperties.Text)?.forEach { found += it.text }
            node.config.getOrNull(SemanticsProperties.ContentDescription)?.forEach { found += it }
            node.children.forEach(::walk)
        }
        walk(composeRule.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        return found
    }

    @Composable
    private fun screen(state: ResultsUiState) {
        ResultsScreen(
            state = state,
            onReplay = {},
            onCatalog = {},
            onHome = {},
            onNextLevel = {},
            onRetry = {},
            onNavigateBack = {},
        )
    }

    private companion object {
        val CLASSIC_PERFECT =
            ResultsUiState(
                stars = 3,
                pegsRemaining = 1,
                elapsedSeconds = 75,
                score = 1_280,
                headlineTier = ResultsHeadlineTier.PERFECT,
                replayId = ClassicLevelIds.ENGLISH33,
                playCelebration = true,
            )
        val CLASSIC_TWO_STARS =
            CLASSIC_PERFECT.copy(stars = 2, pegsRemaining = 2, headlineTier = ResultsHeadlineTier.SOLVED)
        val CHALLENGE_PERFECT = CLASSIC_PERFECT.copy(replayId = "", challengeLevel = 12)
        val CHALLENGE_ONE_STAR =
            CHALLENGE_PERFECT.copy(stars = 1, pegsRemaining = 3, headlineTier = ResultsHeadlineTier.CLEARED)
        val CHALLENGE_STUCK =
            CHALLENGE_PERFECT.copy(stars = 1, pegsRemaining = 5, headlineTier = ResultsHeadlineTier.STUCK)

        const val START_MS = CelebrationFx.CONFETTI_START_MS.toLong()
        const val END_MS = START_MS + CelebrationFx.CONFETTI_TOTAL_MS
        const val FRAME_SLACK_MS = 50L
        const val BEFORE_START_MS = START_MS - FRAME_SLACK_MS
        const val JUST_AFTER_START_MS = START_MS + FRAME_SLACK_MS
        const val MID_FALL_MS = START_MS + CelebrationFx.CONFETTI_TOTAL_MS / 2
        const val BEFORE_END_MS = END_MS - FRAME_SLACK_MS
        const val AFTER_END_MS = END_MS + FRAME_SLACK_MS
        const val STEP_MS = 100L
        const val RESULTS_ENTRY_KEY = "results"
    }
}

/** A phone-sized window that holds the whole Results column. */
private const val TALL_WINDOW = "w411dp-h915dp"

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val LAYERS_ROBOLECTRIC_SDK = 34
