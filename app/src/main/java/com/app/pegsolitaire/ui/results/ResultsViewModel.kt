package com.app.pegsolitaire.ui.results

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.app.pegsolitaire.domain.scoring.ScoreParams
import com.app.pegsolitaire.navigation.Results
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Exposes the immutable [ResultsUiState] for the end-of-game Results destination
 * (SCOR-03) from the typed `Results` route args, with a star-tiered headline (D-08).
 *
 * It is a display-only ViewModel: the score/stars/pegs/time arrive already computed
 * by the pure-domain `ScoreCalculator` (02-01) as `Results` route args (read via
 * [SavedStateHandle.toRoute]) and are passed through verbatim — nothing
 * trust-sensitive is re-derived here (T-02-06-ARG). The only computation is the
 * star-to-headline-tier mapping; Replay re-plays the played `levelId`. Classic has no
 * "next board" (a Challenge-mode concept), so no catalog lookup is needed.
 *
 * @param savedStateHandle carries the typed `Results(...)` route args.
 */
@HiltViewModel
class ResultsViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val args = savedStateHandle.toRoute<Results>()

        private val _uiState =
            MutableStateFlow(
                ResultsUiState(
                    stars = args.stars,
                    pegsRemaining = args.pegsRemaining,
                    elapsedSeconds = args.elapsedSeconds,
                    score = args.score,
                    headlineTier =
                        tierFor(
                            stars = args.stars,
                            pegsRemaining = args.pegsRemaining,
                            challengeLevel = args.challengeLevel,
                        ),
                    replayId = args.levelId,
                    challengeLevel = args.challengeLevel,
                ),
            )

        /** Read-only state stream for the UI; the mutable flow never leaks (state hoisting). */
        val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

        private companion object {
            /** A solved board leaves exactly one peg; a Challenge board that did not is stuck. */
            const val PEGS_REMAINING_SOLVED = 1

            /**
             * Maps the terminal outcome to its headline bucket (D-08). For a Challenge
             * result the headline must agree with the offered action (WR-03): a board
             * that did NOT fold to a single peg is stuck regardless of star count, so a
             * 2-star Challenge board left with 2-3 pegs reads "Out of moves" (STUCK) over
             * its Retry action instead of a contradictory "Solved!". Otherwise (and for
             * Classic, [challengeLevel] `null`, star-only headline D-08): the top band
             * (>= [ScoreParams.THREE_STARS]) is PERFECT, the middle band
             * ([ScoreParams.TWO_STARS]) is SOLVED, everything else (the 1-star floor) STUCK.
             */
            fun tierFor(
                stars: Int,
                pegsRemaining: Int,
                challengeLevel: Int?,
            ): ResultsHeadlineTier =
                when {
                    challengeLevel != null && pegsRemaining != PEGS_REMAINING_SOLVED ->
                        ResultsHeadlineTier.STUCK
                    stars >= ScoreParams.THREE_STARS -> ResultsHeadlineTier.PERFECT
                    stars == ScoreParams.TWO_STARS -> ResultsHeadlineTier.SOLVED
                    else -> ResultsHeadlineTier.STUCK
                }
        }
    }
