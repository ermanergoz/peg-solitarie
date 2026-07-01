package com.app.pegsolitaire.ui.results

import androidx.compose.runtime.Immutable

/**
 * The single immutable UI state for the end-of-game Results destination (SCOR-03).
 *
 * Owned by `ResultsViewModel` and exposed over a read-only `StateFlow`; the screen
 * is a stateless render of this object (state hoisting). Every value is a `val` and
 * the type is `@Immutable`, so Compose can skip recomposition on equal instances.
 *
 * The four display values ([stars], [pegsRemaining], [elapsedSeconds], [score]) are
 * the typed `Results` route args verbatim — they were computed in-game by the
 * pure-domain `ScoreCalculator` (02-01) and are NEVER re-derived here. [headlineTier]
 * is the star-outcome bucket the screen resolves to its copy (D-08); [replayId] is the
 * catalog id the Replay action navigates to.
 *
 * @property stars 1-3 earned, from pegs remaining (D-01); drives the filled-star row.
 * @property pegsRemaining pegs left on the terminal board (PEGS LEFT metric).
 * @property elapsedSeconds whole seconds elapsed, formatted `mm:ss` (TIME metric).
 * @property score the ranking score (SCORE metric, D-03).
 * @property headlineTier the star-tiered headline bucket (D-08).
 * @property replayId the board just played — Replay re-plays this same level.
 */
@Immutable
data class ResultsUiState(
    val stars: Int,
    val pegsRemaining: Int,
    val elapsedSeconds: Int,
    val score: Int,
    val headlineTier: ResultsHeadlineTier,
    val replayId: String,
)

/**
 * The three star-outcome headline buckets (D-08). The tier → string-resource mapping
 * is resolved in `ResultsScreen` (keeping the copy in `strings.xml`), not here.
 */
enum class ResultsHeadlineTier {
    /** A textbook solve (3 stars) → "Perfect!". */
    PERFECT,

    /** A solid finish (2 stars) → "Solved!". */
    SOLVED,

    /** Out of moves with pegs left (1 star) → "Out of moves". */
    STUCK,
}
