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
 * @property hintsUsed Hints taken during this solve, free or rewarded; never negative.
 * @property undosUsed Undos taken during this solve, free or rewarded; never negative.
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
         * Telemetry built from untrusted counters, **clamped** rather than refused
         * (T-12-06 / ASVS V5).
         *
         * The `Results` destination is independently reachable in `AppNavHost` and the
         * resume snapshot is an on-device, player-editable blob, so both counters are an
         * untrusted boundary by construction. The posture copied here is
         * `AssistAllowance.fromStoredOrFull`'s: coerce into a sane range, never crash,
         * never refuse the write. Refusing would let a one-byte edit destroy an honest
         * player's recorded progress in exchange for denying a cheat a cosmetic badge.
         *
         * The clamp BOUNDS, it does not police: a large but in-range value (a forged
         * `9999` hints) is preserved verbatim, because only the *negative* case can
         * corrupt a downstream `> 0` predicate. A forged *low* count — claiming a
         * hint-free solve that was not — is the residual explicitly accepted by T-12-04:
         * the only real fix is a server, and this milestone has none.
         */
        fun coerced(
            hintsUsed: Int,
            undosUsed: Int,
            lastPegOnTarget: Boolean,
        ): SolveTelemetry =
            SolveTelemetry(
                hintsUsed = hintsUsed.coerceAtLeast(0),
                undosUsed = undosUsed.coerceAtLeast(0),
                lastPegOnTarget = lastPegOnTarget,
            )
    }
}
