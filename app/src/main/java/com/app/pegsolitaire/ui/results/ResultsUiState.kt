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
 * @property challengeLevel the Challenge level just played, or `null` for a Classic game.
 *   When non-null the screen renders the mode-aware Challenge actions (Next level / Retry,
 *   D-01/D-10) instead of the Classic Replay / Catalog / Home trio.
 */
@Immutable
data class ResultsUiState(
    val stars: Int,
    val pegsRemaining: Int,
    val elapsedSeconds: Int,
    val score: Int,
    val headlineTier: ResultsHeadlineTier,
    val replayId: String,
    val challengeLevel: Int? = null,
) {
    /**
     * Whether the "Next level" action is offered (D-01): only a solved Challenge level
     * advances. Solved ⇔ exactly one peg by construction, so a stuck Challenge board
     * ([pegsRemaining] >= 2) yields `false` and gets Retry instead (D-10). Always `false`
     * for Classic ([challengeLevel] == null).
     */
    val showNextLevel: Boolean
        get() = challengeLevel != null && pegsRemaining == PEGS_REMAINING_SOLVED
}

/** A solved board leaves exactly one peg — the sole state that unlocks the next level (D-01). */
private const val PEGS_REMAINING_SOLVED = 1

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
