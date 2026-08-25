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
 * The Settings screen (SET-01/SET-02, D-14): sound + haptics toggles; no arguments.
 *
 * Reached from the Home top-bar gear and popped back to Home. Carrying no args keeps the
 * new route surface inert — nothing untrusted crosses it (threat T-05-08).
 */
@Serializable
data object Settings

/**
 * The playable gameplay screen for one board — Classic or Challenge (CLAS-02 / CHAL-01).
 *
 * A single unified `LevelRef` carried as one route (Open-Q2): Classic navigates with a
 * catalog [levelId] (Challenge context absent → `challengeLevel` null); Challenge
 * navigates with a [challengeLevel] number ([levelId] left at its empty default). The
 * lone `GameViewModel` branches on [challengeLevel] alone — no `if (challenge)` reaches
 * the board/game logic; routing to the right board stays inside `LevelRepository`. Both
 * fields carry defaults so each mode's call site names only the arg it needs and the
 * other stays valid.
 *
 * @property levelId the stable catalog id of a Classic board; `GameViewModel` reads it
 *   via `savedStateHandle.toRoute<Gameplay>().levelId` and resolves the board through
 *   `LevelRepository.loadClassic(levelId)`. Empty for a Challenge level.
 * @property challengeLevel the 1-based Challenge level number when this is a Challenge
 *   game, else `null` (Classic). Resolved via `LevelRepository.loadChallenge(n)`; a
 *   value `< 1` is clamped at the ViewModel boundary (T-03-09).
 * @property resume whether to continue the mode's saved in-progress game rather than
 *   start this board fresh (PERS-03, D-01): the Home Continue affordance navigates with
 *   `resume = true` and `GameViewModel` then rebuilds the board by replaying the stored
 *   snapshot instead of loading/generating it (D-06). A snapshot that is absent or
 *   unusable falls back to the normal load, so the flag can only ever be an optimisation
 *   hint — never a way to reach a board the plain route could not (T-05-03).
 */
@Serializable
data class Gameplay(
    val levelId: String = "",
    val challengeLevel: Int? = null,
    val resume: Boolean = false,
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
 * @property score the ranking score (peg base + decayed time bonus, D-03; level-scaled
 *   by the D-11 multiplier when this was a Challenge game).
 * @property challengeLevel the Challenge level that was just played, or `null` for a
 *   Classic game — lets Results offer the mode-aware Next-level/Retry actions (03-05).
 *   Defaults `null` so Classic call sites stay valid.
 */
@Serializable
data class Results(
    val levelId: String,
    val stars: Int,
    val pegsRemaining: Int,
    val elapsedSeconds: Int,
    val score: Int,
    val challengeLevel: Int? = null,
)
