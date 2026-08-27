package com.app.pegsolitaire.platform.ads

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
