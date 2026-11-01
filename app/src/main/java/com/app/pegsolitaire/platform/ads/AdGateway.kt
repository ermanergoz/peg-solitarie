package com.app.pegsolitaire.platform.ads

import kotlinx.coroutines.flow.StateFlow

/**
 * The platform seam through which gameplay requests rewarded grants and shows
 * interstitials (spec §8.5, D-11). It is fulfilled in production by [AdMobAdGateway]
 * (bound in `PlatformModule` since Phase 9) and by the auto-grant [NoOpAdGateway] stub
 * wherever no ad SDK should run. Modelled on the
 * [com.app.pegsolitaire.data.LevelRepository] interface + `@Inject`-constructor-impl
 * + `@Binds` pattern.
 *
 * **Two ad kinds, six members, one shared rule.** The rewarded triple is *offered* — the
 * player is always asked first — while the interstitial triple is not, so its readiness
 * signal is what decides whether a player is interrupted at all. The rule both obey: no
 * Google Mobile Ads type may appear in any signature here. Readiness is a `Boolean`, the
 * rewarded answer is a sealed [RewardOutcome], and the interstitial answer is a `Boolean`,
 * precisely so that this file stays SDK-free — see
 * [com.app.pegsolitaire.platform.ads.di.AdsModule] for what happens to the Hilt graph
 * otherwise.
 */
interface AdGateway {
    /**
     * Whether a rewarded offer is worth putting in front of the player at all (D-12).
     *
     * `false` means ads are **knowably** unservable — consent was refused or the consent
     * flow failed (CONS-04), the ads SDK was never initialized, or readiness is still
     * [com.app.pegsolitaire.platform.ads.consent.AdsReadiness.Unknown] because bootstrap
     * has not answered yet. On `false` the caller MUST **skip the opt-in dialog entirely
     * and grant the assist for free, silently**, exactly like v1.0 [NoOpAdGateway]
     * behaviour: no dialog, no snackbar, no explanation the player did not ask for.
     * Offering "watch an ad?" and then instantly granting the assist for free because
     * nothing could be served is the absurdity D-12 exists to rule out.
     *
     * This is a separate signal rather than a fourth [RewardOutcome] member for two
     * reasons. A fourth member would break the sealed three-way contract that every
     * existing call site already handles exhaustively — [RewardOutcome] is sealed
     * precisely so those three branches stay pinned. And the decision this answers is
     * needed **before** the dialog is shown, not after a request comes back: by the time
     * a [RewardOutcome] exists the player has already been asked.
     *
     * Observable rather than a plain `Boolean` getter because consent resolves
     * asynchronously after launch, so a surface must react to readiness arriving rather
     * than hold a stale answer captured at composition time.
     */
    val rewardedOfferable: StateFlow<Boolean>

    /**
     * Asks the implementation to start warming a rewarded ad for [reason].
     *
     * Non-suspending and fire-and-forget by design: the caller neither waits for nor
     * learns the result. Called at the moment a free assist counter reaches 0 (D-09) —
     * the earliest point at which an offer can occur. No ad is requested while allowance
     * remains, so most levels never preload at all.
     *
     * A no-op is a valid implementation. [NoOpAdGateway] implements it as one, and a real
     * implementation that already holds a loaded ad should also do nothing.
     */
    fun preloadRewarded(reason: AdReason)

    /**
     * Requests a rewarded grant for [reason], answering with a three-way
     * [RewardOutcome]. It is `suspend` because a real implementation awaits a rewarded-ad
     * round trip. The caller contract is not symmetric: [RewardOutcome.Granted] and
     * [RewardOutcome.Unavailable] both **proceed** with the assisted action — ADS-03
     * grants an assist that could not be paid for — and only [RewardOutcome.Declined]
     * skips it. The current stub answers [RewardOutcome.Granted] unconditionally.
     */
    suspend fun requestRewarded(reason: AdReason): RewardOutcome

    /**
     * Whether an interstitial is loaded and showable **right now** (INT-01, D-10).
     *
     * Observable rather than a plain `Boolean` getter for the reason
     * [rewardedOfferable] is: consent resolves asynchronously after launch, and a load
     * lands later still, so a surface must react to readiness arriving rather than hold a
     * stale answer captured at composition time. Unlike [rewardedOfferable] this signal
     * additionally requires that an ad is actually *held* — for the rewarded path the
     * player has already opted in and can be shown a spinner, whereas here readiness
     * decides whether the player is interrupted at all.
     *
     * The caller contract is precise and asymmetric. `false` means the continue tap
     * **navigates instantly, with zero added latency and no scrim** (D-10): the quiescence
     * pause is part of *presenting an ad*, never part of navigation, so a player who could
     * not be served one is a player who was never delayed. `true` means an interstitial
     * may be presented — subject to the cadence policy, which this seam knows nothing
     * about and must not be asked about.
     *
     * `true` is a predicate, not a guarantee: a held ad can still fail at `show()`, which
     * is indistinguishable from a no-fill and lands on the same silent-skip path (D-11).
     */
    val interstitialReady: StateFlow<Boolean>

    /**
     * Asks the implementation to start warming an interstitial.
     *
     * Non-suspending, fire-and-forget and **idempotent**: the caller neither waits for nor
     * learns the result, and calling it with an ad already held does nothing. A no-op is a
     * valid implementation — [NoOpAdGateway] implements it as one.
     *
     * D-12 fixes *when* it is called: at the moment a win makes the **next** continue tap
     * ad-eligible, and not before. That is the interstitial reading of the rule
     * [preloadRewarded] already follows — no ad request is issued while first-session
     * suppression, the session cap, or the level and time gates make an ad impossible, so
     * a player who will not be shown an ad never has one requested on their behalf.
     */
    fun preloadInterstitial()

    /**
     * Shows the held interstitial, suspending until it is dismissed or fails to show, and
     * answering **`true` only if an ad was actually presented**.
     *
     * `suspend` mirrors [requestRewarded] and is what makes D-09 expressible: the ad
     * presents over the dimmed Results screen and navigation proceeds on dismissal, so the
     * caller needs a resumption point rather than a callback. A `Boolean` return — rather
     * than an SDK handle or an outcome type — is what keeps this seam free of any Google
     * Mobile Ads type.
     *
     * **The caller contract is deliberately asymmetric, and it is what makes D-09 and D-11
     * work.** Advance the cadence counters **only** on `true`: an ad that never presented
     * must not consume the player's quota, so the very next eligible win can show one
     * (D-11 — the skip is silent and the counters keep running). Navigate
     * **unconditionally**, on either answer: the tap's intent is honoured exactly once
     * whether or not an ad appeared, and a failed presentation must never dead-end the
     * player on the Results screen.
     */
    suspend fun showInterstitial(): Boolean
}
