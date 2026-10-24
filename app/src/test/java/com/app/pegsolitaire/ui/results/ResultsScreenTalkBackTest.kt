package com.app.pegsolitaire.ui.results

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
