package com.app.pegsolitaire.platform.ads

import javax.inject.Inject

/**
 * The auto-grant [AdGateway] stub: every rewarded request is granted for free
 * ([requestRewarded] returns [RewardOutcome.Granted], unconditionally and for every
 * [AdReason]) and [showInterstitial] is a no-op (D-11). The grant is deliberately not
 * configurable (D-02): this is the behavior that keeps hint and undo identical to v1.0
 * while the seam around it is rebuilt, and a stub that could decline would change debug
 * behavior inside a phase whose contract is "nothing a player can see changes". Mirrors
 * [com.app.pegsolitaire.data.DefaultLevelRepository]'s `@Inject constructor` impl shape.
 */
class NoOpAdGateway
    @Inject
    constructor() : AdGateway {
        override suspend fun requestRewarded(reason: AdReason): RewardOutcome = RewardOutcome.Granted

        override fun showInterstitial() = Unit
    }
