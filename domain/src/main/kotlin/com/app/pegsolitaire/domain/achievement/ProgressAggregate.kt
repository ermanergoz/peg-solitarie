package com.app.pegsolitaire.domain.achievement

/**
 * The domain-side identity of a shipped Classic board, in D-18 ascending hole-count
 * order (33, 37, 39, 41, 45).
 *
 * `:app`'s level-id string constants map onto this enum at the data boundary in plan
 * 12-06, so `:domain` still never learns an id string — the same rule
 * `Boards.CLASSIC_SPECS`' KDoc states for footprints, applied to achievements.
 * `Boards.CLASSIC_SPECS` carries the geometry in this same order.
 *
 * Exhaustiveness over this enum is what makes the five `CLEARED_*` achievements
 * compiler-checked: `AchievementRules` maps a board to its unlock with a `when` that has
 * no `else` branch, so adding a sixth Classic board fails the build rather than shipping
 * a board that silently earns nothing.
 */
enum class ClassicBoard {
    /** The 33-hole English cross — the Center-finish board (D-01), target `(3, 3)`. */
    ENGLISH33,

    /** The 37-hole European board; starts off-centre, so Center finish never applies. */
    EUROPEAN37,

    /** The 39-hole asymmetric board. */
    ASYMMETRIC39,

    /** The 41-hole diamond, whose proven floor is 2 pegs, not 1 (`DIAMOND41_FEWEST_PEGS_FLOOR`). */
    DIAMOND41,

    /** The 45-hole Wiegleb board. */
    WIEGLEB45,
}

/**
 * The stored-progress snapshot the achievement rules consume — and the ONLY input they
 * take (D-11).
 *
 * Every property is derived by SQL over `level_result` + `play_session` (plan 12-06),
 * and the projection is computed **identically** for a live solve — where the aggregate
 * already reflects the result just recorded — and for the offline backfill. That single
 * shape is what removes the second rule set D-11 forbids: a player who reached Challenge
 * 50 while signed out receives the ladder tiers, the per-board unlocks, Grand tour and
 * Perfectionist on sign-in through the very same `AchievementRules.evaluate` call a
 * normal finish makes.
 *
 * Every property is DEFAULTED, so a brand-new player is exactly `ProgressAggregate()` and
 * a test fixture names only the field under test.
 *
 * Nullability is not decoration: `MAX(...)`/`SUM(...)` over an empty set is SQL `NULL`,
 * and "no Challenge level has ever been solved" is meaningfully different from "the
 * highest solved Challenge level is 0". The non-null counters are the projections plan
 * 12-06 coalesces to `0` at the DAO boundary.
 *
 * @property highestChallengeLevel `MAX(CAST(levelId AS INTEGER))` over
 *   `level_result WHERE mode = 'challenge'`; `null` when no Challenge level is solved.
 *   Note this is highest **solved**, which `ChallengeProgress.currentLevel` (a *next*
 *   level) deliberately does not equal — the off-by-one is expected, not a bug.
 * @property challengeScoreTotal `SUM(bestScore)` over the same rows — the cumulative
 *   Challenge score of PGSL-02 / D-07, i.e. the sum of per-level BESTS, not a running
 *   total of every play. **`Long?`, never `Int?`**: a single Challenge level scores in
 *   the tens of thousands and a wrapped negative would be submitted to the leaderboard.
 *   [AchievementRules] deliberately ignores this field; it rides here so ONE aggregate
 *   serves both the leaderboard and the achievement halves of a single D-11 pass.
 * @property totalStars `SUM(stars)` across BOTH modes — the absolute step value for the
 *   three `STARS_*` tiers (D-03).
 * @property solvedLevelCount `COUNT(*)` over `level_result` across both modes; drives
 *   First solve (D-02).
 * @property threeStarCount `COUNT(*)` where `stars >= 3` across both modes; drives First
 *   3-star (D-02).
 * @property clearedClassicBoards Which Classic boards have a stored result at all. Held
 *   as a per-board SET rather than a bare count because the five `CLEARED_*`
 *   achievements need per-board identity anyway, and deriving Grand tour from the same
 *   set removes the possibility of a count disagreeing with the set it was counted from.
 * @property perfectClassicBoards Which Classic boards have a stored best in their OWN
 *   3-star band (`stars >= AchievementThresholds.PERFECT_STARS`). `ScoreCalculator` bands
 *   the top star against the finishing board's own `BoardSpec.minimumPegs`, so a 3-star
 *   Diamond-41 result IS that board's proven 2-peg floor (D-04) — no special case needed.
 * @property anyHintFreeSolve `EXISTS(SELECT 1 FROM play_session WHERE hintsUsed = 0)`.
 *   **Mode-agnostic** (D-01): a hint-free solve in EITHER mode earns Hint-free.
 * @property anyUndoFreeSolve `EXISTS(SELECT 1 FROM play_session WHERE undosUsed = 0)`,
 *   likewise mode-agnostic (D-01).
 * @property anyCenterFinish `EXISTS(... WHERE mode = 'classic' AND levelId = <English-33>
 *   AND lastPegOnTarget = 1)`. This means **English-33 with the last peg on `(3, 3)`**
 *   and nothing else (D-01) — it is NOT a generalised "finished on the start hole": the
 *   rules expose no such concept, because European-37 starts off-centre, Diamond-41
 *   cannot reach one peg, and Challenge boards have no defined centre.
 * @property anySpeedSolve `EXISTS(... WHERE elapsedMs < :speedLimitMs)` with
 *   `:speedLimitMs` bound from [AchievementThresholds.SPEED_SOLVE_MAX_ELAPSED_MS] — any
 *   level, either mode, solved in under 60 seconds (D-04).
 */
data class ProgressAggregate(
    val highestChallengeLevel: Int? = null,
    val challengeScoreTotal: Long? = null,
    val totalStars: Int = 0,
    val solvedLevelCount: Int = 0,
    val threeStarCount: Int = 0,
    val clearedClassicBoards: Set<ClassicBoard> = emptySet(),
    val perfectClassicBoards: Set<ClassicBoard> = emptySet(),
    val anyHintFreeSolve: Boolean = false,
    val anyUndoFreeSolve: Boolean = false,
    val anyCenterFinish: Boolean = false,
    val anySpeedSolve: Boolean = false,
)
