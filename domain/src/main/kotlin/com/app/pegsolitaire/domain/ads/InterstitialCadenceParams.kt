package com.app.pegsolitaire.domain.ads

/**
 * Scoped tunable constants for the between-level interstitial CADENCE (INT-02).
 *
 * Split by scope per QUAL-03's no-magic-number rule, in the same shape as
 * `AssistAllowanceLimits` and [com.app.pegsolitaire.domain.scoring.ScoreParams]: every value is a
 * KDoc'd `const val` naming the decision that fixes it, and every gate in
 * [InterstitialCadenceState] reads its threshold from here rather than restating it. A second
 * statement of any of these numbers — in the policy, in a test, in a store — is a bug waiting for
 * the two copies to drift apart.
 *
 * ## Remit: cadence, and only cadence
 *
 * This object owns *how often* an interstitial may appear. It deliberately does **not** live in
 * `app`'s `platform/ads/AdConstants`, whose own KDoc limits that object to ad **timing** — how
 * long a loaded ad stays fresh, how long a caller may wait for one. Timing is a property of the
 * SDK handle and belongs beside it; cadence is a property of the player's experience, it is
 * decided before any SDK is asked anything, and it is the half that must stay JVM-testable in pure
 * `:domain`. Two objects, two remits: merging them would drag SDK-shaped constants into the module
 * that is forbidden to name them.
 */
object InterstitialCadenceParams {
    /**
     * Wins that must accrue since the last interstitial before another one is eligible (D-01).
     *
     * The level gate on its own is not enough — a run of speed-cleared easy levels would satisfy
     * it in under a minute, which is why [MIN_GAP_MS] must hold at the same time.
     */
    const val WINS_BETWEEN_INTERSTITIALS = 3

    /**
     * Minimum wall-clock gap before another interstitial is eligible (D-02).
     *
     * D-15 widened what the gap is measured FROM without touching the value: the stamp it is
     * compared against is the last **fullscreen** ad, not the last interstitial, so a rewarded ad
     * opens this cooldown too and can never be immediately followed by an interstitial.
     */
    const val MIN_GAP_MS = 90_000L

    /**
     * Lifetime completed levels that stay interstitial-free on a new install (D-03).
     *
     * Half of a two-part suppression: the first session must ALSO have ended, whichever comes
     * later. A player who installs, plays six levels and stays in the app sees no interstitial.
     */
    const val SUPPRESSED_FIRST_LEVELS = 5

    /** Maximum interstitials shown in a single session (D-04). */
    const val MAX_PER_SESSION = 4

    /** Background duration that ends a session (D-04) — the standard analytics convention. */
    const val SESSION_GAP_MINUTES = 30L

    /** Milliseconds in one minute, so [SESSION_GAP_MS] can be derived rather than restated. */
    const val MILLIS_PER_MINUTE = 60_000L

    /**
     * The session boundary in milliseconds — the unit [InterstitialCadenceState.isNewSession]
     * actually compares in.
     *
     * DERIVED from [SESSION_GAP_MINUTES] and [MILLIS_PER_MINUTE] rather than written as a
     * standalone `1_800_000L`, per `ScoreParams.TWO_STAR_MAX_PEGS`' discipline: the stated 30
     * minutes and the compared milliseconds cannot drift apart, because moving the minutes moves
     * this value with them.
     */
    const val SESSION_GAP_MS = SESSION_GAP_MINUTES * MILLIS_PER_MINUTE
}
