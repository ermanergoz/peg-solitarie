package com.app.pegsolitaire.platform.ads

/**
 * The platform seam through which gameplay requests rewarded grants and shows
 * interstitials (spec §8.5, D-11). In Slice 1 this is fulfilled by an auto-grant
 * [NoOpAdGateway] stub so hint/undo are free; Slice 2 swaps only the Hilt binding
 * (`PlatformModule`) for a real AdMob-backed implementation — no gameplay or UI
 * change. Modelled on the [com.app.pegsolitaire.data.LevelRepository] interface +
 * `@Inject`-constructor-impl + `@Binds` pattern.
 */
interface AdGateway {
    /**
     * Requests a rewarded grant for [reason], returning whether the reward is granted.
     * The Slice-1 stub always returns `true` (free). It is `suspend` because the
     * Slice-2 implementation awaits a real rewarded-ad round trip; callers must treat
     * a `false` return as "not granted" and skip the assisted action.
     */
    suspend fun requestRewarded(reason: AdReason): Boolean

    /**
     * Shows an interstitial (the Slice-2 between-levels hook). A no-op in Slice 1;
     * the real implementation is introduced with the binding swap in Slice 2.
     */
    fun showInterstitial()
}
