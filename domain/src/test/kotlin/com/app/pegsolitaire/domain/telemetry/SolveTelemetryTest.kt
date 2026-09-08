package com.app.pegsolitaire.domain.telemetry

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Behaviour spec for the pure-domain [SolveTelemetry] carrier (D-08/D-09).
 *
 * The interesting behaviour is entirely in [SolveTelemetry.coerced]'s posture at an
 * untrusted boundary (T-12-06): it must CLAMP the one value that can corrupt a
 * downstream predicate — a negative counter — while passing everything else through
 * untouched, including implausibly large forged values. Rejecting those would convert a
 * tamper attempt, or an unrelated corruption bug, into progress loss for an honest
 * player, which is the trade `AssistAllowance.fromStoredOrFull` already refuses.
 */
class SolveTelemetryTest {
    @Test
    fun noneIsTheAllZeroNonTargetTelemetry() {
        // D-09: absent fields on an old resume blob read as this, not as "unknown".
        assertThat(SolveTelemetry.NONE.hintsUsed).isEqualTo(0)
        assertThat(SolveTelemetry.NONE.undosUsed).isEqualTo(0)
        assertThat(SolveTelemetry.NONE.lastPegOnTarget).isFalse()
    }

    @Test
    fun coercedClampsANegativeHintCountToZero() {
        val telemetry = SolveTelemetry.coerced(hintsUsed = -7, undosUsed = 2, lastPegOnTarget = false)

        assertThat(telemetry.hintsUsed).isEqualTo(0)
        assertThat(telemetry.undosUsed).isEqualTo(2)
    }

    @Test
    fun coercedClampsANegativeUndoCountToZero() {
        val telemetry = SolveTelemetry.coerced(hintsUsed = 1, undosUsed = -1, lastPegOnTarget = false)

        assertThat(telemetry.undosUsed).isEqualTo(0)
        assertThat(telemetry.hintsUsed).isEqualTo(1)
    }

    @Test
    fun coercedClampsBothCountersIndependently() {
        val telemetry = SolveTelemetry.coerced(hintsUsed = -3, undosUsed = -4, lastPegOnTarget = true)

        assertThat(telemetry).isEqualTo(SolveTelemetry(hintsUsed = 0, undosUsed = 0, lastPegOnTarget = true))
    }

    @Test
    fun coercedPassesInRangeValuesAndTheFinishFlagThrough() {
        val telemetry = SolveTelemetry.coerced(hintsUsed = 4, undosUsed = 9, lastPegOnTarget = true)

        assertThat(telemetry).isEqualTo(SolveTelemetry(hintsUsed = 4, undosUsed = 9, lastPegOnTarget = true))
    }

    @Test
    fun coercedPreservesAnImplausiblyLargeForgedCountRatherThanRefusingIt() {
        // T-12-06: the clamp BOUNDS, it does not police. A forged high assist count only
        // ever costs the forger an achievement; refusing it would cost an honest player
        // their recorded solve.
        val telemetry = SolveTelemetry.coerced(hintsUsed = 9999, undosUsed = 9999, lastPegOnTarget = false)

        assertThat(telemetry.hintsUsed).isEqualTo(9999)
        assertThat(telemetry.undosUsed).isEqualTo(9999)
    }

    @Test
    fun coercedOfZeroYieldsTheSameValueAsNone() {
        val telemetry = SolveTelemetry.coerced(hintsUsed = 0, undosUsed = 0, lastPegOnTarget = false)

        assertThat(telemetry).isEqualTo(SolveTelemetry.NONE)
    }
}
