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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
        repeat(SETTLE_FRAMES) { composeRule.mainClock.advanceTimeByFrame() }

        val regions =
            composeRule
                .onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion),
                ).fetchSemanticsNodes()
        val spoken = regions.mapNotNull { it.config.getOrNull(SemanticsProperties.ContentDescription)?.single() }
        assertThat(spoken).containsExactly("Solved! — 2 of 3 stars, 2 pegs left.")
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
        repeat(SETTLE_FRAMES) { composeRule.mainClock.advanceTimeByFrame() }

        assertThat(starRows()).hasSize(1)
        assertThat(spokenLines()).isEmpty()
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

        val SOLVED_TWO_STARS =
            ResultsUiState(
                stars = 2,
                pegsRemaining = 2,
                elapsedSeconds = 95,
                score = 1_000,
                headlineTier = ResultsHeadlineTier.SOLVED,
                replayId = ClassicLevelIds.ENGLISH33,
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
