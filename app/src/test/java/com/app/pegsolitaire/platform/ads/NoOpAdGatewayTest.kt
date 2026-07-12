package com.app.pegsolitaire.platform.ads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Proves the Slice-1 [NoOpAdGateway] stub auto-grants every rewarded reason (D-11) and
 * that its interstitial hook is a safe no-op. This is the contract 04-03 (Hint) reuses
 * unchanged; Slice 2 swaps the binding, not this behavior.
 */
class NoOpAdGatewayTest {
    private val gateway = NoOpAdGateway()

    @Test
    fun requestRewardedGrantsHint() =
        runTest {
            assertThat(gateway.requestRewarded(AdReason.HINT)).isTrue()
        }

    @Test
    fun requestRewardedGrantsUndo() =
        runTest {
            assertThat(gateway.requestRewarded(AdReason.UNDO)).isTrue()
        }

    @Test
    fun showInterstitialDoesNotThrow() {
        gateway.showInterstitial()
    }
}
