package com.app.pegsolitaire.domain.achievement

/**
 * Scoped tunable constants for the achievement roster (PGSA-01..PGSA-03).
 *
 * Split by scope per CLAUDE.md's no-magic-number rule, in the same shape as
 * `ScoreParams`, `AssistAllowanceLimits` and `InterstitialCadenceParams`: every value is
 * a KDoc'd `const val` naming the decision that fixes it, and every predicate in
 * [AchievementRules] reads its threshold from here rather than restating it. detekt's
 * `MagicNumber` exempts constant declarations, which is precisely why this object is the
 * only legal home for these numbers — a second statement of any of them, in the rules,
 * in a test, or in a SQL binding, is a bug waiting for the two copies to drift apart.
 *
 * ## Remit: numbers, and only numbers
 *
 * Display names, descriptions and point values for the 20 achievements are **not** here
 * and never will be: they are operator-facing text owned by plan 12-08's Play Console
 * checklist, and the `AchievementId → String` Play Games ID map lives in
 * `platform/games/GamesIds.kt` outside `:domain` entirely (D-17). `:domain` speaks
 * [AchievementId] and nothing else.
 */
object AchievementThresholds {
    /** Challenge level for the first ladder tier — `LADDER_10` (D-03). */
    const val LADDER_TIER_1 = 10

    /** Challenge level for the second ladder tier — `LADDER_50` (D-03). */
    const val LADDER_TIER_2 = 50

    /** Challenge level for the third ladder tier — `LADDER_100` (D-03). */
    const val LADDER_TIER_3 = 100

    /** Total stars for the first star tier — `STARS_25` (D-03). */
    const val STARS_TIER_1 = 25

    /** Total stars for the second star tier — `STARS_100` (D-03). */
    const val STARS_TIER_2 = 100

    /** Total stars for the third star tier — `STARS_250` (D-03). */
    const val STARS_TIER_3 = 250

    /**
     * The Challenge level that earns the one-shot first-hour win (D-02).
     *
     * A one-shot, NOT a tier: it is deliberately below [LADDER_TIER_1] so a new player
     * banks something inside the first session, before the incremental ladder has moved
     * far enough to show meaningful progress.
     */
    const val EARLY_CHALLENGE_LEVEL = 5

    /**
     * The **exclusive** upper bound on `elapsedSeconds` for the Speed-solve one-shot
     * (D-04) — "under 60 seconds", so exactly 60 does not qualify.
     */
    const val SPEED_SOLVE_MAX_ELAPSED_SECONDS = 60

    /** Milliseconds in one second, so [SPEED_SOLVE_MAX_ELAPSED_MS] can be derived. */
    const val MILLIS_PER_SECOND = 1_000L

    /**
     * The Speed-solve bound in milliseconds — the unit `play_session.elapsedMs` is
     * actually stored and queried in.
     *
     * DERIVED from [SPEED_SOLVE_MAX_ELAPSED_SECONDS] and [MILLIS_PER_SECOND] rather than
     * written as a standalone `60_000L`, per `InterstitialCadenceParams.SESSION_GAP_MS`'
     * discipline: the stated 60 seconds and the compared milliseconds cannot drift apart.
     * Plan 12-06 binds THIS value as the SQL `:speedLimitMs` parameter, so the 60 exists
     * exactly once in the codebase.
     */
    const val SPEED_SOLVE_MAX_ELAPSED_MS = SPEED_SOLVE_MAX_ELAPSED_SECONDS * MILLIS_PER_SECOND

    /**
     * The star count that makes a stored Classic best "perfect" for Perfectionist (D-04).
     *
     * Perfectionist reduces to a star comparison rather than a peg comparison because
     * `ScoreCalculator` already bands the top star against the FINISHING board's own
     * `BoardSpec.minimumPegs` — so a 3-star Diamond-41 result IS a 2-peg finish at that
     * board's proven floor, and no per-board peg table is needed here.
     *
     * Equal by construction to `ScoreParams.THREE_STARS` — the same number viewed from
     * the achievement side rather than the scoring side. The two packages deliberately do
     * not import each other for it (the `ScoreParams.THREE_STAR_MAX_PEGS` /
     * `BoardConstants.MINIMUM_PEGS_DEFAULT` precedent); `AchievementCatalogTest` asserts
     * belt-and-braces that they have not drifted.
     */
    const val PERFECT_STARS = 3

    /**
     * How many Classic boards must be cleared for Grand tour, and be perfect for
     * Perfectionist (D-04).
     *
     * DERIVED from [ClassicBoard] rather than written as a literal `5`: shipping a sixth
     * Classic board must move this denominator automatically, not leave a stale 5 that
     * hands out Grand tour one board early. Plan 12-08's Play Console checklist quotes
     * this count, and [AchievementId] carries one `CLEARED_*` constant per entry.
     */
    val CLASSIC_BOARD_TOTAL: Int = ClassicBoard.entries.size
}
