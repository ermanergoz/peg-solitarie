package com.app.pegsolitaire.platform.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * The auto-grant [AdGateway] stub: every rewarded request is granted for free
 * ([requestRewarded] returns [RewardOutcome.Granted], unconditionally and for every
 * [AdReason]), [preloadRewarded] and [preloadInterstitial] are no-ops because there is
 * nothing to warm, and [showInterstitial] never presents anything and says so. Mirrors
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
 * **[interstitialReady] is `false`, and the asymmetry against that `true` is deliberate,
 * not an oversight left to be tidied away.** The two answers come from the same principle
 * applied to two different ads: this stub's contract is that the player is never worse off
 * than v1.0. v1.0 had a hint/undo economy the ADS-01 dialog belongs to, so the dialog must
 * stay reachable — hence `rewardedOfferable = true`. v1.0 had no interstitials at all, so
 * there is nothing to keep reachable — and a stub reporting one ready would put an
 * unrequested dim, a 500 ms pause and a scrim (D-08) in front of a player who can never
 * actually be shown an ad. That is strictly worse than v1.0 for zero revenue. `false`
 * means a stub build behaves exactly like a no-fill: instant navigation, no pause (D-10,
 * D-11).
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

        private val interstitialReadyState = MutableStateFlow(false)

        override val interstitialReady: StateFlow<Boolean> = interstitialReadyState.asStateFlow()

        override fun preloadInterstitial() = Unit

        override suspend fun showInterstitial(): Boolean = false
    }
