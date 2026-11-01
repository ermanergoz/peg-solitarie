package com.app.pegsolitaire.platform.ads

/**
 * Why a rewarded grant is being requested through [AdGateway.requestRewarded]. The two
 * assist affordances below are the complete set: retrying a level stays free
 * permanently, so no `RETRY` member will ever be added (D-03).
 */
enum class AdReason {
    /** A "reveal the next winning move" hint (ASST-01, plan 04-03). */
    HINT,

    /** An "undo the last move" rescue (ASST-02, this plan). */
    UNDO,
}
