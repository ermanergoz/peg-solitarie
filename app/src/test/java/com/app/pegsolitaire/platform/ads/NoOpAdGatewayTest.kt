package com.app.pegsolitaire.platform.ads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Proves the [NoOpAdGateway] stub answers [RewardOutcome.Granted] to every rewarded
 * reason (D-02) — never [RewardOutcome.Declined] and never [RewardOutcome.Unavailable] —
 * that it reports rewarded offers as available so the ADS-01 opt-in dialog is reachable
 * against the stub, and that its interstitial hook is a safe no-op. Granting
 * unconditionally is what keeps hint and undo free and byte-identical to v1.0.
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

    @Test
    fun showInterstitialDoesNotThrow() {
        gateway.showInterstitial()
    }
}
