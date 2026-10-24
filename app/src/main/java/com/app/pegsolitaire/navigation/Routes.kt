package com.app.pegsolitaire.navigation

import com.app.pegsolitaire.domain.model.BoardConstants
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
 * The interactive tutorial (ONB-01, D-01/D-01g): three micro-puzzles on a live board; no
 * arguments.
 *
 * Reached from Home on first run and from Settings > How to play on replay, and **its only
 * outbound edge is `popBackStack()`** — so it lands back wherever the player entered from,
 * and it is deliberately *not* reachable from `Gameplay` or `Results`. That absence is a
 * contract, not an omission (§5.1 prohibition 4): a tutorial that could reach `Results` could
 * bank a star, and a banked tutorial star corrupts the Home Stars pill on the player's very
 * first launch.
 *
 * Being a destination is also what scopes its `TutorialViewModel` to one visit: the entry is
 * cleared on pop, so a replay from Settings always restarts at step 1 (D-01g), which an
 * Activity-scoped holder would have got wrong.
 */
@Serializable
data object Tutorial

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
 * [hintsUsed]/[undosUsed]/[lastPegOnTarget] are the D-08 `SolveTelemetry` in flat scalar
 * form. **Flat scalars, not a nested payload**, on purpose: `Results` is already an
 * all-scalar route, and a nested `@Serializable` type in a Navigation 2.9 type-safe route
 * requires a hand-written custom `NavType` — real friction for no benefit at three
 * primitives. Their **defaults are load-bearing rather than cosmetic**: they are what
 * makes this destination reachable without telemetry (every `Results(...)` call site that
 * predates Phase 12, and a process-death restore of an older back stack) read as
 * `SolveTelemetry.NONE` instead of failing to construct.
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
 * @property minimumPegs the finishing board's own `BoardSpec.minimumPegs` — the peg count
 *   at or below which this play counts as SOLVED, which is what decides whether the play
 *   is banked and submitted. It is a per-board fact, not a constant: `GameViewModel`
 *   declares the win with `pegCount <= spec.minimumPegs` and Diamond-41's proven floor is
 *   TWO, so a solved test hardcoded to one peg silently drops every flawless Diamond-41
 *   win. Defaults [BoardConstants.MINIMUM_PEGS_DEFAULT] — the floor of every other shipped
 *   board — so pre-Phase-12 call sites and a process-death restore of an older back stack
 *   stay valid and keep the one-peg meaning they were written with. Untrusted like every
 *   other arg here: `ResultsViewModel` clamps it into the shipped range once, at the nav
 *   boundary, so a forged value cannot declare an unplayed board solved.
 * @property hintsUsed hints the player actually received during this play, free or
 *   rewarded (PGSA-02, D-09). Defaults `0` — absent means "no telemetry carried", which
 *   `ResultsViewModel` reads as `SolveTelemetry.NONE`. Note the direction that default
 *   points: zero is the purest count, so a telemetry-less route fails OPEN toward the
 *   Hint-free / Undo-free achievements. That is D-09's locked choice, accepted because the
 *   only thing riding on it is one self-awarded cosmetic badge. A *forged negative* is a
 *   different case and is resolved by `SolveTelemetry.coerced` to a count that
 *   disqualifies purity, never to zero.
 * @property undosUsed undos that actually rewound the board during this play — same
 *   contract as [hintsUsed], counted independently.
 * @property lastPegOnTarget whether the final surviving peg came to rest on the finishing
 *   board's own target hole (D-01). Recorded for every board; the narrowing to English-33
 *   that decides the Center-finish achievement is a data-layer predicate, not this flag.
 *   Defaults `false`.
 */
@Serializable
data class Results(
    val levelId: String,
    val stars: Int,
    val pegsRemaining: Int,
    val elapsedSeconds: Int,
    val score: Int,
    val challengeLevel: Int? = null,
    val minimumPegs: Int = BoardConstants.MINIMUM_PEGS_DEFAULT,
    val hintsUsed: Int = 0,
    val undosUsed: Int = 0,
    val lastPegOnTarget: Boolean = false,
)

/**
 * One Licenses entry's licence text or texts (spec §4; tech design §3.6).
 *
 * Carries the catalogue entry's stable id, **never an asset path**: route arguments are restorable,
 * forgeable state (the `Gameplay(levelId)` and T-05-08 reasoning), so an id `LicenseCatalog` does
 * not know is the screen's error state rather than a crash or a file lookup, and nothing this route
 * carries reaches `AssetManager.open`. Its only outbound edge is `popBackStack()`.
 *
 * @property entryId a `LicenseCatalog` entry id.
 */
@Serializable
data class LicenseText(
    val entryId: String,
)
