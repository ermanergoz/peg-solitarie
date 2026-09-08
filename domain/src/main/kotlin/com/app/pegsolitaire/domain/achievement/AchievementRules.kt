package com.app.pegsolitaire.domain.achievement

/**
 * What a [ProgressAggregate] has earned: the one-shots to unlock and the absolute step
 * counts to report.
 *
 * Two invariants hold for every outcome [AchievementRules.evaluate] produces, and both
 * are asserted by `AchievementRulesTest`:
 *  - **[steps] never carries a value `<= 0`.** `setSteps` documents `numSteps` as *"must
 *    be greater than 0"*, so a zero-valued entry would be an invalid call the SDK
 *    rejects — and under D-10 a rejected submission sets the pending flag, giving a fresh
 *    install an unclearable dirty state on every clean launch (T-12-05).
 *  - **The two collections are DISJOINT.** A tiered id only ever appears in [steps] and a
 *    one-shot only ever in [unlocked]: Play Games auto-unlocks an incremental achievement
 *    when its steps reach the total, so unlocking one directly would be a redundant call
 *    that also discards the progress bar the tiers exist to show.
 *
 * @property unlocked The ids fed to `GamesService.unlockAchievement`. One-shots only.
 * @property steps The ABSOLUTE values fed to `GamesService.setAchievementSteps` (D-12) —
 *   never deltas, so a resubmitted backfill pass is idempotent.
 */
data class AchievementOutcome(
    val unlocked: Set<AchievementId>,
    val steps: Map<AchievementId, Int>,
)

/**
 * The pure achievement kernel: one pass over the aggregate, one rule set (D-11).
 *
 * [evaluate] is a **total, deterministic, side-effect-free** function of a
 * [ProgressAggregate] and nothing else — no clock, no I/O, no SDK, no hidden state. That
 * is what lets a single call serve both paths D-11 names:
 *  - a **live solve**, whose aggregate already reflects the result just recorded, and
 *  - the **offline backfill**, which has only the aggregate to work from.
 *
 * A player who reached Challenge 50 while signed out therefore receives the ladder tiers,
 * the per-board unlocks, Grand tour and Perfectionist on sign-in through the very same
 * code path a normal finish takes. There is no second rule set and no single-solve
 * overload — deliberately, because a second path is a second thing to keep in agreement.
 *
 * Per-solve facts (hints taken, undos taken, where the last peg landed) are **not** a
 * parameter here. They reach the rules only once the data layer has folded them into the
 * aggregate's counts and EXISTS flags, which is what keeps this function's output a
 * function of *stored history* alone and so recomputable at any time (T-12-04).
 *
 * `:domain` speaks [AchievementId] only: no Play Games ID string, display name,
 * description or point value appears here or ever may (T-12-07).
 */
object AchievementRules {
    /** Everything [aggregate] has earned, as one-shot unlocks plus absolute tier steps. */
    fun evaluate(aggregate: ProgressAggregate): AchievementOutcome =
        AchievementOutcome(
            unlocked = unlockedAchievements(aggregate),
            steps = tierSteps(aggregate),
        )

