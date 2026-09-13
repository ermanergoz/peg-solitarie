package com.app.pegsolitaire.platform.games

import com.app.pegsolitaire.BuildConfig
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.domain.achievement.AchievementId

/**
 * The app's single window onto the generated Play Games leaderboard and achievement
 * identifiers (PLAT-06 / D-13 / D-17) — the [com.app.pegsolitaire.platform.ads.AdIds] of
 * this phase.
 *
 * Three things a reader needs to know here:
 *
 * 1. **These are the only `BuildConfig` Play Games field readers in the app.** Everything
 *    that names a leaderboard or an achievement — plan 12-07's `ProgressSubmitter`
 *    included — goes through this object, so there is exactly one place to look when an ID
 *    is wrong and exactly one place to change when a field is renamed. Nothing in `ui/`
 *    ever names an ID (D-13).
 *
 * 2. **The values differ per build type, so they cannot live in source.** They are
 *    resolved at build time from an untracked `local.properties` or a same-named CI
 *    environment variable; an unset key becomes the inert `UNCONFIGURED-<KEY>` on debug
 *    and fails a release invocation outright. See `app/build.gradle.kts` and
 *    `build-logic/src/main/kotlin/GamesIdResolution.kt`. The `gamesIdLiteralCheck` build
 *    gate fails on any ID-shaped literal under `src/main` to keep it that way.
 *
 * 3. **Plan 12-07 will add `fun of(id: AchievementId): String`** — an exhaustive `when`
 *    mapping the `:domain` `AchievementId` enum onto [Achievements]' accessors, so the
 *    enum-to-ID mapping is checked at compile time. That enum is plan 12-02's work and
 *    does **not** exist yet, so this file deliberately stops short of it: there is no
 *    stub enum, no placeholder `of()` and no `TODO` marker here. [Achievements] is structured
 *    so the function drops in beside it without anything moving.
 */
object GamesIds {
    /** RED stub — see plan 12-07 Task 1. The exhaustive map arrives in the GREEN commit. */
    fun of(id: AchievementId): String = Achievements.firstSolve

    /** The seven leaderboards: two Challenge boards plus one per Classic board. */
    object Leaderboards {
        /** Highest Challenge level reached (PGSL-01). */
        val challengeLadder: String get() = BuildConfig.PGS_LB_CHALLENGE_LADDER

        /** Cumulative Challenge score — `SUM(bestScore)` over Challenge (PGSL-02, D-07). */
        val challengeScore: String get() = BuildConfig.PGS_LB_CHALLENGE_SCORE

        /** English-33 board leaderboard (PGSL-03). */
        val classicEnglish33: String get() = BuildConfig.PGS_LB_CLASSIC_ENGLISH33

        /** European-37 board leaderboard (PGSL-03). */
        val classicEuropean37: String get() = BuildConfig.PGS_LB_CLASSIC_EUROPEAN37

        /** Asymmetric-39 board leaderboard (PGSL-03). */
        val classicAsymmetric39: String get() = BuildConfig.PGS_LB_CLASSIC_ASYMMETRIC39

        /** Diamond-41 board leaderboard (PGSL-03). */
        val classicDiamond41: String get() = BuildConfig.PGS_LB_CLASSIC_DIAMOND41

        /** Wiegleb-45 board leaderboard (PGSL-03). */
        val classicWiegleb45: String get() = BuildConfig.PGS_LB_CLASSIC_WIEGLEB45

        /**
         * The board leaderboard for a Classic level id, or `null` if [levelId] names no
         * Classic board (PGSL-03).
         *
         * **The nullability is meaningful, and `null` is not an error.** It means "this
         * level has no board leaderboard", which is the correct and expected answer for
         * every Challenge level — those submit to [challengeLadder] and [challengeScore]
         * instead. The caller in plan 12-07 must therefore treat `null` as *skip*, never
         * as a failure to report or a reason to set the dirty flag.
         *
         * Branches on the [ClassicLevelIds] constants rather than on id strings, so
         * renaming a Classic id is a compile-time change here, not a silent miss.
         */
        fun forClassic(levelId: String): String? =
            when (levelId) {
                ClassicLevelIds.ENGLISH33 -> classicEnglish33
                ClassicLevelIds.EUROPEAN37 -> classicEuropean37
                ClassicLevelIds.ASYMMETRIC39 -> classicAsymmetric39
                ClassicLevelIds.DIAMOND41 -> classicDiamond41
                ClassicLevelIds.WIEGLEB45 -> classicWiegleb45
                else -> null
            }
    }

