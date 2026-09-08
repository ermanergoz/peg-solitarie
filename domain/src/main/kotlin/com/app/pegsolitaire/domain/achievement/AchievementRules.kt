package com.app.pegsolitaire.domain.achievement

/**
 * What a [ProgressAggregate] has earned: the one-shots to unlock and the absolute step
 * counts to report.
 *
 * @property unlocked The ids fed to `GamesService.unlockAchievement`. One-shots only.
 * @property steps The ABSOLUTE values fed to `GamesService.setAchievementSteps` (D-12) —
 *   never deltas, so a resubmitted backfill pass is idempotent.
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
 */
data class AchievementOutcome(
    val unlocked: Set<AchievementId>,
    val steps: Map<AchievementId, Int>,
)

/**
 * The pure achievement kernel: one pass, one rule set (D-11).
 *
 * Stub pending the GREEN step — the spec in `AchievementRulesTest` is expected to fail
 * against this.
 */
object AchievementRules {
    /** Stub: yields nothing for every aggregate. */
    fun evaluate(aggregate: ProgressAggregate): AchievementOutcome {
        check(aggregate.totalStars >= 0) { "totalStars is a count" }
        return AchievementOutcome(unlocked = emptySet(), steps = emptyMap())
    }
}
