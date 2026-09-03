package com.app.pegsolitaire.platform.ads

/**
 * Ad timing tokens: the bounded waits and the retry cadence the ad seam's implementations
 * must observe.
 *
 * Every value here is a unitless `Long` count of milliseconds — bounded waits, the retry
 * cadence and the pre-presentation pause, and nothing else. **How often an ad may be
 * shown is not timing and does not belong here**: the interstitial cadence policy
 * (D-01..D-04) lives in `:domain` as pure, testable Kotlin, and writing one of its values
 * into this object would put half a policy in the platform layer.
 * **No ad-unit identifier ever enters this object either**: IDs differ per build type and are
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

    /**
     * The age past which a loaded-but-unshown *interstitial* is discarded and reloaded
     * rather than shown.
     *
     * **The underlying expiry figure is assumed, not documented**, exactly as it is for
     * [REWARDED_AD_MAX_AGE_MS]. Google's *legacy* Google Mobile Ads documentation states a
     * loaded fullscreen ad expires roughly one hour after load; that figure was not
     * restated on any GMA Next-Gen page consulted for this phase, so it is carried here as
     * `[ASSUMED]`. This value is therefore a **defensive bound set under the assumed
     * hour**, not an encoding of a guarantee — treat it as a heuristic that keeps a long
     * peg-solitaire session from parking a certainly-stale ad, and do not cite it as
     * documented SDK behaviour.
     *
     * Deliberately the same number as [REWARDED_AD_MAX_AGE_MS] rather than a reference to
     * it: the two ad kinds share an assumption today, but they are separate inventory with
     * separate lifetimes, and one must be free to move without dragging the other with it.
     *
     * Correctness does not rest on it being right. A stale interstitial that slips through
     * fails at `show()`, arrives as `onAdFailedToShowFullScreenContent`, and is treated as
     * a **silent skip that navigates immediately** (D-11) — the player's continue tap is
     * honoured either way, with no message they did not ask for. This bound only makes
     * that outcome rarer.
     */
    const val INTERSTITIAL_AD_MAX_AGE_MS = 2_700_000L

    /**
     * How long the Results screen sits dimmed under the scrim before the interstitial
     * actually presents (D-08, INT-04).
     *
     * This is **ad timing** — the pause that is part of presenting an ad — which is this
     * object's remit. It is emphatically **not** cadence: how *often* an interstitial may
     * show (the D-01..D-04 level gate, cooldown, first-session suppression and session
     * cap) is policy, lives in `:domain`'s `InterstitialCadenceParams`, and may never be
     * written into this object. See the class KDoc.
     *
     * The value is chosen to exceed Android's 300 ms `ViewConfiguration` double-tap
     * timeout: a player who taps "Next level" twice out of habit has their stray second
     * tap land on the scrim rather than on a fullscreen ad that appeared underneath their
     * finger (INT-04). Longer than the double-tap window, short enough that the dim reads
     * as "something is coming" rather than as a stall.
     */
    const val INTERSTITIAL_QUIESCENCE_MS = 500L
}
