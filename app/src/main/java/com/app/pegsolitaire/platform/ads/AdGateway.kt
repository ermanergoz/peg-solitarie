package com.app.pegsolitaire.platform.ads

import kotlinx.coroutines.flow.StateFlow

/**
 * The platform seam through which gameplay requests rewarded grants and shows
 * interstitials (spec §8.5, D-11). Today it is fulfilled by an auto-grant
 * [NoOpAdGateway] stub so hint/undo are free. Swapping the Hilt binding
 * (`PlatformModule`) for a real ad-backed implementation is necessary but **not
 * sufficient**: every real SDK call needs a foreground Activity, which only
 * platform-layer code may hold, and Phase 9 additionally puts an opt-in dialog in
 * front of any ad that plays — so the swap does come with UI work. Modelled on the
 * [com.app.pegsolitaire.data.LevelRepository] interface + `@Inject`-constructor-impl
 * + `@Binds` pattern.
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
     * Shows an interstitial (the Slice-2 between-levels hook). A no-op in Slice 1;
     * the real implementation is introduced with the binding swap in Slice 2.
     */
    fun showInterstitial()
}
