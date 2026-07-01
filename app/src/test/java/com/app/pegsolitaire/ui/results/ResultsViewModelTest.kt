package com.app.pegsolitaire.ui.results

import androidx.lifecycle.SavedStateHandle
import com.app.pegsolitaire.data.ClassicLevelIds
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Truth coverage of `ResultsViewModel`'s documented behavior: it builds an immutable
 * [ResultsUiState] from the typed `Results` route args, maps stars to the headline tier
 * (D-08), and exposes the played board as `replayId` — without re-deriving any of the
 * passed-through score values. Classic has no "next board", so no catalog lookup exists.
 *
 * Robolectric backs `SavedStateHandle.toRoute<Results>()` (the same harness
 * `GameViewModelTest` uses for `toRoute<Gameplay>()`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class ResultsViewModelTest {
    @Test
    fun replayIdIsThePlayedBoard() {
        val viewModel = viewModelFor(levelId = ClassicLevelIds.ENGLISH33, stars = THREE_STARS)

        assertThat(viewModel.uiState.value.replayId).isEqualTo(ClassicLevelIds.ENGLISH33)
    }

    @Test
    fun headlineTierMapsThreeTwoAndOneStarsToPerfectSolvedAndStuck() {
        assertThat(viewModelFor(levelId = ClassicLevelIds.ENGLISH33, stars = THREE_STARS).uiState.value.headlineTier)
            .isEqualTo(ResultsHeadlineTier.PERFECT)
        assertThat(viewModelFor(levelId = ClassicLevelIds.ENGLISH33, stars = TWO_STARS).uiState.value.headlineTier)
            .isEqualTo(ResultsHeadlineTier.SOLVED)
        assertThat(viewModelFor(levelId = ClassicLevelIds.ENGLISH33, stars = ONE_STAR).uiState.value.headlineTier)
            .isEqualTo(ResultsHeadlineTier.STUCK)
    }

    @Test
    fun theFourScoreArgValuesPassThroughUnchanged() {
        val viewModel =
            viewModelFor(
                levelId = ClassicLevelIds.EUROPEAN37,
                stars = TWO_STARS,
                pegsRemaining = SAMPLE_PEGS_REMAINING,
                elapsedSeconds = SAMPLE_ELAPSED_SECONDS,
                score = SAMPLE_SCORE,
            )

        val state = viewModel.uiState.value
        assertThat(state.stars).isEqualTo(TWO_STARS)
        assertThat(state.pegsRemaining).isEqualTo(SAMPLE_PEGS_REMAINING)
        assertThat(state.elapsedSeconds).isEqualTo(SAMPLE_ELAPSED_SECONDS)
        assertThat(state.score).isEqualTo(SAMPLE_SCORE)
    }

    @Test
    fun solvedChallengeLevelOffersNextLevel() {
        val viewModel =
            viewModelFor(
                levelId = "",
                stars = THREE_STARS,
                pegsRemaining = SOLVED_PEGS_REMAINING,
                challengeLevel = SAMPLE_CHALLENGE_LEVEL,
            )

        val state = viewModel.uiState.value
        assertThat(state.challengeLevel).isEqualTo(SAMPLE_CHALLENGE_LEVEL)
        assertThat(state.showNextLevel).isTrue()
    }

    @Test
    fun stuckChallengeLevelDoesNotOfferNextLevel() {
        val viewModel =
            viewModelFor(
                levelId = "",
                stars = ONE_STAR,
                pegsRemaining = SAMPLE_PEGS_REMAINING,
                challengeLevel = SAMPLE_CHALLENGE_LEVEL,
            )

        assertThat(viewModel.uiState.value.showNextLevel).isFalse()
    }

    @Test
    fun classicResultsNeverOffersNextLevelAndKeepsReplaySemantics() {
        val viewModel =
            viewModelFor(
                levelId = ClassicLevelIds.ENGLISH33,
                stars = THREE_STARS,
                pegsRemaining = SOLVED_PEGS_REMAINING,
                challengeLevel = null,
            )

        val state = viewModel.uiState.value
        assertThat(state.challengeLevel).isNull()
        assertThat(state.showNextLevel).isFalse()
        assertThat(state.replayId).isEqualTo(ClassicLevelIds.ENGLISH33)
    }

    private fun viewModelFor(
        levelId: String,
        stars: Int,
        pegsRemaining: Int = SAMPLE_PEGS_REMAINING,
        elapsedSeconds: Int = SAMPLE_ELAPSED_SECONDS,
        score: Int = SAMPLE_SCORE,
        challengeLevel: Int? = null,
    ): ResultsViewModel =
        ResultsViewModel(
            savedStateHandle =
                SavedStateHandle(
                    buildMap {
                        put(LEVEL_ID_ARG, levelId)
                        put(STARS_ARG, stars)
                        put(PEGS_REMAINING_ARG, pegsRemaining)
                        put(ELAPSED_SECONDS_ARG, elapsedSeconds)
                        put(SCORE_ARG, score)
                        if (challengeLevel != null) put(CHALLENGE_LEVEL_ARG, challengeLevel)
                    },
                ),
        )
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34

/** The `Results(...)` route arg keys (the route's property names). */
private const val LEVEL_ID_ARG = "levelId"
private const val STARS_ARG = "stars"
private const val PEGS_REMAINING_ARG = "pegsRemaining"
private const val ELAPSED_SECONDS_ARG = "elapsedSeconds"
private const val SCORE_ARG = "score"
private const val CHALLENGE_LEVEL_ARG = "challengeLevel"

/** Earned-star counts for the three D-08 headline tiers. */
private const val THREE_STARS = 3
private const val TWO_STARS = 2
private const val ONE_STAR = 1

/** Arbitrary pass-through metric values asserted to survive verbatim. */
private const val SAMPLE_PEGS_REMAINING = 3
private const val SAMPLE_ELAPSED_SECONDS = 75
private const val SAMPLE_SCORE = 1234

/** A solved board leaves exactly one peg — the sole state that unlocks the next level (D-01). */
private const val SOLVED_PEGS_REMAINING = 1

/** An arbitrary in-progress Challenge level number. */
private const val SAMPLE_CHALLENGE_LEVEL = 5
