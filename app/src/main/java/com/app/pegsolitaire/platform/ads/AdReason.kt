package com.app.pegsolitaire.platform.ads

/**
 * Why a rewarded grant is being requested through [AdGateway.requestRewarded]. Only
 * the two Slice-1 assist affordances exist here; `RETRY` is reserved for Slice-2
 * between-levels interstitials (do not add it now — it flows through
 * [AdGateway.showInterstitial], not `requestRewarded`).
 */
enum class AdReason {
    /** A "reveal the next winning move" hint (ASST-01, plan 04-03). */
    HINT,

    /** An "undo the last move" rescue (ASST-02, this plan). */
    UNDO,
}
