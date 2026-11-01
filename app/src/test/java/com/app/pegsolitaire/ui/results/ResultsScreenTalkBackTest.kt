package com.app.pegsolitaire.ui.results

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.testing.descriptionChangesSent
import com.app.pegsolitaire.testing.liveRegionsHandedOut
import com.app.pegsolitaire.testing.turnOnATouchExplorationService
import com.app.pegsolitaire.ui.components.ANNOUNCEMENT_ARM_GAP_MS
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.ClayAnim
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * TB-8 (D-TB as amended by D-TBa item 7, `docs/ux-spec.md` §11.1, "Results"): the star row is one
 * stop saying "2 of 3 stars", and the summary — the headline, the stars and the pegs left — is spoken
 * once, on the showing the D-16 celebration latch marks, and never on a showing it does not.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [RESULTS_TALKBACK_SDK])
class ResultsScreenTalkBackTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theStarRowIsOneStopSayingItsStarsOutOfThree() {
        show(SOLVED_TWO_STARS)

        val row = composeRule.onNodeWithTag(RESULTS_STAR_ROW_TEST_TAG).fetchSemanticsNode()
        assertThat(row.config.getOrNull(SemanticsProperties.ContentDescription)).containsExactly("2 of 3 stars")
        assertThat(row.config[EarnedStarCountKey]).isEqualTo(2)
        assertThat(row.children).isEmpty()
    }

    @Test
    fun theSummaryIsSpokenOnTheShowingTheCelebrationLatchMarks() {
        // On a paused clock: the star pops would otherwise run the clock past the line's hold.
        composeRule.mainClock.autoAdvance = false
        show(SOLVED_TWO_STARS.copy(playCelebration = true))
        // Loop 3: the channel speaks once armed (defect ruling §1, mechanism 5).
        composeRule.mainClock.advanceTimeBy(ANNOUNCEMENT_ARM_GAP_MS)
        repeat(SETTLE_FRAMES) { composeRule.mainClock.advanceTimeByFrame() }

        val regions =
            composeRule
                .onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion),
                ).fetchSemanticsNodes()
        val spoken = regions.mapNotNull { it.config.getOrNull(SemanticsProperties.ContentDescription)?.single() }
        assertThat(spoken).containsExactly("Solved! — 2 of 3 stars, 2 pegs left.")
    }

    /**
     * Criterion 30d (ux-spec D-SB-2, §4.6; TB-8): a one-star Challenge clear is summarised once in
     * the existing shape — "Cleared! — 1 of 3 stars, 3 pegs left." — and its next-star line is a stop
     * of its own, straight after the star row: plain text, no live region, no role, and outside the
     * row's merge, so the row still says only its stars.
     */
    @Test
    fun aOneStarClearIsSummarisedAsClearedAndItsNextStarLineIsTheStopAfterTheStars() {
        composeRule.mainClock.autoAdvance = false
        show(CLEARED_ONE_STAR.copy(playCelebration = true))
        composeRule.mainClock.advanceTimeBy(ANNOUNCEMENT_ARM_GAP_MS)
        repeat(SETTLE_FRAMES) { composeRule.mainClock.advanceTimeByFrame() }

        assertThat(spokenLines()).containsExactly("Cleared! — 1 of 3 stars, 3 pegs left.")
        val line = composeRule.onNodeWithTag(RESULTS_NEXT_STAR_HINT_TEST_TAG).fetchSemanticsNode()
        assertThat(line.config.getOrNull(SemanticsProperties.Text)?.map { it.text })
            .containsExactly("Leave 2 pegs for 2 stars.")
        assertThat(line.config.getOrNull(SemanticsProperties.LiveRegion)).isNull()
        assertThat(line.config.getOrNull(SemanticsProperties.Role)).isNull()
        val row = composeRule.onNodeWithTag(RESULTS_STAR_ROW_TEST_TAG).fetchSemanticsNode()
        assertThat(row.config.getOrNull(SemanticsProperties.ContentDescription)).containsExactly("1 of 3 stars")
        assertThat(row.children).isEmpty()
        val column = checkNotNull(row.parent) { "the star row sits in the Results column" }.children
        assertThat(column.indexOfFirst { it.id == line.id }).isEqualTo(column.indexOfFirst { it.id == row.id } + 1)
    }

    @Test
    fun noSummaryIsSpokenOnAShowingTheLatchDoesNotMark() {
        show(SOLVED_TWO_STARS.copy(playCelebration = false))

        val summary =
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf("Solved! — 2 of 3 stars, 2 pegs left."),
            )
        assertThat(composeRule.onAllNodes(summary).fetchSemanticsNodes()).isEmpty()
    }

    /**
     * Loop 1, F-1: the ViewModel survives a rotation and a return onto the same entry, and its latch
     * stays `true`, so the screen must remember on its own, across leaving and re-entering
     * composition with its saved state (as Navigation keeps an entry's), that it already spoke.
     */
    @Test
    fun reEnteringCompositionOverTheRetainedLatchSpeaksTheSummaryNoSecondTime() {
        var shown by mutableStateOf(true)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            val saved = rememberSaveableStateHolder()
            AppTheme {
                if (shown) {
                    saved.SaveableStateProvider(
                        RESULTS_ENTRY,
                    ) { ResultsOver(SOLVED_TWO_STARS.copy(playCelebration = true)) }
                }
            }
        }
        // Loop 3: the channel speaks once armed (defect ruling §1, mechanism 5).
        composeRule.mainClock.advanceTimeBy(ANNOUNCEMENT_ARM_GAP_MS)
        repeat(SETTLE_FRAMES) { composeRule.mainClock.advanceTimeByFrame() }
        assertThat(spokenLines()).containsExactly("Solved! — 2 of 3 stars, 2 pegs left.")
        composeRule.mainClock.advanceTimeBy(PAST_THE_HOLD_MS)
        assertThat(spokenLines()).isEmpty()

        shown = false
        Snapshot.sendApplyNotifications()
        repeat(SETTLE_FRAMES) { composeRule.mainClock.advanceTimeByFrame() }
        assertThat(starRows()).isEmpty()
        shown = true
        Snapshot.sendApplyNotifications()
        composeRule.mainClock.advanceTimeBy(ANNOUNCEMENT_ARM_GAP_MS)
        repeat(SETTLE_FRAMES) { composeRule.mainClock.advanceTimeByFrame() }

        assertThat(starRows()).hasSize(1)
        assertThat(spokenLines()).isEmpty()
    }

    /** Defect ruling §4 (a), B-4: a service is handed one live region on Results, and it is a leaf. */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE) // Compose prunes with android.graphics.Region
    fun aServiceIsHandedOneLiveRegionOnResultsAndItIsALeaf() {
        turnOnATouchExplorationService()
        show(SOLVED_TWO_STARS)

        val regions = composeRule.liveRegionsHandedOut()
        assertThat(regions).hasSize(1)
        assertThat(regions.single().childCount).isEqualTo(0)
    }

    /**
     * Defect ruling §4 (b), B-4: the summary waits for the channel's arm, then is sent once — not
     * again as the count-up lays the screen out, and not when its hold ends.
     */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE) // Compose prunes with android.graphics.Region
    fun theSummaryWaitsForTheArmAndIsSentOnceThroughTheCountUpAndTheHoldsEnd() {
        turnOnATouchExplorationService()
        composeRule.mainClock.autoAdvance = false
        show(SOLVED_TWO_STARS.copy(playCelebration = true))
        composeRule.mainClock.advanceTimeByFrame()
        assertThat(spokenLines()).isEmpty()

        composeRule.mainClock.advanceTimeBy(ANNOUNCEMENT_ARM_GAP_MS + FRAME_MS)
        assertThat(spokenLines()).containsExactly(SUMMARY)
        composeRule.mainClock.advanceTimeBy(ClayAnim.SCORE_COUNT_UP_MS + PAST_THE_HOLD_MS)

        assertThat(descriptionChangesSent().filter { it == SUMMARY }).hasSize(1)
    }

    /** Defect ruling §4 (c): a Results restored from its saved state sends the service no second summary. */
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE) // Compose prunes with android.graphics.Region
    fun aRestoredResultsSendsTheServiceNoSecondSummary() {
        turnOnATouchExplorationService()
        val restorer = StateRestorationTester(composeRule)
        restorer.setContent { AppTheme { ResultsOver(SOLVED_TWO_STARS.copy(playCelebration = true)) } }
        composeRule.mainClock.advanceTimeBy(ANNOUNCEMENT_ARM_GAP_MS + PAST_THE_HOLD_MS)
        val sentBefore = descriptionChangesSent().size

        restorer.emulateSavedInstanceStateRestore()
        composeRule.mainClock.advanceTimeBy(ANNOUNCEMENT_ARM_GAP_MS + PAST_THE_HOLD_MS)

        assertThat(descriptionChangesSent().drop(sentBefore)).doesNotContain(SUMMARY)
    }

    private fun starRows() =
        composeRule.onAllNodes(SemanticsMatcher.keyIsDefined(EarnedStarCountKey)).fetchSemanticsNodes()

    /** Every line a polite live region on the screen carries right now. */
    private fun spokenLines(): List<String> =
        composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
            .fetchSemanticsNodes()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.ContentDescription)?.single() }

    private fun show(state: ResultsUiState) {
        composeRule.setContent {
            AppTheme {
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
        }
        composeRule.waitForIdle()
    }

    private companion object {
        /** The saved-state key the test's holder keeps Results under, as Navigation keys an entry. */
        const val RESULTS_ENTRY = "results"

        /** Comfortably past the line's one-second hold. */
        const val PAST_THE_HOLD_MS = 2_000L

        /** Frames for the latch's effect to run and the line to reach the semantics tree. */
        const val SETTLE_FRAMES = 3
        const val FRAME_MS = 16L
        const val SUMMARY = "Solved! — 2 of 3 stars, 2 pegs left."

        val SOLVED_TWO_STARS =
            ResultsUiState(
                stars = 2,
                pegsRemaining = 2,
                elapsedSeconds = 95,
                score = 1_000,
                headlineTier = ResultsHeadlineTier.SOLVED,
                replayId = ClassicLevelIds.ENGLISH33,
            )

        /** A one-star Challenge clear: three pegs left on level 4. */
        val CLEARED_ONE_STAR =
            ResultsUiState(
                stars = 1,
                pegsRemaining = 3,
                elapsedSeconds = 38,
                score = 3_100,
                headlineTier = ResultsHeadlineTier.CLEARED,
                replayId = "",
                challengeLevel = 4,
            )
    }
}

private const val RESULTS_TALKBACK_SDK = 34

/** Results over [state], every callback a no-op. */
@Composable
private fun ResultsOver(state: ResultsUiState) {
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
