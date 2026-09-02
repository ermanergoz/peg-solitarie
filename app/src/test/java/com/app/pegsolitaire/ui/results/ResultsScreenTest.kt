package com.app.pegsolitaire.ui.results

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.NewBests
import com.app.pegsolitaire.ui.game.GameTimerFormat
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
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
                    onNextLevel = {},
                    onRetry = {},
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

    @Test
    fun solvedChallengeShowsExactlyOneNextLevelAndNoReplayOrRetry() {
        composeRule.setContent {
            AppTheme {
                ResultsScreen(
                    state = SOLVED_CHALLENGE_STATE,
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

        // Exactly one filled primary "Next level" (the sole accent), plus neutral Home.
        composeRule.onAllNodesWithText(context.getString(R.string.action_next_level)).assertCountEquals(1)
        assertTextDisplayed(R.string.action_home)
        // No Retry, Replay, or Catalog on a solved Challenge result.
        composeRule.onAllNodesWithText(context.getString(R.string.action_retry)).assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.action_replay)).assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.action_catalog)).assertCountEquals(0)
    }

    @Test
    fun stuckChallengeShowsExactlyOneRetryAndNoNextLevel() {
        composeRule.setContent {
            AppTheme {
                ResultsScreen(
                    state = STUCK_CHALLENGE_STATE,
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

        // Exactly one filled primary "Retry" (the sole accent), plus neutral Home; no "Next level".
        composeRule.onAllNodesWithText(context.getString(R.string.action_retry)).assertCountEquals(1)
        assertTextDisplayed(R.string.action_home)
        composeRule.onAllNodesWithText(context.getString(R.string.action_next_level)).assertCountEquals(0)
    }

    @Test
    fun theNewBestCueAppearsOnlyWhenThisPlayImprovedAStoredBest() {
        composeRule.setContent {
            AppTheme {
                ResultsScreen(
                    state = SAMPLE_STATE.copy(newBests = A_NEW_TIME_ONLY),
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

        composeRule.onNodeWithTag(RESULTS_NEW_BEST_TEST_TAG).assertExists()
        assertTextDisplayed(R.string.results_new_best)
    }

    @Test
    fun aPlayThatBeatNothingShowsNoNewBestCue() {
        composeRule.setContent {
            AppTheme {
                ResultsScreen(
                    state = SAMPLE_STATE,
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

        composeRule.onNodeWithTag(RESULTS_NEW_BEST_TEST_TAG).assertDoesNotExist()
        composeRule.onAllNodesWithText(context.getString(R.string.results_new_best)).assertCountEquals(0)
    }

    @Test
    fun theQuiescenceScrimIsDisplayedWhileAnInterstitialIsComing() {
        setResultsContent(state = AD_COMING_STATE)

        composeRule.onNodeWithTag(RESULTS_AD_SCRIM_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun theFastPathDimsNothing() {
        // D-10: a continue tap with no ad behind it costs the player nothing — no scrim and
        // no delay. The dim is conditional on state.adScrimVisible, never unconditional.
        setResultsContent(state = SOLVED_CHALLENGE_STATE)

        composeRule.onNodeWithTag(RESULTS_AD_SCRIM_TEST_TAG).assertDoesNotExist()
    }

    /**
     * INT-04's actual proof: with the dim up, a tap on the primary action does not reach it.
     *
     * This is the accidental-click window the scrim exists to close — in the quiescence pause
     * the player's finger is still travelling, and a second tap that lands after the
     * interstitial has appeared underneath it is invalid traffic attributed to the publisher.
     * Asserting "the callback did not fire" is only meaningful because of its paired control
     * below: without [theSamePrimaryActionTapLandsWhenNoAdIsComing], this test would still pass
     * if the button had simply stopped being rendered at all.
     */
    @Test
    fun aTapBehindTheQuiescenceScrimNeverReachesThePrimaryAction() {
        var tappedNextLevel: Int? = null
        setResultsContent(state = AD_COMING_STATE, onNextLevel = { tappedNextLevel = it })

        composeRule.onNodeWithText(context.getString(R.string.action_next_level)).performClick()
        composeRule.waitForIdle()

        assertThat(tappedNextLevel).isNull()
    }

    /** The control for [aTapBehindTheQuiescenceScrimNeverReachesThePrimaryAction]. */
    @Test
    fun theSamePrimaryActionTapLandsWhenNoAdIsComing() {
        var tappedNextLevel: Int? = null
        setResultsContent(state = SOLVED_CHALLENGE_STATE, onNextLevel = { tappedNextLevel = it })

        composeRule.onNodeWithText(context.getString(R.string.action_next_level)).performClick()
        composeRule.waitForIdle()

        assertThat(tappedNextLevel).isEqualTo(SAMPLE_CHALLENGE_LEVEL + 1)
    }

    private fun assertTextDisplayed(resId: Int) {
        composeRule.onNodeWithText(context.getString(resId)).performScrollTo().assertIsDisplayed()
    }

    /** Renders the stateless screen over [state], recording only the callback under test. */
    private fun setResultsContent(
        state: ResultsUiState,
        onNextLevel: (Int) -> Unit = {},
    ) {
        composeRule.setContent {
            AppTheme {
                ResultsScreen(
                    state = state,
                    onReplay = {},
                    onCatalog = {},
                    onHome = {},
                    onNextLevel = onNextLevel,
                    onRetry = {},
                    onNavigateBack = {},
                )
            }
        }
        composeRule.waitForIdle()
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

/** A solved Challenge level (one peg) — offers "Next level" (D-01). */
private val SOLVED_CHALLENGE_STATE =
    ResultsUiState(
        stars = PERFECT_STARS,
        pegsRemaining = SOLVED_PEGS_REMAINING,
        elapsedSeconds = SAMPLE_ELAPSED_SECONDS,
        score = SAMPLE_SCORE,
        headlineTier = ResultsHeadlineTier.PERFECT,
        replayId = "",
        challengeLevel = SAMPLE_CHALLENGE_LEVEL,
    )

/** A stuck Challenge level (pegs left) — offers "Retry", not "Next level" (D-10). */
private val STUCK_CHALLENGE_STATE =
    ResultsUiState(
        stars = STUCK_STARS,
        pegsRemaining = SAMPLE_PEGS_REMAINING,
        elapsedSeconds = SAMPLE_ELAPSED_SECONDS,
        score = SAMPLE_SCORE,
        headlineTier = ResultsHeadlineTier.STUCK,
        replayId = "",
        challengeLevel = SAMPLE_CHALLENGE_LEVEL,
    )

/**
 * The same solved Challenge result the instant an ad-eligible continue tap is accepted:
 * the ViewModel has raised the INT-04 quiescence signal and the interstitial is coming.
 */
private val AD_COMING_STATE = SOLVED_CHALLENGE_STATE.copy(adScrimVisible = true)

/** An arbitrary in-progress Challenge level number. */
private const val SAMPLE_CHALLENGE_LEVEL = 5

/** A play that beat only the stored time — enough to earn the "New best!" cue (D-09). */
private val A_NEW_TIME_ONLY = NewBests(stars = false, time = true, score = false)

/** A textbook solve earns three stars and leaves a single peg. */
private const val PERFECT_STARS = 3
private const val SOLVED_PEGS_REMAINING = 1

/** A stuck board floors at one star. */
private const val STUCK_STARS = 1