    /**
     * The twenty achievements of the CONTEXT D-01…D-04 roster.
     *
     * Plan 12-07's `of(AchievementId)` will map the `:domain` enum onto exactly these
     * accessors; the order below follows `GamesIdKeys.ACHIEVEMENTS` so the two lists can
     * be read side by side.
     */
    object Achievements {
        /** First solved level in either mode (D-02). */
        val firstSolve: String get() = BuildConfig.PGS_ACH_FIRST_SOLVE

        /** First three-star result in either mode (D-02). */
        val firstThreeStar: String get() = BuildConfig.PGS_ACH_FIRST_THREE_STAR

        /** Reached Challenge level 5 (D-02). */
        val challengeLevel5: String get() = BuildConfig.PGS_ACH_CHALLENGE_LEVEL_5

        /** Any level solved in under 60 seconds (D-04). */
        val speedSolve: String get() = BuildConfig.PGS_ACH_SPEED_SOLVE

        /** A level solved without using a hint, either mode (D-01). */
        val hintFree: String get() = BuildConfig.PGS_ACH_HINT_FREE

        /** A level solved without using an undo, either mode (D-01). */
        val undoFree: String get() = BuildConfig.PGS_ACH_UNDO_FREE

        /** English-33 solved with the last peg on the centre hole (D-01). */
        val centerFinish: String get() = BuildConfig.PGS_ACH_CENTER_FINISH

        /** First stored result on English-33 (D-04). */
        val clearedEnglish33: String get() = BuildConfig.PGS_ACH_CLEARED_ENGLISH33

        /** First stored result on European-37 (D-04). */
        val clearedEuropean37: String get() = BuildConfig.PGS_ACH_CLEARED_EUROPEAN37

        /** First stored result on Asymmetric-39 (D-04). */
        val clearedAsymmetric39: String get() = BuildConfig.PGS_ACH_CLEARED_ASYMMETRIC39

        /** First stored result on Diamond-41 (D-04). */
        val clearedDiamond41: String get() = BuildConfig.PGS_ACH_CLEARED_DIAMOND41

        /** First stored result on Wiegleb-45 (D-04). */
        val clearedWiegleb45: String get() = BuildConfig.PGS_ACH_CLEARED_WIEGLEB45

        /** Ladder tier — Challenge level 10, incremental (D-03). */
        val ladder10: String get() = BuildConfig.PGS_ACH_LADDER_10

        /** Ladder tier — Challenge level 50, incremental (D-03). */
        val ladder50: String get() = BuildConfig.PGS_ACH_LADDER_50

        /** Ladder tier — Challenge level 100, incremental (D-03). */
        val ladder100: String get() = BuildConfig.PGS_ACH_LADDER_100

        /** Star tier — 25 total stars, incremental (D-03). */
        val stars25: String get() = BuildConfig.PGS_ACH_STARS_25

        /** Star tier — 100 total stars, incremental (D-03). */
        val stars100: String get() = BuildConfig.PGS_ACH_STARS_100

        /** Star tier — 250 total stars, incremental (D-03). */
        val stars250: String get() = BuildConfig.PGS_ACH_STARS_250

        /** A stored result on every one of the five Classic boards (D-04). */
        val grandTour: String get() = BuildConfig.PGS_ACH_GRAND_TOUR

        /** Every Classic board's best is at that board's minimum-peg finish (D-04). */
        val perfectionist: String get() = BuildConfig.PGS_ACH_PERFECTIONIST
    }
}
