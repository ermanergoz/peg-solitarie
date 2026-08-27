package com.app.pegsolitaire.platform.ads

/**
 * The result of a rewarded-grant request through [AdGateway.requestRewarded] (D-01).
 *
 * - [Granted]: the reward is earned — proceed with the assist.
 * - [Declined]: the player was offered an ad and said no — skip the assist.
 * - [Unavailable]: no ad could be served — proceed with the assist anyway (ADS-03).
 *
 * Sealed rather than `Boolean` so the compiler forces every call site to decide what
 * happens when no ad could be served: a `Boolean` cannot express the difference between
 * "the player said no" and "we had nothing to show them", yet ADS-03 requires those two
 * to behave in opposite ways. The per-request motive lives next door in [AdReason].
 */
sealed interface RewardOutcome {
    /** The reward is granted: an ad reported the reward earned, or none was required. */
    data object Granted : RewardOutcome

    /** The player was offered an ad and explicitly declined, or dismissed it unearned. */
    data object Declined : RewardOutcome

    /**
     * No ad could be served (no consent yet, no fill, load error, no foreground
     * Activity). Callers MUST proceed with the assist anyway (ADS-03) — the player
     * is never dead-ended by an ad that failed to arrive.
     */
    data object Unavailable : RewardOutcome
}
