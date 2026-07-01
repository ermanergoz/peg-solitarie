package com.app.pegsolitaire.navigation

import kotlinx.serialization.Serializable

/*
 * The type-safe navigation contract for the whole Phase-2 nav graph.
 *
 * Every screen is reached by an `@Serializable` route instance and reads its
 * arguments back via `toRoute<Route>()` (NAV-02) — no string keys, no manual
 * `Bundle` parsing, compile-time arg safety. The five routes below are the single
 * source of truth the `NavHost` (02-08), the Classic catalog (02-07), the Results
 * screen (02-06), and `GameViewModel` (this plan) all build against; the field
 * names here are the binding contract (`toRoute<Gameplay>().levelId`,
 * `toRoute<Results>().score`, …), so they must not drift.
 *
 * Routes carry only the minimal identifying state needed to reconstruct a screen
 * (a `levelId`, the already-computed results) — never a `Board`/bitmask; the board
 * is the `LevelRepository`'s single source of truth, looked up from the `levelId`.
 */

/** The mode-select home screen (NAV-01/NAV-02): no arguments. */
@Serializable
data object Home

/** The Classic board catalog (CLAS-01/NAV-02): browse the five boards; no arguments. */
@Serializable
data object ClassicCatalog

/**
 * The playable gameplay screen for one Classic board (CLAS-02).
 *
 * @property levelId the stable catalog id of the board to play; `GameViewModel`
 *   reads it via `savedStateHandle.toRoute<Gameplay>().levelId` and resolves the
 *   board through `LevelRepository.loadClassic(levelId)`.
 */
@Serializable
data class Gameplay(
    val levelId: String,
)

/**
 * The end-of-game results screen (SCOR-03).
 *
 * All values are computed in-game (from the validated `Board.pegCount` and the
 * in-VM monotonic timer, via `ScoreCalculator`) and passed by value — Results is a
 * display-only destination that re-derives nothing trust-sensitive.
 *
 * @property levelId the board that was just played (lets Results offer Replay / Next).
 * @property stars 1-3 earned, from pegs remaining (D-01).
 * @property pegsRemaining pegs left on the terminal board.
 * @property elapsedSeconds whole seconds elapsed (fed the time-bonus decay).
 * @property score the ranking score (peg base + decayed time bonus, D-03).
 */
@Serializable
data class Results(
    val levelId: String,
    val stars: Int,
    val pegsRemaining: Int,
    val elapsedSeconds: Int,
    val score: Int,
)
