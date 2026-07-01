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

    private fun viewModelFor(
        levelId: String,
        stars: Int,
        pegsRemaining: Int = SAMPLE_PEGS_REMAINING,
        elapsedSeconds: Int = SAMPLE_ELAPSED_SECONDS,
        score: Int = SAMPLE_SCORE,
    ): ResultsViewModel =
        ResultsViewModel(
            savedStateHandle =
                SavedStateHandle(
                    mapOf(
                        LEVEL_ID_ARG to levelId,
                        STARS_ARG to stars,
                        PEGS_REMAINING_ARG to pegsRemaining,
                        ELAPSED_SECONDS_ARG to elapsedSeconds,
                        SCORE_ARG to score,
                    ),
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

/** Earned-star counts for the three D-08 headline tiers. */
private const val THREE_STARS = 3
private const val TWO_STARS = 2
private const val ONE_STAR = 1

/** Arbitrary pass-through metric values asserted to survive verbatim. */
private const val SAMPLE_PEGS_REMAINING = 3
private const val SAMPLE_ELAPSED_SECONDS = 75
private const val SAMPLE_SCORE = 1234
