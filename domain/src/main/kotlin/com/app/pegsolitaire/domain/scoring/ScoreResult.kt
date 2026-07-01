package com.app.pegsolitaire.domain.scoring

/**
 * The immutable outcome of scoring a finished board.
 *
 * Pure-domain value type (zero Android dependencies, QUAL-01): all fields are
 * `Int`/`val`, supplied by the caller from the validated `Board.pegCount` and the
 * in-ViewModel monotonic timer. Consumed by `GameRoute`/the Results destination
 * (02-07) to render stars and the ranking score.
 *
 * @property stars 1-3, derived solely from [pegsRemaining] (D-01); never 0.
 * @property score Peg-base points plus a time bonus floored at 0 (D-03).
 * @property pegsRemaining Pegs left on the terminal board (drives [stars]).
 * @property pegsCleared Pegs removed over the game (drives the peg-base score).
 * @property elapsedSeconds Whole seconds elapsed (drives the time-bonus decay).
 */
data class ScoreResult(
    val stars: Int,
    val score: Int,
    val pegsRemaining: Int,
    val pegsCleared: Int,
    val elapsedSeconds: Int,
)
