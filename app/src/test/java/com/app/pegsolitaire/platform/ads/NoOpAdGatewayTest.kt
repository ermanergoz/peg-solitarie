package com.app.pegsolitaire.platform.ads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Proves the [NoOpAdGateway] stub answers [RewardOutcome.Granted] to every rewarded
 * reason (D-02) — never [RewardOutcome.Declined] and never [RewardOutcome.Unavailable] —
 * that it reports rewarded offers as available so the ADS-01 opt-in dialog is reachable
 * against the stub, and that its interstitial members answer the **opposite** polarity —
 * never ready, never presented — so no player on a stub build is ever paused for an ad
 * that cannot exist. Granting unconditionally is what keeps hint and undo free and
 * byte-identical to v1.0; refusing interstitials outright is what keeps the rest of the
 * experience byte-identical to v1.0 too.
 */
class NoOpAdGatewayTest {
    private val gateway = NoOpAdGateway()

    @Test
    fun requestRewardedGrantsHint() =
        runTest {
            assertThat(gateway.requestRewarded(AdReason.HINT)).isEqualTo(RewardOutcome.Granted)
        }

    @Test
    fun requestRewardedGrantsUndo() =
        runTest {
            assertThat(gateway.requestRewarded(AdReason.UNDO)).isEqualTo(RewardOutcome.Granted)
        }

    /**
     * Pins `rewardedOfferable == true` — RESEARCH.md **Pitfall 6**.
     *
     * The naive reading of "the NoOp gateway has no ads" is `false`, but `false` makes
     * D-12 suppress the ADS-01 opt-in dialog entirely. The dialog would then never render
     * against the stub, the ROADMAP-mandated "build and test the dialog before the real
     * SDK" ordering would prove nothing, and the AdMob opt-in policy gap would survive to
     * the SDK swap. If a future change "corrects" this to `false`, this test is the thing
     * that explains why it must not.
     */
    @Test
    fun rewardedOfferableIsTrueSoTheOptInDialogIsReachable() =
        runTest {
            assertThat(gateway.rewardedOfferable.value).isTrue()
        }

    /**
     * Pins `interstitialReady == false` — the deliberate **opposite** polarity to
     * [rewardedOfferableIsTrueSoTheOptInDialogIsReachable], and the two must not be
     * "harmonised".
     *
     * Both answers come from one principle: against this stub the player is never worse
     * off than v1.0. v1.0 had the hint/undo economy the ADS-01 dialog belongs to, so the
     * dialog must stay reachable and `rewardedOfferable` is `true`. v1.0 had **no
     * interstitials at all**, so there is nothing to keep reachable — and a stub reporting
     * one ready would put an unrequested dim, a quiescence pause and a scrim (D-08) in
     * front of a player who can never actually be shown an ad. That is strictly worse than
     * v1.0 for zero revenue. If a future change "corrects" this to `true` for symmetry
     * with the line above, this test is the thing that explains why it must not.
     */
    @Test
    fun interstitialReadyIsFalseSoTheStubNeverPausesThePlayer() =
        runTest {
            assertThat(gateway.interstitialReady.value).isFalse()
        }

    /**
     * Pins `showInterstitial() == false` — "no ad was presented".
     *
     * `false` is the answer that makes the caller navigate immediately and advance no
     * cadence counters, so a stub build is byte-for-byte the no-fill path (D-11): the
     * continue tap is honoured once, nothing is delayed, and nothing is spent. Returning
     * `true` here would tell a caller an ad played and let it burn a slot of the player's
     * session cap on an ad that never existed.
     */
    @Test
    fun showInterstitialReportsNotPresented() =
        runTest {
            assertThat(gateway.showInterstitial()).isFalse()
        }
}
