package com.app.pegsolitaire.ui.results

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.domain.scoring.ScoreParams
import com.app.pegsolitaire.navigation.Results
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Exposes the immutable [ResultsUiState] for the end-of-game Results destination
 * (SCOR-03) from the typed `Results` route args, with a wrapping "Next board" id
 * (D-06) and a star-tiered headline (D-08).
 *
 * It is a display-only ViewModel: the score/stars/pegs/time arrive already computed
 * by the pure-domain `ScoreCalculator` (02-01) as `Results` route args (read via
 * [SavedStateHandle.toRoute]) and are passed through verbatim — nothing
 * trust-sensitive is re-derived here (T-02-06-ARG). The only computation is the
 * wrapping next-board id, taken against the [LevelRepository.classicCatalog] order
 * (D-18); an unknown/forged `levelId` resolves `indexOf == -1`, so
 * `(-1 + 1) % size == 0` deterministically falls back to the first catalog board
 * rather than crashing (T-02-06-NEXT).
 *
 * @param levelRepository the catalog single-source-of-truth for the next-board wrap.
 * @param savedStateHandle carries the typed `Results(...)` route args.
 */
@HiltViewModel
class ResultsViewModel
    @Inject
    constructor(
        levelRepository: LevelRepository,
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
                    headlineTier = tierFor(args.stars),
                    replayId = args.levelId,
                    nextId = nextIdFrom(levelRepository.classicCatalog().map { it.id }, args.levelId),
                ),
            )

        /** Read-only state stream for the UI; the mutable flow never leaks (state hoisting). */
        val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

        private companion object {
            /** Step to the catalog id one position after the current board (before the wrap). */
            const val NEXT_OFFSET = 1

            /**
             * Maps the earned star count to its headline bucket (D-08): the top band
             * (>= [ScoreParams.THREE_STARS]) is PERFECT, the middle band
             * ([ScoreParams.TWO_STARS]) is SOLVED, everything else (the 1-star floor) is STUCK.
             */
            fun tierFor(stars: Int): ResultsHeadlineTier =
                when {
                    stars >= ScoreParams.THREE_STARS -> ResultsHeadlineTier.PERFECT
                    stars == ScoreParams.TWO_STARS -> ResultsHeadlineTier.SOLVED
                    else -> ResultsHeadlineTier.STUCK
                }

            /**
             * The catalog id immediately after [currentId] in [order], wrapping to the
             * first id past the end (D-06). An unknown [currentId] (`indexOf == -1`)
             * resolves to index 0 — a safe in-catalog board, never a crash (T-02-06-NEXT).
             */
            fun nextIdFrom(
                order: List<String>,
                currentId: String,
            ): String = order[(order.indexOf(currentId) + NEXT_OFFSET) % order.size]
        }
    }
