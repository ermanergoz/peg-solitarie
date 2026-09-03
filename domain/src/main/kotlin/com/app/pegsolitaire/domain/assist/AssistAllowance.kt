package com.app.pegsolitaire.domain.assist

/**
 * Which assist a player is asking for (ADS-02).
 *
 * ## Why this is not `AdReason`
 *
 * `AdReason` (`app`'s `platform/ads/`) names the same two things from the *ads* side, and
 * the duplication is deliberate rather than an oversight to be tidied away. `:domain` is
 * pure Kotlin with zero dependencies on app code — `DomainPurityTest` enforces exactly
 * that — so reaching for `AdReason` here would invert the module graph and take the free-
 * allowance policy out of JVM unit-test reach. `GameViewModel` owns the mapping between
 * the two enums; it is the only place that knows both.
 */
enum class AssistKind {
    /** A solver-backed hint. */
    HINT,

    /** One step of undo. */
    UNDO,
}

/**
 * Scoped tunable constants for the per-level free-assist allowance (ADS-02).
 *
 * Split by scope per QUAL-03's no-magic-number rule, in the same shape as `ScoreParams`:
 * each value is a KDoc'd `const val` naming the decision that fixes it. **These two
 * numbers are written exactly once in the whole codebase** (D-02) — the Phase-9 dialog
 * copy formats them through a `%1$d` placeholder rather than typing them into
 * `strings.xml`, and tests assert against these constants rather than against bare
 * literals. A second statement of either number anywhere is a bug waiting for the two
 * copies to drift apart.
 */
object AssistAllowanceLimits {
    /** Free hints granted per level before an ad is offered (D-02). */
    const val FREE_HINTS = 3

    /**
     * Free undos granted per level before an ad is offered (D-02). Higher than
     * [FREE_HINTS] because undo is a correction of the player's own mistake rather than
     * a shortcut past the puzzle, so gating it early would read as a punishment.
     */
    const val FREE_UNDOS = 5
}

/**
 * How many free hints and undos the current level still owes the player (ADS-02).
 *
 * A pure immutable value type: every mutator returns a new allowance, so the counters can
 * live inside a `UiState` without a shared-mutable-state hazard, and the whole policy is
 * JVM-unit-testable with no Android runtime (D-01, QUAL-01).
 *
 * Hints and undos are **separate counters**, not one shared pool (D-01): exhausting your
 * hints must not cost you the ability to take back a misplaced jump.
 *
 * @property hintsRemaining Free hints left this level, always in `0..FREE_HINTS`.
 * @property undosRemaining Free undos left this level, always in `0..FREE_UNDOS`.
 */
data class AssistAllowance(
    val hintsRemaining: Int = AssistAllowanceLimits.FREE_HINTS,
    val undosRemaining: Int = AssistAllowanceLimits.FREE_UNDOS,
) {
    /** Free uses of [kind] still available. */
    fun remaining(kind: AssistKind): Int =
        when (kind) {
            AssistKind.HINT -> hintsRemaining
            AssistKind.UNDO -> undosRemaining
        }

    /** Whether [kind] can be used for free right now — i.e. without offering an ad. */
    fun hasFree(kind: AssistKind): Boolean = remaining(kind) > 0

    /**
     * The allowance after consuming one free use of [kind]. The other kind is untouched
     * (D-01).
     *
     * The counter is the product of a call sequence rather than a range-checked type, so
     * the [coerceAtLeast] floor is a correctness requirement, not defensive noise
     * (T-09-03 / ASVS V5): it makes `spend` total for every reachable state, including the
     * exhausted one. Without it a caller that spent past zero would drive the counter
     * negative, and a later `remaining(kind) > 0` would keep reading false while the
     * *stored* value silently became a forged-looking negative that
     * [fromStoredOrFull] would then have to reject on the next resume.
     */
    fun spend(kind: AssistKind): AssistAllowance =
        when (kind) {
            AssistKind.HINT -> copy(hintsRemaining = (hintsRemaining - 1).coerceAtLeast(0))
            AssistKind.UNDO -> copy(undosRemaining = (undosRemaining - 1).coerceAtLeast(0))
        }

    /**
     * The allowance after a watched ad restores [kind] to its **full** limit (D-03) —
     * not `+1`. One ad buys a whole new allowance of that assist; the other kind is
     * untouched. `refill(kind).spend(kind)` is the Granted-path ordering `GameViewModel`
     * uses, leaving `limit - 1` for that kind.
     */
    fun refill(kind: AssistKind): AssistAllowance =
        when (kind) {
            AssistKind.HINT -> copy(hintsRemaining = AssistAllowanceLimits.FREE_HINTS)
            AssistKind.UNDO -> copy(undosRemaining = AssistAllowanceLimits.FREE_UNDOS)
        }

    companion object {
        /**
         * The allowance a stored resume snapshot denotes (D-04 / D-17), resolving each
         * counter independently and **failing open to the full limit**.
         *
         * `null` means "this blob was written before the allowance existed", NOT "zero
         * remaining" — an old saved game resumes with a fresh allowance rather than a
         * locked-out one.
         *
         * A present value is accepted only when it is `in 0..limit`, and anything else is
         * **rejected in favour of the default** rather than clamped (T-09-01 / ASVS V5,
         * the same discipline WARN-02 applied to stored coordinates). The distinction
         * matters: clamping a forged `9999` to the limit and rejecting it happen to
         * coincide here, but clamping *states* that the stored number is meaningful, and
         * the next tampered field where the two differ would inherit that assumption.
         * `0` is in range and is therefore preserved — a genuinely exhausted allowance
         * must survive process death as exhausted, or the persistence would be a free
         * refill for anyone willing to background the app.
         *
         * Failing **open** rather than refusing the snapshot is the deliberate asymmetry
         * with `SnapshotMapper.resolveMinimumPegsOrNull`, whose out-of-range path kills
         * the whole resume: a missing target is a fact the game needs to score correctly,
         * whereas a bad allowance is at worst a few free hints. Refusing to restore over
         * a forged counter would convert a tamper attempt — or an unrelated corruption
         * bug — into progress loss for an honest player (T-09-02).
         */
        fun fromStoredOrFull(
            hints: Int?,
            undos: Int?,
        ): AssistAllowance =
            AssistAllowance(
                hintsRemaining = hints.orFullIfOutOfRange(AssistAllowanceLimits.FREE_HINTS),
                undosRemaining = undos.orFullIfOutOfRange(AssistAllowanceLimits.FREE_UNDOS),
            )

        /** This stored counter if it is a value [limit] could have produced, else [limit]. */
        private fun Int?.orFullIfOutOfRange(limit: Int): Int = if (this != null && this in 0..limit) this else limit
    }
}