    /**
     * The fourteen one-shots [aggregate] satisfies.
     *
     * Written as conditional additions into a `buildSet` rather than a chain of early
     * returns: detekt's `ReturnCount` (max 2) has bitten `GameViewModel` and
     * `ResultsViewModel` in this codebase before, and the builder shape also keeps each
     * predicate readable as the single line of policy it is.
     */
    private fun unlockedAchievements(aggregate: ProgressAggregate): Set<AchievementId> =
        buildSet {
            if (aggregate.solvedLevelCount > 0) {
                add(AchievementId.FIRST_SOLVE)
            }
            if (aggregate.threeStarCount > 0) {
                add(AchievementId.FIRST_THREE_STAR)
            }
            // A never-played Challenge history is SQL NULL, which reads as "no level" —
            // not as level 0, and never as a comparison that throws.
            if ((aggregate.highestChallengeLevel ?: 0) >= AchievementThresholds.EARLY_CHALLENGE_LEVEL) {
                add(AchievementId.CHALLENGE_LEVEL_5)
            }
            if (aggregate.anySpeedSolve) {
                add(AchievementId.SPEED_SOLVE)
            }
            // D-01: hint-free and undo-free are mode-agnostic — a qualifying solve in
            // EITHER mode earns them, and the aggregate's EXISTS flag already says so.
            if (aggregate.anyHintFreeSolve) {
                add(AchievementId.HINT_FREE)
            }
            if (aggregate.anyUndoFreeSolve) {
                add(AchievementId.UNDO_FREE)
            }
            // D-01: this flag means English-33 finishing on (3, 3) and nothing else. The
            // rules expose no generalised "finished on the start hole" notion, because no
            // other shipped board has one.
            if (aggregate.anyCenterFinish) {
                add(AchievementId.CENTER_FINISH)
            }
            aggregate.clearedClassicBoards.forEach { board -> add(clearedAchievement(board)) }
            if (aggregate.clearedClassicBoards.containsAll(ClassicBoard.entries)) {
                add(AchievementId.GRAND_TOUR)
            }
            if (aggregate.perfectClassicBoards.containsAll(ClassicBoard.entries)) {
                add(AchievementId.PERFECTIONIST)
            }
        }

    /**
     * The six tiers' ABSOLUTE step values (D-03/D-12) — the ladder tiers all receive the
     * highest Challenge level and the star tiers all receive the total stars.
     *
     * The value is taken verbatim, **never clamped to the tier's own total**: Play Games
     * auto-unlocks an incremental achievement once its steps reach the total and ignores
     * every further mutation, so sending 137 to a 10-step tier is both correct and inert.
     * Clamping would only lose information the next tier still needs.
     */
    private fun tierSteps(aggregate: ProgressAggregate): Map<AchievementId, Int> =
        buildMap {
            putIfPositive(AchievementId.LADDER_10, aggregate.highestChallengeLevel)
            putIfPositive(AchievementId.LADDER_50, aggregate.highestChallengeLevel)
            putIfPositive(AchievementId.LADDER_100, aggregate.highestChallengeLevel)
            putIfPositive(AchievementId.STARS_25, aggregate.totalStars)
            putIfPositive(AchievementId.STARS_100, aggregate.totalStars)
            putIfPositive(AchievementId.STARS_250, aggregate.totalStars)
        }

    /**
     * Records [steps] for [id] only when it is a value `setSteps` accepts — one guarded
     * helper shared by both axes rather than six copies of the same `if` (T-12-05).
     *
     * `null` (an empty Challenge history) and `0` (a brand-new player's star total) are
     * both omissions, not zeroes to send.
     */
    private fun MutableMap<AchievementId, Int>.putIfPositive(
        id: AchievementId,
        steps: Int?,
    ) {
        if (steps != null && steps > 0) {
            put(id, steps)
        }
    }

    /**
     * The per-board first-solve unlock for [board] (D-04).
     *
     * Exhaustive with **no `else` branch** on purpose: a sixth [ClassicBoard] must fail to
     * compile here rather than silently ship a board that earns nothing.
     */
    private fun clearedAchievement(board: ClassicBoard): AchievementId =
        when (board) {
            ClassicBoard.ENGLISH33 -> AchievementId.CLEARED_ENGLISH33
            ClassicBoard.EUROPEAN37 -> AchievementId.CLEARED_EUROPEAN37
            ClassicBoard.ASYMMETRIC39 -> AchievementId.CLEARED_ASYMMETRIC39
            ClassicBoard.DIAMOND41 -> AchievementId.CLEARED_DIAMOND41
            ClassicBoard.WIEGLEB45 -> AchievementId.CLEARED_WIEGLEB45
        }
}
