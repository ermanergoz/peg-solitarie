package com.app.pegsolitaire.ui.results

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.data.ProgressRepository
import com.app.pegsolitaire.domain.scoring.ScoreParams
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.app.pegsolitaire.navigation.Results
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Exposes the immutable [ResultsUiState] for the end-of-game Results destination
 * (SCOR-03) from the typed `Results` route args, with a star-tiered headline (D-08),
 * and persists the play's per-level best (PERS-01).
 *
 * The score/stars/pegs/time arrive already computed by the pure-domain
 * `ScoreCalculator` (02-01) as `Results` route args (read via [SavedStateHandle.toRoute])
 * and are passed through verbatim — nothing trust-sensitive is re-derived here
 * (T-02-06-ARG). Beyond the star-to-headline-tier mapping, the ViewModel's one side
 * effect is a single [ProgressRepository.recordResult] call: the repository merges each
 * best independently (D-07) and reports which fields improved, which becomes the
 * "New best!" cue on the screen (D-09).
 *
 * Two guards keep that write honest:
 *  - **Only a solved play is recorded (D-08).** A board that did not fold to a single
 *    peg reaches Results in its stuck/Retry state (D-10) and must not leave a stored
 *    best behind for a level that was never solved.
 *  - **It is recorded at most once (T-05-09).** The guard flag lives in the
 *    [SavedStateHandle], so neither a configuration change (same instance) nor a
 *    process-death restore (new instance, same handle) can double-count the play.
 *
 * Replay re-plays the played `levelId`. Classic has no "next board" (a Challenge-mode
 * concept), so no catalog lookup is needed.
 *
 * @param savedStateHandle carries the typed `Results(...)` route args and the
 *   already-recorded guard.
 * @param progressRepository the single source of truth for per-level bests.
 */
@HiltViewModel
class ResultsViewModel
    @Inject
    constructor(
        private val savedStateHandle: SavedStateHandle,
        private val progressRepository: ProgressRepository,
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

        init {
            recordSolvedPlayOnce()
        }

        /**
         * Merges this play into the level's stored bests exactly once, then surfaces the
         * returned per-field flags as the one-shot "New best!" cue (D-09).
         *
         * Returns without writing when the play was not a solve (D-08) or when this play
         * has already been recorded — the flag is set *before* the write is launched, so
         * a second ViewModel built over the same handle cannot race a duplicate in.
         */
        private fun recordSolvedPlayOnce() {
            if (args.pegsRemaining != PEGS_REMAINING_SOLVED) return
            if (savedStateHandle.get<Boolean>(RECORDED_KEY) == true) return
            savedStateHandle[RECORDED_KEY] = true

            val mode =
                if (args.challengeLevel != null) {
                    ProgressPersistenceKeys.MODE_CHALLENGE
                } else {
                    ProgressPersistenceKeys.MODE_CLASSIC
                }
            val levelId = args.challengeLevel?.toString() ?: args.levelId

            viewModelScope.launch {
                val newBests =
                    progressRepository.recordResult(
                        mode = mode,
                        levelId = levelId,
                        result = scoreResultFromArgs(),
                        nowMs = System.currentTimeMillis(),
                    )
                _uiState.update { it.copy(newBests = newBests) }
            }
        }

        /**
         * Rebuilds the domain [ScoreResult] from the route args so the repository merges
         * the same numbers the screen displays. `pegsCleared` is not carried by the route
         * (the Results screen never shows it) and is not part of any stored best, so it is
         * filled with [PEGS_CLEARED_NOT_CARRIED] rather than being re-derived here.
         */
        private fun scoreResultFromArgs(): ScoreResult =
            ScoreResult(
                stars = args.stars,
                score = args.score,
                pegsRemaining = args.pegsRemaining,
                pegsCleared = PEGS_CLEARED_NOT_CARRIED,
                elapsedSeconds = args.elapsedSeconds,
            )

        private companion object {
            /** A solved board leaves exactly one peg; a Challenge board that did not is stuck. */
            const val PEGS_REMAINING_SOLVED = 1

            /** SavedStateHandle flag marking this play as already merged into the stored bests. */
            const val RECORDED_KEY = "results_recorded"

            /** The `Results` route does not carry pegsCleared, and no stored best uses it. */
            const val PEGS_CLEARED_NOT_CARRIED = 0

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
