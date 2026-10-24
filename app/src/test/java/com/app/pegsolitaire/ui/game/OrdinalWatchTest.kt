package com.app.pegsolitaire.ui.game

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * When a one-shot ordinal counts as new for an effect (D-06, WT-6): only when it rises past the
 * value this watch last saw — never for the value it was created with, whatever that is.
 *
 * That is stricter than the house `> 0` guard on purpose. The `> 0` guard keeps a fresh state at
 * 0 quiet, but a screen recomposed from scratch with a carried ordinal of 3 — a rotation recreates
 * the Activity, and the ViewModel keeps its state — would pass it and fire again. Runner-free.
 */
class OrdinalWatchTest {
    @Test
    fun theValueAWatchStartsWithIsNeverNewEvenAboveZero() {
        assertThat(OrdinalWatch(0).advancedTo(0)).isFalse()
        assertThat(OrdinalWatch(CARRIED).advancedTo(CARRIED)).isFalse()
    }

    @Test
    fun eachRiseIsNewOnceAndOnlyOnce() {
        val watch = OrdinalWatch(0)

        assertThat(watch.advancedTo(1)).isTrue()
        assertThat(watch.advancedTo(1)).isFalse()
        assertThat(watch.advancedTo(2)).isTrue()
    }

    @Test
    fun aDropIsNotNewButTheRiseAfterItIs() {
        // Ordinals are carried, never reset, so a drop should not happen; if one ever does, the
        // next real wrong tap must still be answered.
        val watch = OrdinalWatch(CARRIED)

        assertThat(watch.advancedTo(0)).isFalse()
        assertThat(watch.advancedTo(1)).isTrue()
    }

    private companion object {
        const val CARRIED = 3
    }
}
