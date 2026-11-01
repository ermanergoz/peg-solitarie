package com.app.pegsolitaire.data.db

/*
 * The Room projection row types behind
 * `com.app.pegsolitaire.domain.achievement.ProgressAggregate` (plan 12-06).
 *
 * None of them carries a Room annotation, and none needs one: Room matches a query's
 * RESULT COLUMN NAMES to constructor parameter names, so **every SQL alias in
 * [LevelResultDao] and [PlaySessionDao] must equal the parameter name here character for
 * character**. A renamed parameter without a renamed alias is a KSP-time failure, not a
 * silent null — which is why these three types are the readable contract for the four
 * aggregate queries.
 *
 * They exist because the ROADMAP's Phase 12 note requires every submitted number to be
 * produced by SQL rather than an in-memory fold over rows: the Challenge ladder reaches
 * thousands of rows, and folding them in Kotlin would load the whole history to compute
 * a `MAX`.
 */

/**
 * The scalar totals over `level_result`, across both modes except where stated.
 *
 * @property highestChallengeLevel `MAX(CAST(levelId AS INTEGER))` over Challenge rows —
 *   the highest level actually BANKED (PGSL-01). **Nullable on purpose**: `MAX` over an
 *   empty set is SQL `NULL`, and "no Challenge level solved yet" is meaningfully
 *   different from "the highest solved level is 0".
 * @property challengeScoreTotal `SUM(bestScore)` over the same Challenge rows — the
 *   cumulative Challenge score of PGSL-02 / D-07, i.e. the sum of per-level BESTS rather
 *   than a running total of every play. **`Long`, not `Int`**: `bestScore` is an `Int`
 *   column, but a single Challenge level scores in the tens of thousands, so the sum over
 *   tens of thousands of banked levels overflows `Int.MAX_VALUE` — and a wrapped negative
 *   would be submitted to a public leaderboard (Pitfall 6). Nullable for the same
 *   empty-set reason as above.
 * @property totalStars `SUM(stars)` across BOTH modes, coalesced to `0` — the star tiers
 *   count stars everywhere (D-03).
 * @property solvedLevelCount `COUNT(*)` across both modes; drives First solve (D-02).
 * @property threeStarCount `COUNT(*)` where `stars >= PERFECT_STARS`, both modes; drives
 *   First 3-star (D-02). `COUNT` never yields `NULL`, so neither counter is coalesced or
 *   nullable.
 */
data class LevelResultTotals(
    val highestChallengeLevel: Int?,
    val challengeScoreTotal: Long?,
    val totalStars: Int,
    val solvedLevelCount: Int,
    val threeStarCount: Int,
)

/**
 * The two derived values the Home progress ribbon reads, in ONE row (§7 R-1/R-2).
 *
 * A separate projection from [LevelResultTotals] rather than a widening of it, because the
 * two answer different questions: `totals()` is a `suspend` one-shot for the achievement
 * submission pass, and its `totalStars` deliberately carries no `WHERE`, so it structurally
 * cannot count Classic boards. This one is `Flow`-returning — Home must not go stale after a
 * solve — and is the *only* read on the start destination.
 *
 * @property totalStars `SUM(stars)` across BOTH modes, coalesced to `0`. Both-modes on
 *   purpose: the pill's label is "Stars", a total, and it is the same number the D-03 star
 *   tiers count. `COALESCE` because `SUM` over an empty table is SQL `NULL` and a brand-new
 *   player has zero stars, not an unknown number of them.
 * @property classicBoardsCleared `COUNT(*)` over Classic rows at or above the bound star
 *   floor — `ProgressConstants.ANY_STARS`, the same predicate the achievement aggregate's
 *   cleared set uses. A stored row means the board was cleared regardless of how well, so
 *   the floor is `0` and not `1`. `COUNT` never yields `NULL`.
 */
data class HomeProgressTotals(
    val totalStars: Int,
    val classicBoardsCleared: Int,
)

/**
 * One stored Classic board's standing — the row behind the cleared set, the perfect set
 * and the per-board bests of PGSL-03, all three read from the SAME query.
 *
 * `level_result`'s primary key is `(mode, levelId)`, so the table already holds **at most
 * one row per board**. No `GROUP BY` is involved and no aggregation can double-count a
 * replay; the row IS the board's standing.
 *
 * @property levelId The stored id text; translated to a `ClassicBoard` by
 *   `ClassicBoardIds.boardFor`, which drops an id naming no shipped board.
 * @property stars The board's best star rating; `>= PERFECT_STARS` is what makes it
 *   perfect (D-04).
 * @property bestScore The board's best score — the value its Classic leaderboard takes.
 */
data class ClassicStanding(
    val levelId: String,
    val stars: Int,
    val bestScore: Int,
)

/**
 * The four `play_session` existence predicates, one `EXISTS(...)` each.
 *
 * Every property is named identically to its `ProgressAggregate` counterpart, so the
 * repository's assembly is a positional copy with nothing to get subtly wrong.
 *
 * @property anyHintFreeSolve Any solved play with `hintsUsed = 0`, in EITHER mode (D-01).
 * @property anyUndoFreeSolve Any solved play with `undosUsed = 0`, in EITHER mode (D-01).
 * @property anyCenterFinish Any English-33 Classic play that finished on the target hole,
 *   and nothing else (D-01) — the narrowing lives in the SQL, because `GameViewModel`
 *   records the raw flag for every board.
 * @property anySpeedSolve Any play under `AchievementThresholds.SPEED_SOLVE_MAX_ELAPSED_MS`
 *   milliseconds, any level, either mode (D-04).
 */
data class PlaySessionFlags(
    val anyHintFreeSolve: Boolean,
    val anyUndoFreeSolve: Boolean,
    val anyCenterFinish: Boolean,
    val anySpeedSolve: Boolean,
)
