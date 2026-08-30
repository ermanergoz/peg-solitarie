package com.app.pegsolitaire.platform.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * The auto-grant [AdGateway] stub: every rewarded request is granted for free
 * ([requestRewarded] returns [RewardOutcome.Granted], unconditionally and for every
 * [AdReason]), [preloadRewarded] is a no-op because there is nothing to warm, and
 * [showInterstitial] is a no-op (D-11). Mirrors
 * [com.app.pegsolitaire.data.DefaultLevelRepository]'s `@Inject constructor` impl shape.
 *
 * **The grant was "deliberately not configurable" for the duration of Phase 6 only
 * (D-02).** That constraint was scoped to a phase whose contract was "nothing a player can
 * see changes", so a stub that could decline would have changed debug behaviour inside it.
 * Phase 9 is where that changes: the ROADMAP requires the ADS-01 opt-in dialog to be built
 * and tested against this stub *before* any real ad SDK exists, and the stub therefore has
 * to be able to drive the dialog. It still never declines on its own — declining is the
 * player's choice inside the dialog, and the test fakes, not this stub, cover the
 * [RewardOutcome.Declined] and [RewardOutcome.Unavailable] branches.
 *
 * [rewardedOfferable] is `true`, and that is the load-bearing choice here. The naive
 * reading of "the NoOp gateway has no ads" gives `false`, which makes D-12 suppress the
 * opt-in dialog entirely — so the dialog would never render, the stub-first ordering would
 * prove nothing, and the AdMob opt-in policy gap would survive to the SDK swap. With
 * `true` the stub models "ads are offerable and instantly succeed": the dialog shows,
 * accepting resolves [RewardOutcome.Granted], and declining exercises the ViewModel's own
 * `Declined` path.
 *
 * Unlike [com.app.pegsolitaire.platform.games.NoOpGamesService], which deliberately
 * *declines* because reporting a connected player would let a leaderboard surface render
 * as though the player were ranked, this stub fails open: a missing ad would otherwise
 * cost the player an assist (ADS-03), while a missing leaderboard costs them nothing.
 */
class NoOpAdGateway
    @Inject
    constructor() : AdGateway {
        private val offerable = MutableStateFlow(true)

        override val rewardedOfferable: StateFlow<Boolean> = offerable.asStateFlow()

        override fun preloadRewarded(reason: AdReason) = Unit

        override suspend fun requestRewarded(reason: AdReason): RewardOutcome = RewardOutcome.Granted

        override fun showInterstitial() = Unit
    }
