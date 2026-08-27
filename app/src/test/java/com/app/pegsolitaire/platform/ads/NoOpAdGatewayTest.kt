package com.app.pegsolitaire.platform.ads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Proves the [NoOpAdGateway] stub answers [RewardOutcome.Granted] to every rewarded
 * reason (D-02) — never [RewardOutcome.Declined] and never [RewardOutcome.Unavailable] —
 * and that its interstitial hook is a safe no-op. Granting unconditionally is what keeps
 * hint and undo free and byte-identical to v1.0; Phase 9 swaps the binding, not this
 * behavior.
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

    @Test
    fun showInterstitialDoesNotThrow() {
        gateway.showInterstitial()
    }
}
