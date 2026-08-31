package com.app.pegsolitaire.platform.ads

/**
 * Ad timing tokens: the bounded waits and the retry cadence the ad seam's implementations
 * must observe.
 *
 * Every value here is a unitless `Long` count of milliseconds — timeouts and cadence only.
 * **No ad-unit identifier ever enters this object**: IDs differ per build type and are
 * resolved at build time through [AdIds]/`BuildConfig`, which is the only sanctioned
 * route. Writing one here is exactly the violation the `adIdLiteralCheck` build gate
 * exists to stop.
 *
 * Scoped, not global: this object owns ad timing and only ad timing, as
 * [com.app.pegsolitaire.audio.SoundParams] does for audio.
 */
object AdConstants {
    /**
     * The bounded in-dialog wait after the player accepts an offer but before the ad is
     * ready (D-10).
     *
     * The opt-in dialog's button shows a loading state for at most this long, then falls
     * open: the assist is granted free with an explanation. Bounded so the control can
     * never spin forever — a single surface, no dead end.
     */
    const val ACCEPT_WAIT_MS = 5_000L

    /**
     * The cap callers MUST apply around
     * [com.app.pegsolitaire.platform.activity.ForegroundActivityProvider.awaitActivity].
     *
     * That interface's own KDoc makes the timeout the caller's responsibility: a
     * backgrounded app may never resume, and an ad or consent request that waits forever
     * leaves the assist control spinning with no way out. This is the one number every
     * such call site uses, so the bound is a documented decision rather than a literal
     * repeated per site.
     */
    const val ACTIVITY_WAIT_MS = 10_000L

    /**
     * How long a failed preload waits before trying again (D-09).
     *
     * A preload fires when a counter hits 0 and may fail on no fill or no network. Retry
     * is deliberately unhurried: the player has not asked for anything yet, and a tight
     * loop against a no-fill response burns battery and request quota for no benefit.
     *
     * Deliberately longer than [ACCEPT_WAIT_MS], which is what makes the retry a
     * *preload-only* affordance: a player who has already accepted an offer falls open to
     * a free assist long before a second attempt could land, and never waits on one.
     */
    const val PRELOAD_RETRY_DELAY_MS = 30_000L

    /**
     * The age past which a loaded-but-unshown rewarded ad is discarded and reloaded
     * rather than shown.
     *
     * **The underlying expiry figure is assumed, not documented.** Google's *legacy*
     * Google Mobile Ads documentation states a rewarded ad expires roughly one hour after
     * load; that figure was not restated on any GMA Next-Gen page consulted for this
     * phase, so it is carried here as `[ASSUMED]` (RESEARCH A1 / Pitfall 11). This value
     * is therefore a **defensive bound set under the assumed hour**, not an encoding of a
     * guarantee — treat it as a heuristic that keeps a long peg-solitaire session from
     * parking a certainly-stale ad, and do not cite it as documented SDK behaviour.
     *
     * Correctness does not rest on it being right. A stale ad that slips through fails at
     * `show()` and arrives as `onAdFailedToShowFullScreenContent`, which the gateway maps
     * to [com.app.pegsolitaire.platform.ads.RewardOutcome.Unavailable] — so the player
     * still gets the assist either way (ADS-03). This bound only makes that outcome rarer.
     */
    const val REWARDED_AD_MAX_AGE_MS = 2_700_000L
}
