package com.app.pegsolitaire.domain.achievement

/**
 * The fixed 20-achievement roster (D-05), as the only achievement identity `:domain`
 * speaks.
 *
 * Twenty visible achievements means twenty unique 512×512 icons; plan 12-08 owns the
 * Play Console checklist that creates them and updates the ROADMAP's External
 * Prerequisites row. `AchievementCatalogTest` asserts the size, so a silently dropped or
 * added constant fails the build instead of quietly desynchronising the code from the
 * Console entries.
 *
 * ## The layering rule
 *
 * The `AchievementId → String` Play Games ID map lives in `platform/games/GamesIds.kt`
 * (plans 12-04/12-07), fed from `BuildConfig` (D-17); display names, descriptions and
 * point values live only in plan 12-08's operator checklist. **None of the three may ever
 * appear in `:domain`** — the enum travels, the string does not (T-12-07). That is what
 * makes "nothing in `ui/` touches an ID" true structurally rather than by convention.
 *
 * @property stepTotal For the six **incremental** tiers, the step count Play Games was
 *   configured with — which is that tier's [AchievementThresholds] value, named once and
 *   inlined at compile time (these are `const val`s, so there is no class-initialisation
 *   cycle). `null` for the fourteen one-shots.
 */
enum class AchievementId(
    val stepTotal: Int? = null,
) {
    /** Any level solved, either mode — `solvedLevelCount > 0` (D-02). */
    FIRST_SOLVE,

    /** Any level solved at 3 stars, either mode — `threeStarCount > 0` (D-02). */
    FIRST_THREE_STAR,

    /** Reached Challenge level [AchievementThresholds.EARLY_CHALLENGE_LEVEL] (D-02). */
    CHALLENGE_LEVEL_5,

    /** Any level solved in under [AchievementThresholds.SPEED_SOLVE_MAX_ELAPSED_SECONDS] seconds (D-04). */
    SPEED_SOLVE,

    /** Any level solved without a single hint, either mode (D-01). */
    HINT_FREE,

    /** Any level solved without a single undo, either mode (D-01). */
    UNDO_FREE,

    /** English-33 solved with the last peg on the centre hole `(3, 3)` — that board only (D-01). */
    CENTER_FINISH,

    /** Challenge ladder tier 1 — incremental, driven by absolute steps (D-03/D-12). */
    LADDER_10(stepTotal = AchievementThresholds.LADDER_TIER_1),

    /** Challenge ladder tier 2 — incremental (D-03/D-12). */
    LADDER_50(stepTotal = AchievementThresholds.LADDER_TIER_2),

    /** Challenge ladder tier 3 — incremental (D-03/D-12). */
    LADDER_100(stepTotal = AchievementThresholds.LADDER_TIER_3),

    /** Total-stars tier 1 — incremental (D-03/D-12). */
    STARS_25(stepTotal = AchievementThresholds.STARS_TIER_1),

    /** Total-stars tier 2 — incremental (D-03/D-12). */
    STARS_100(stepTotal = AchievementThresholds.STARS_TIER_2),

    /** Total-stars tier 3 — incremental (D-03/D-12). */
    STARS_250(stepTotal = AchievementThresholds.STARS_TIER_3),

    /** First solve of [ClassicBoard.ENGLISH33] (D-04). */
    CLEARED_ENGLISH33,

    /** First solve of [ClassicBoard.EUROPEAN37] (D-04). */
    CLEARED_EUROPEAN37,

    /** First solve of [ClassicBoard.ASYMMETRIC39] (D-04). */
    CLEARED_ASYMMETRIC39,

    /** First solve of [ClassicBoard.DIAMOND41] (D-04). */
    CLEARED_DIAMOND41,

    /** First solve of [ClassicBoard.WIEGLEB45] (D-04). */
    CLEARED_WIEGLEB45,

    /** Every one of the [AchievementThresholds.CLASSIC_BOARD_TOTAL] Classic boards cleared (D-04). */
    GRAND_TOUR,

    /** Every Classic board's stored best in its own 3-star band (D-04). */
    PERFECTIONIST,
    ;

    /**
     * Whether this achievement is driven by absolute `setAchievementSteps` (D-03/D-12)
     * rather than by a one-shot `unlockAchievement`.
     *
     * True for exactly the six tiers. Play Games auto-unlocks an incremental achievement
     * once its steps reach [stepTotal] and ignores every further mutation, which is what
     * makes resubmitting the same absolute aggregate — as the D-11 backfill does — safe
     * and idempotent, and why the rules never place a tiered id in the unlock set.
     */
    val isIncremental: Boolean get() = stepTotal != null
}
