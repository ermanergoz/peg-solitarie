package com.app.pegsolitaire.domain.telemetry

/**
 * The per-solve facts that scoring does not care about but achievements do (D-08).
 *
 * Pure-domain value type with zero Android dependencies (QUAL-01), deliberately kept
 * SEPARATE from `ScoreResult` rather than bolted onto it: `ScoreResult` is a scoring
 * type — stars, score, pegs, elapsed seconds — and stays one. This carrier rides
 * *alongside* it through `GameViewModel`'s finish event and the Results nav args, and
 * `ProgressRepository.recordResult` (plan 12-06) writes it into `play_session` in the
 * same transaction as the `level_result` row.
 *
 * ## What counts (D-09)
 *
 * **Every** hint and **every** undo counts, whether it came out of the free allowance
 * (`AssistAllowance`) or was bought with a rewarded ad — there is no "this one was paid
 * for so it does not spoil the run" carve-out. The counters persist in the resume
 * snapshot as nullable fields, so a resumed game cannot launder an assist by killing the
 * process; an old snapshot with the fields absent reads as [NONE]. Restarting a level
 * and starting a new one both reset the counters.
 *
 * ## What this type is NOT
 *
 * It is **not** an input to `AchievementRules.evaluate` (D-11). The rules are a pure
 * function of `ProgressAggregate` only, so single-solve facts reach them exclusively
 * after the data layer has folded them into the aggregate's EXISTS flags. That is what
 * lets one rule pass serve both a live solve and the offline backfill.
 *
 * @property hintsUsed Hints taken during this solve, free or rewarded; never negative —
 *   [coerced] resolves a forged negative to [UNACCOUNTED_ASSIST] before this type exists.
 * @property undosUsed Undos taken during this solve, free or rewarded; never negative —
 *   same resolution as [hintsUsed].
 * @property lastPegOnTarget Whether the final surviving peg sat on the board's target
 *   hole. Only English-33 finishing on `(3, 3)` earns the Center-finish achievement
 *   (D-01); the flag is recorded for every board, and the *achievement* predicate that
 *   narrows it to English-33 lives in the data layer's aggregate query, not here.
 */
data class SolveTelemetry(
    val hintsUsed: Int,
    val undosUsed: Int,
    val lastPegOnTarget: Boolean,
) {
    companion object {
        /**
         * The zero telemetry: no assists, not a target finish.
         *
         * This is what an old resume blob written before D-09's fields existed, or a
         * missing Results nav argument, denotes — "absent fields read as zero" (D-09).
         */
        val NONE = SolveTelemetry(hintsUsed = 0, undosUsed = 0, lastPegOnTarget = false)

        /**
         * What a FORGED NEGATIVE counter resolves to: one unaccounted assist.
         *
         * A negative count is not a legacy state and never a real one — no writer in this
         * codebase can produce it — so it is tampering, and resolving tampering in the
         * tamperer's favour is the one thing this boundary must not do. The Hint-free and
         * Undo-free predicates are `= 0` over `play_session`, so ANY non-zero value
         * disqualifies purity; one is the smallest honest way to say "assists were taken
         * and this play cannot be counted pure", and it stays a plausible count rather
         * than a sentinel that would look absurd anywhere it is ever displayed or summed.
         *
         * Named rather than written inline because it is a *policy*, not arithmetic —
         * the `SnapshotMapper.NO_ASSISTS_RECORDED` discipline, and its exact mirror
         * image: that constant is the ABSENT case (zero, per D-09, fail-open toward the
         * achievement and accepted as such), this one is the FORGED case.
         */
        const val UNACCOUNTED_ASSIST = 1

        /**
         * Telemetry built from untrusted counters, **resolved** rather than refused
         * (T-12-06 / ASVS V5).
         *
         * The `Results` destination is independently reachable in `AppNavHost` and the
         * resume snapshot is an on-device, player-editable blob, so both counters are an
         * untrusted boundary by construction. The posture copied here is
         * `AssistAllowance.fromStoredOrFull`'s: resolve into a sane range, never crash,
         * never refuse the write. Refusing would let a one-byte edit destroy an honest
         * player's recorded progress in exchange for denying a cheat a cosmetic badge.
         *
         * Three cases, and the directions genuinely differ:
         *  - An **absent** arg never reaches here as a distinct value — the `Results`
         *    route defaults it to `0` (D-09), which fails OPEN toward the Hint-free /
         *    Undo-free achievements. That is a locked product decision, accepted because
         *    the store is local-only and the exposure is one self-awarded badge.
         *  - A **large** value (a forged `9999` hints) passes through verbatim: it only
         *    ever disqualifies purity, so there is nothing to defend against.
         *  - A **negative** value resolves to [UNACCOUNTED_ASSIST], NOT to zero. It is
         *    forgery rather than history, and zero would be the one resolution that pays
         *    the forger — it is precisely the value the Hint-free and Undo-free predicates
         *    are looking for. This mirrors `SnapshotMapper`, which refuses a resume
         *    outright for the same shape; refusing is not available at this boundary,
         *    because the honest player's recorded best rides on the same call.
         *
         * A forged *low but non-negative* count — claiming a hint-free solve that was not
         * — remains the residual explicitly accepted by T-12-04: the only real fix is a
         * server, and this milestone has none.
         */
        fun coerced(
            hintsUsed: Int,
            undosUsed: Int,
            lastPegOnTarget: Boolean,
        ): SolveTelemetry =
            SolveTelemetry(
                hintsUsed = hintsUsed.orUnaccountedIfNegative(),
                undosUsed = undosUsed.orUnaccountedIfNegative(),
                lastPegOnTarget = lastPegOnTarget,
            )

        /**
         * A forged negative count resolved to [UNACCOUNTED_ASSIST]; everything else is
         * taken as written.
         *
         * One resolver rather than two inline expressions, so the two counters cannot
         * drift into disagreeing about what a negative means.
         */
        private fun Int.orUnaccountedIfNegative(): Int = if (this < 0) UNACCOUNTED_ASSIST else this
    }
}
