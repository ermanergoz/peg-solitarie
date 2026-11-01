package com.app.pegsolitaire.ui.results

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.testing.TURKISH_DEVICE
import com.app.pegsolitaire.testing.englishString
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The Results screen's main flow on a Turkish phone (docs/ux/localisation-tr.md §7.6, criterion R-1):
 * the headline, the three stat labels and the actions read in Turkish, and every action still reaches
 * its own callback, Classic and Challenge alike. Nodes are found by the strings this configuration
 * resolves, never by typed copy; [englishString] shows those strings are not the English ones.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [RESULTS_TURKISH_SDK], qualifiers = TURKISH_DEVICE)
class ResultsScreenTurkishTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context get() = RuntimeEnvironment.getApplication()

    private var replays = 0
    private var catalogs = 0
    private var homes = 0
    private var nextLevels = 0
    private var retries = 0

    @Test
    fun aClassicResultReadsInTurkishAndEachOfItsThreeActionsReachesItsOwnCallback() {
        render(CLASSIC_SOLVED)

        listOf(
            R.string.results_headline_solved,
            R.string.results_label_score,
            R.string.results_label_time,
            R.string.results_label_pegs_left,
        ).forEach { id ->
            assertThat(context.getString(id)).isNotEqualTo(englishString(id))
            composeRule.onNodeWithText(context.getString(id)).performScrollTo().assertIsDisplayed()
        }
        tap(R.string.action_replay)
        tap(R.string.action_catalog)
        tap(R.string.action_home)

        assertThat(listOf(replays, catalogs, homes, nextLevels, retries)).containsExactly(1, 1, 1, 0, 0).inOrder()
    }

    @Test
    fun aSolvedChallengeResultOffersOnlyTheTurkishNextLevelBesideHome() {
        render(CHALLENGE_SOLVED)

        assertThat(context.getString(R.string.action_next_level))
            .isNotEqualTo(englishString(R.string.action_next_level))
        composeRule.onAllNodesWithText(context.getString(R.string.action_retry)).assertCountEquals(0)
        composeRule.onAllNodesWithText(context.getString(R.string.action_replay)).assertCountEquals(0)
        tap(R.string.action_next_level)

        assertThat(nextLevels).isEqualTo(1)
    }

    @Test
    fun aStuckChallengeResultOffersOnlyTheTurkishRetryBesideHome() {
        render(CHALLENGE_STUCK)

        assertThat(context.getString(R.string.results_headline_stuck))
            .isNotEqualTo(englishString(R.string.results_headline_stuck))
        composeRule.onNodeWithText(context.getString(R.string.results_headline_stuck)).assertIsDisplayed()
        composeRule.onAllNodesWithText(context.getString(R.string.action_next_level)).assertCountEquals(0)
        tap(R.string.action_retry)

        assertThat(retries).isEqualTo(1)
    }

    private fun render(state: ResultsUiState) {
        composeRule.setContent {
            AppTheme {
                ResultsScreen(
                    state = state,
                    onReplay = { replays++ },
                    onCatalog = { catalogs++ },
                    onHome = { homes++ },
                    onNextLevel = { nextLevels++ },
                    onRetry = { retries++ },
                    onNavigateBack = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun tap(id: Int) {
        composeRule.onNodeWithText(context.getString(id)).performScrollTo().performClick()
        composeRule.waitForIdle()
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val RESULTS_TURKISH_SDK = 34

private const val TWO_STARS = 2
private const val THREE_STARS = 3
private const val ONE_STAR = 1
private const val ONE_PEG = 1
private const val FIVE_PEGS = 5
private const val ELAPSED_SECONDS = 75
private const val SCORE = 1280
private const val CHALLENGE_LEVEL = 5

/** A 2-star (Solved) result on English-33: Replay, Catalog and Home. */
private val CLASSIC_SOLVED =
    ResultsUiState(
        stars = TWO_STARS,
        pegsRemaining = FIVE_PEGS,
        elapsedSeconds = ELAPSED_SECONDS,
        score = SCORE,
        headlineTier = ResultsHeadlineTier.SOLVED,
        replayId = ClassicLevelIds.ENGLISH33,
    )

/** A solved Challenge level: Next level and Home. */
private val CHALLENGE_SOLVED =
    ResultsUiState(
        stars = THREE_STARS,
        pegsRemaining = ONE_PEG,
        elapsedSeconds = ELAPSED_SECONDS,
        score = SCORE,
        headlineTier = ResultsHeadlineTier.PERFECT,
        replayId = "",
        challengeLevel = CHALLENGE_LEVEL,
    )

/** A stuck Challenge level: Retry and Home. */
private val CHALLENGE_STUCK =
    ResultsUiState(
        stars = ONE_STAR,
        pegsRemaining = FIVE_PEGS,
        elapsedSeconds = ELAPSED_SECONDS,
        score = SCORE,
        headlineTier = ResultsHeadlineTier.STUCK,
        replayId = "",
        challengeLevel = CHALLENGE_LEVEL,
    )
