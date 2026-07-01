package com.app.pegsolitaire.ui.results

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.ui.game.GameTimerFormat
import com.app.pegsolitaire.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric + Compose UI test (`createComposeRule`, JVM, no emulator) for the
 * stateless [ResultsScreen]: rendering a sample [ResultsUiState] surfaces the tiered
 * headline (D-08), the 2-filled/1-outline star row, the SCORE / TIME / PEGS LEFT
 * metric block (captions + the mm:ss-formatted time), and the three action buttons
 * (Replay / Catalog / Home) — with no "Next board" action (Classic has no next board).
 *
 * Assertions read the row-level [EarnedStarCountKey] semantics (the star icons are
 * decorative) and the visible copy via `onNodeWithText`, so they are deterministic
 * rather than pixel-based. The Robolectric runtime SDK is pinned independently of the
 * app's `compileSdk 36` (Android 16 has no Robolectric android-all jar yet).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class ResultsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun rendersHeadlineStarsMetricsAndTheThreeActions() {
        composeRule.setContent {
            AppTheme {
                ResultsScreen(
                    state = SAMPLE_STATE,
                    onReplay = {},
                    onCatalog = {},
                    onHome = {},
                    onNavigateBack = {},
                )
            }
        }
        composeRule.waitForIdle()

        // Tiered headline (2 stars → "Solved!", D-08).
        assertTextDisplayed(R.string.results_headline_solved)

        // Star row: 2 filled of 3 (the remaining 1 is outline).
        composeRule
            .onNodeWithTag(RESULTS_STAR_ROW_TEST_TAG, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(EarnedStarCountKey, SAMPLE_STARS))

        // Metric captions + the mm:ss-formatted time + the raw score/pegs values.
        assertTextDisplayed(R.string.results_label_score)
        assertTextDisplayed(R.string.results_label_time)
        assertTextDisplayed(R.string.results_label_pegs_left)
        composeRule.onNodeWithText(GameTimerFormat.format(SAMPLE_ELAPSED_SECONDS)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(SAMPLE_SCORE.toString()).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(SAMPLE_PEGS_REMAINING.toString()).performScrollTo().assertIsDisplayed()

        // The three actions (Replay is the single filled primary; Catalog / Home neutral).
        assertTextDisplayed(R.string.action_replay)
        assertTextDisplayed(R.string.action_catalog)
        assertTextDisplayed(R.string.action_home)

        // No "Next board" action — Classic has no next board.
        composeRule.onAllNodesWithText(NEXT_BOARD_LABEL).assertCountEquals(0)
    }

    private fun assertTextDisplayed(resId: Int) {
        composeRule.onNodeWithText(context.getString(resId)).performScrollTo().assertIsDisplayed()
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34

private const val SAMPLE_STARS = 2
private const val SAMPLE_PEGS_REMAINING = 5
private const val SAMPLE_ELAPSED_SECONDS = 75
private const val SAMPLE_SCORE = 1280

/** The removed "Next board" label — asserted absent so the Challenge-mode action never appears. */
private const val NEXT_BOARD_LABEL = "Next board"

/** A 2-star (Solved) result on English-33; Replay re-plays the same board. */
private val SAMPLE_STATE =
    ResultsUiState(
        stars = SAMPLE_STARS,
        pegsRemaining = SAMPLE_PEGS_REMAINING,
        elapsedSeconds = SAMPLE_ELAPSED_SECONDS,
        score = SAMPLE_SCORE,
        headlineTier = ResultsHeadlineTier.SOLVED,
        replayId = ClassicLevelIds.ENGLISH33,
    )
