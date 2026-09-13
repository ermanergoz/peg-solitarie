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
 * 3. **[of] is the enum-to-ID map, and it is exhaustive at compile time.** It takes the
 *    `:domain` [AchievementId] — the app's only achievement identity — and returns the
 *    matching [Achievements] accessor. `:domain` never learns an ID string; it travels
 *    as an enum constant and becomes text exactly here.
 */
object GamesIds {
    /**
     * The Play Games achievement ID for [id] (D-17).
     *
     * A single `when` over all twenty constants with **no `else` branch**, in the enum's
     * own declaration order so the two lists can be diffed by eye.
     *
     * **The missing `else` is the point.** A twenty-first [AchievementId] must fail to
     * compile here, because both alternatives an `else` admits are worse than a build
     * error: returning a placeholder submits the *wrong* achievement to Play Games under a
     * valid-looking ID, and throwing crashes a fire-and-forget submission pass that has no
     * handler of its own. The same argument `AchievementRules.clearedAchievement` and
     * [com.app.pegsolitaire.data.ClassicBoardIds.levelIdFor] make for [ClassicLevelIds].
     *
     * ## The `CyclomaticComplexMethod` suppression
     *
     * detekt counts every `when` arm as a decision point, so a total map over a 20-constant
     * enum scores 21 against a threshold of 14 — by construction, and the score rises with
     * the roster rather than with anything a reader would call complexity. There is no
     * branching logic here at all: each arm is one constant, there are no conditions, no
     * nesting and no early returns, and the compiler's own exhaustiveness check is what
     * makes the function total.
     *
     * The plan's suggested remedy — splitting into `ofOneShot` / `ofTiered` — was tried and
     * rejected, because it **cannot be done without reintroducing an `else`**: a `when` over
     * [AchievementId] listing only the fourteen one-shots is not exhaustive, so each half
     * would need an `else` throwing or returning a placeholder, which is precisely the
     * compile-time guarantee this function exists to provide. A suppression that keeps one
     * total, exhaustive map is strictly safer than two partial ones that compile when the
     * roster grows. Follows [PlayGamesService]'s documented `TooManyFunctions` precedent:
     * suppress the metric, and state why the metric is measuring the wrong thing.
     */
    @Suppress("CyclomaticComplexMethod") // see "The CyclomaticComplexMethod suppression" above
    fun of(id: AchievementId): String =
        when (id) {
            AchievementId.FIRST_SOLVE -> Achievements.firstSolve
            AchievementId.FIRST_THREE_STAR -> Achievements.firstThreeStar
            AchievementId.CHALLENGE_LEVEL_5 -> Achievements.challengeLevel5
            AchievementId.SPEED_SOLVE -> Achievements.speedSolve
            AchievementId.HINT_FREE -> Achievements.hintFree
            AchievementId.UNDO_FREE -> Achievements.undoFree
            AchievementId.CENTER_FINISH -> Achievements.centerFinish
            AchievementId.LADDER_10 -> Achievements.ladder10
            AchievementId.LADDER_50 -> Achievements.ladder50
            AchievementId.LADDER_100 -> Achievements.ladder100
            AchievementId.STARS_25 -> Achievements.stars25
            AchievementId.STARS_100 -> Achievements.stars100
            AchievementId.STARS_250 -> Achievements.stars250
            AchievementId.CLEARED_ENGLISH33 -> Achievements.clearedEnglish33
            AchievementId.CLEARED_EUROPEAN37 -> Achievements.clearedEuropean37
            AchievementId.CLEARED_ASYMMETRIC39 -> Achievements.clearedAsymmetric39
            AchievementId.CLEARED_DIAMOND41 -> Achievements.clearedDiamond41
            AchievementId.CLEARED_WIEGLEB45 -> Achievements.clearedWiegleb45
            AchievementId.GRAND_TOUR -> Achievements.grandTour
            AchievementId.PERFECTIONIST -> Achievements.perfectionist
        }

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
     * [of] maps the `:domain` enum onto exactly these accessors; the order below follows
     * `GamesIdKeys.ACHIEVEMENTS` so the two lists can be read side by side.
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
