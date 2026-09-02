package com.app.pegsolitaire.domain.ads

/**
 * Everything the "may an interstitial show right now?" question needs, as plain data (INT-02).
 *
 * A pure immutable value type in the shape of `AssistAllowance`: [mayShow] is a conjunction of four
 * named gates over these fields, and every mutator returns a new state, so the whole cadence policy
 * is a total function with no clock, no Android runtime and no I/O — exhaustively JVM-unit-testable
 * with zero scaffolding, which is exactly what the ROADMAP's Phase 10 note requires of it.
 *
 * ## One counter, both modes (D-07)
 *
 * There is a single state value and a single policy over it. No per-mode counter exists, so a
 * player alternating Classic and Challenge cannot dodge the cap: the player experiences one app,
 * and the cadence is one app's cadence.
 *
 * ## Why `nowMs` is a parameter
 *
 * `DomainPurityTest` fails the build on any `android..`/`androidx..` dependency from `..domain..`,
 * which mechanically forbids `SystemClock` here — and that gate is the point, not an obstacle. The
 * timebase must be wall-clock anyway: a monotonic uptime clock resets on reboot, and D-13 persists
 * these stamps across process death, so `elapsedRealtime` would let a reboot erase the cooldown.
 * The caller supplies `nowMs`, exactly as `ProgressRepository.recordResult` already does.
 *
 * ## Why this is not `AdReason` / `AdConstants`
 *
 * `app`'s `platform/ads/` names ad concepts from the *SDK* side, and the separation is deliberate
 * rather than an oversight to be tidied away. `:domain` is pure Kotlin with zero dependencies on
 * app code, so reaching into `platform/ads` from here would invert the module graph and take the
 * cadence policy out of JVM unit-test reach — the same trade `AssistAllowance` already documents
 * for `AdReason`. The gateway decides *how* an ad is loaded and shown; this class decides
 * *whether*, and it is the only thing in the phase that decides it.
 *
 * @property winsSinceLastInterstitial Wins banked since the last interstitial, saturating at
 *   [InterstitialCadenceParams.WINS_BETWEEN_INTERSTITIALS] (D-01, D-07).
 * @property lastFullscreenAdAtMs Wall-clock stamp of the last **fullscreen** ad — interstitial or
 *   rewarded (D-15) — or [NEVER_SHOWN] if the player has seen none.
 * @property lifetimeCompletedLevels Levels completed since install, saturating at
 *   [InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS] (D-03).
 * @property firstSessionComplete Whether the install's first session has ended (D-03).
 * @property interstitialsThisSession Interstitials shown in the current session (D-04). Held in
 *   memory only, per D-13 — it derives from a session boundary a relaunch has already crossed.
 */
data class InterstitialCadenceState(
    val winsSinceLastInterstitial: Int = 0,
    val lastFullscreenAdAtMs: Long = NEVER_SHOWN,
    val lifetimeCompletedLevels: Int = 0,
    val firstSessionComplete: Boolean = false,
    val interstitialsThisSession: Int = 0,
) {
    /**
     * Whether an interstitial may show at [nowMs]. ALL four gates must pass — D-01 AND D-02 AND
     * D-03 AND D-04 — and each clause is a separately named predicate so a failing test names the
     * gate it broke rather than "the cadence".
     */
    fun mayShow(nowMs: Long): Boolean = TODO("10-01 task 2")

    /** The state after the player completes a level (D-07: one shared counter, both modes). */
    fun recordWin(): InterstitialCadenceState = TODO("10-01 task 2")

    /** The state after an interstitial actually presented at [nowMs] (D-11). */
    fun recordInterstitialShown(nowMs: Long): InterstitialCadenceState = TODO("10-01 task 2")

    /** The state after a REWARDED ad presented at [nowMs] (D-15). */
    fun recordRewardedShown(nowMs: Long): InterstitialCadenceState = TODO("10-01 task 2")

    /** The state at the start of a new session (D-04, D-03). */
    fun startNewSession(): InterstitialCadenceState = TODO("10-01 task 2")

    companion object {
        /** [lastFullscreenAdAtMs] when no fullscreen ad has ever been shown on this install. */
        const val NEVER_SHOWN = -1L

        /** The last-foreground stamp when the app has never been foregrounded (first launch). */
        const val NEVER_FOREGROUNDED = -1L

        /**
         * Whether arriving at [nowMs] after last being in the foreground at [lastForegroundAtMs]
         * opens a NEW session (D-04, D-16).
         */
        fun isNewSession(
            lastForegroundAtMs: Long,
            nowMs: Long,
        ): Boolean = TODO("10-01 task 2")

        /**
         * The cadence state a persisted record denotes (D-13), validated on read and failing
         * toward FEWER ads (ASVS V5).
         */
        fun fromStored(
            winsSinceLastInterstitial: Int?,
            lastFullscreenAdAtMs: Long?,
            lifetimeCompletedLevels: Int?,
            firstSessionComplete: Boolean?,
        ): InterstitialCadenceState = TODO("10-01 task 2")
    }
}
