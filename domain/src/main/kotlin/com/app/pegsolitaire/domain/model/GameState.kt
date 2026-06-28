package com.app.pegsolitaire.domain.model

/**
 * The full immutable state of a game in progress: the current [board], the
 * ordered [moveHistory] of jumps applied so far, and the lifecycle [status].
 *
 * Every move produces a brand-new [GameState] via `copy` — no field is ever
 * mutated in place, so a `StateFlow<GameState>` recomposes reliably.
 */
data class GameState(
    val board: Board,
    val moveHistory: List<Move>,
    val status: GameStatus,
)
