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
     *
     * The clauses are ordered widest-suppression-first: the two that can hold for a whole install
     * or a whole session are asked before the two that change move by move, so the common "no" is
     * also the cheapest one and reads first at the call site.
     */
    fun mayShow(nowMs: Long): Boolean =
        newInstallSuppressionOver() &&
            underSessionCap() &&
            enoughWins() &&
            enoughElapsed(nowMs)

    /**
     * D-03: **both** halves of the new-install suppression must have passed — the first session
     * has ended AND the lifetime level threshold is met — so suppression lifts on whichever ends
     * later. A player who installs, plays six levels and never leaves the app still sees none.
     */
    private fun newInstallSuppressionOver(): Boolean =
        firstSessionComplete &&
            lifetimeCompletedLevels >= InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS

    /** D-04: the per-session cap, which is the only gate a long single sitting can exhaust. */
    private fun underSessionCap(): Boolean = interstitialsThisSession < InterstitialCadenceParams.MAX_PER_SESSION

    /** D-01: enough wins have accrued since the last interstitial. */
    private fun enoughWins(): Boolean =
        winsSinceLastInterstitial >= InterstitialCadenceParams.WINS_BETWEEN_INTERSTITIALS

    /**
     * D-02 (as widened by D-15): enough wall-clock time has passed since the last **fullscreen**
     * ad of either kind.
     *
     * [NEVER_SHOWN] is not a stamp to subtract from — a player who has seen no fullscreen ad has
     * trivially waited long enough — so it opens the gate before any arithmetic happens.
     *
     * A NEGATIVE delta also opens this gate, and the asymmetry with [isNewSession] (where a
     * negative delta CLOSES the boundary) is deliberate rather than an inconsistency. The clock
     * belongs to the user: an NTP correction can move it backwards mid-session, and a forged
     * future stamp is exactly what an ad-suppression exploit would write. Opening here means the
     * worst a moved clock can do is show one ad early; closing there means the worst it can do is
     * leave the D-04 cap binding. Both readings resolve the same rule from opposite sides — no
     * gate is ever permanently stuck open for the attacker or permanently stuck shut for the
     * honest player (T-10-07).
     */
    private fun enoughElapsed(nowMs: Long): Boolean =
        if (lastFullscreenAdAtMs == NEVER_SHOWN) {
            true
        } else {
            val elapsed = nowMs - lastFullscreenAdAtMs
            elapsed < 0L || elapsed >= InterstitialCadenceParams.MIN_GAP_MS
        }

    /**
     * The state after the player completes a level — in EITHER mode (D-07: one shared pair of
     * counters, so mode-switching cannot dodge the cap).
     *
     * Both counters saturate at their own threshold, and that is the overflow control rather than
     * tidiness. Each one is consumed only by a `>=` comparison against the very threshold it is
     * capped at, so a value beyond it carries no information that any caller could read — and
     * capping there makes `Int` overflow unreachable by construction, however many wins a
     * long-lived install accumulates or a tampered store claims (T-10-05 / ASVS V5).
     */
    fun recordWin(): InterstitialCadenceState =
        copy(
            winsSinceLastInterstitial =
                (winsSinceLastInterstitial + 1)
                    .coerceAtMost(InterstitialCadenceParams.WINS_BETWEEN_INTERSTITIALS),
            lifetimeCompletedLevels =
                (lifetimeCompletedLevels + 1)
                    .coerceAtMost(InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS),
        )

    /**
     * The state after an interstitial **actually presented** at [nowMs]: the win counter restarts,
     * the shared fullscreen stamp moves, and the session count advances (saturating at the cap).
     *
     * D-11 is the whole reason this is a separate call from the decision: counters advance only
     * when an ad really showed. A no-fill at the trigger moment leaves them running, so the very
     * next eligible win can show an ad as soon as one loads — a missing ad costs the player
     * nothing and costs the cadence nothing.
     *
     * [lifetimeCompletedLevels] and [firstSessionComplete] are untouched: they describe the
     * install's history, not this ad.
     */
    fun recordInterstitialShown(nowMs: Long): InterstitialCadenceState =
        copy(
            winsSinceLastInterstitial = 0,
            lastFullscreenAdAtMs = nowMs,
            interstitialsThisSession =
                (interstitialsThisSession + 1)
                    .coerceAtMost(InterstitialCadenceParams.MAX_PER_SESSION),
        )

    /**
     * The state after a REWARDED ad presented at [nowMs] — the shared fullscreen stamp moves and
     * nothing else does (D-15).
     *
     * [lastFullscreenAdAtMs] means "the last fullscreen ad", not "the last interstitial", and that
     * one word is the entire mechanism: a rewarded ad opens the same [InterstitialCadenceParams
     * .MIN_GAP_MS] cooldown, so an interstitial can never immediately follow one. That closes the
     * back-to-back fullscreen pairing AdMob policy prohibits, at no cost to D-02's locked value.
     *
     * A rewarded ad must NOT touch [winsSinceLastInterstitial] or [interstitialsThisSession]: it
     * is not an interstitial, the player opted into it, and charging it against the interstitial
     * budget would penalise them for accepting an offer they were asked to accept.
     */
    fun recordRewardedShown(nowMs: Long): InterstitialCadenceState = copy(lastFullscreenAdAtMs = nowMs)

    /**
     * The state at the start of a new session (D-04): the per-session count restarts, and the
     * install's first session is — by definition of having started a second one — over (D-03).
     */
    fun startNewSession(): InterstitialCadenceState =
        copy(
            interstitialsThisSession = 0,
            firstSessionComplete = true,
        )

    companion object {
        /** [lastFullscreenAdAtMs] when no fullscreen ad has ever been shown on this install. */
        const val NEVER_SHOWN = -1L

        /** The last-foreground stamp when the app has never been foregrounded (first launch). */
        const val NEVER_FOREGROUNDED = -1L

        /**
         * Whether arriving at [nowMs] after last being in the foreground at [lastForegroundAtMs]
         * opens a NEW session (D-04, D-16) — i.e. whether the app has been backgrounded for the
         * whole [InterstitialCadenceParams.SESSION_GAP_MS] window.
         *
         * The [NEVER_FOREGROUNDED] guard is load-bearing, not defensive politeness. Without it the
         * very first launch would compare against a sentinel, report a new session, immediately set
         * `firstSessionComplete = true` through [startNewSession], and delete D-03's entire
         * first-session suppression on install day — the one window the phase most needs to hold.
         * A first-ever launch closes no prior session because there was none.
         *
         * A backwards or forged-future [lastForegroundAtMs] yields a negative delta, which fails
         * the `>=` and keeps the session — and therefore the D-04 cap — intact. See
         * [enoughElapsed] for why that direction is the opposite of the wall-clock gate's and why
         * both are the same rule. The subtraction cannot overflow: [fromStored] admits only
         * [NEVER_FOREGROUNDED] or a non-negative value below `Long.MAX_VALUE` into a stamp.
         */
        fun isNewSession(
            lastForegroundAtMs: Long,
            nowMs: Long,
        ): Boolean =
            lastForegroundAtMs != NEVER_FOREGROUNDED &&
                (nowMs - lastForegroundAtMs) >= InterstitialCadenceParams.SESSION_GAP_MS

        /**
         * The cadence state a persisted record denotes (D-13), validated on read and failing
         * toward **fewer ads** (T-10-01 / ASVS V5).
         *
         * This is the deliberate INVERSE of `AssistAllowance.fromStoredOrFull`, which fails open
         * to a FULL allowance because the cost of getting it wrong there is an honest player
         * losing hints they had earned. Here the cost of getting it wrong is a player seeing an ad
         * the policy had not authorised, so every ambiguous input resolves toward silence: a
         * missing [firstSessionComplete] reads `false`, which alone makes [mayShow] false at every
         * `nowMs`. A corrupt store costs revenue, never goodwill.
         *
         * Out-of-range counters are **clamped** rather than rejected upward — a forged `9999` can
         * only ever buy what an honest player's counter could — and negatives read as `0`.
         * A stamp is trusted only when it is non-negative and strictly below `Long.MAX_VALUE`;
         * everything else, including `null` and a forged `Long.MAX_VALUE`, becomes [NEVER_SHOWN]
         * (T-10-08). That bound is what makes `nowMs - lastFullscreenAdAtMs` representable for
         * every non-negative `nowMs`, so the D-02 delta can never overflow.
         *
         * [interstitialsThisSession] is deliberately NOT a parameter: D-13 keeps it in memory,
         * because it derives from a session boundary any relaunch has already crossed. The caller
         * supplies it from its own process-lifetime counter.
         */
        fun fromStored(
            winsSinceLastInterstitial: Int?,
            lastFullscreenAdAtMs: Long?,
            lifetimeCompletedLevels: Int?,
            firstSessionComplete: Boolean?,
        ): InterstitialCadenceState =
            InterstitialCadenceState(
                winsSinceLastInterstitial =
                    winsSinceLastInterstitial
                        .orZeroClampedTo(InterstitialCadenceParams.WINS_BETWEEN_INTERSTITIALS),
                lastFullscreenAdAtMs = lastFullscreenAdAtMs.orNeverShown(),
                lifetimeCompletedLevels =
                    lifetimeCompletedLevels
                        .orZeroClampedTo(InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS),
                firstSessionComplete = firstSessionComplete ?: false,
            )

        /** This stored counter pulled into `0..threshold`; absent or negative reads as none. */
        private fun Int?.orZeroClampedTo(threshold: Int): Int = (this ?: 0).coerceIn(0, threshold)

        /** This stored stamp if it is one a real presentation could have written, else the sentinel. */
        private fun Long?.orNeverShown(): Long =
            if (this != null && this >= 0L && this != Long.MAX_VALUE) this else NEVER_SHOWN
    }
}
