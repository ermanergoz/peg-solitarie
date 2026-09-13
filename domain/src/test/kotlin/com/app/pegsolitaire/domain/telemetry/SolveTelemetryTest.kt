package com.app.pegsolitaire.domain.telemetry

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Behaviour spec for the pure-domain [SolveTelemetry] carrier (D-08/D-09).
 *
 * The interesting behaviour is entirely in [SolveTelemetry.coerced]'s posture at an
 * untrusted boundary (T-12-06): it must RESOLVE the one value that can never be honest —
 * a negative counter — toward [SolveTelemetry.UNACCOUNTED_ASSIST] rather than toward the
 * zero the Hint-free / Undo-free predicates are looking for, while passing everything
 * else through untouched, including implausibly large forged values. It never refuses:
 * rejecting would convert a tamper attempt, or an unrelated corruption bug, into progress
 * loss for an honest player, the trade `AssistAllowance.fromStoredOrFull` already refuses.
 *
 * Note which direction each case points. The *absent* case (zero, supplied by the
 * `Results` route's default per D-09) fails OPEN toward the achievement and is accepted as
 * a product decision; only the *forged negative* is resolved against the forger.
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
    fun aForgedNegativeHintCountDisqualifiesPurityRatherThanResolvingToZero() {
        // Zero is exactly what the Hint-free predicate (`hintsUsed = 0`) rewards, so
        // resolving a forged negative to it would pay the forger the badge they edited the
        // value to get. One unaccounted assist is the smallest honest answer.
        val telemetry = SolveTelemetry.coerced(hintsUsed = -7, undosUsed = 2, lastPegOnTarget = false)

        assertThat(telemetry.hintsUsed).isEqualTo(SolveTelemetry.UNACCOUNTED_ASSIST)
        assertThat(telemetry.hintsUsed).isNotEqualTo(0)
        assertThat(telemetry.undosUsed).isEqualTo(2)
    }

    @Test
    fun aForgedNegativeUndoCountDisqualifiesPurityRatherThanResolvingToZero() {
        val telemetry = SolveTelemetry.coerced(hintsUsed = 1, undosUsed = -1, lastPegOnTarget = false)

        assertThat(telemetry.undosUsed).isEqualTo(SolveTelemetry.UNACCOUNTED_ASSIST)
        assertThat(telemetry.undosUsed).isNotEqualTo(0)
        assertThat(telemetry.hintsUsed).isEqualTo(1)
    }

    @Test
    fun bothForgedNegativeCountersAreResolvedIndependentlyAndTheFinishFlagSurvives() {
        val telemetry = SolveTelemetry.coerced(hintsUsed = -3, undosUsed = -4, lastPegOnTarget = true)

        assertThat(telemetry).isEqualTo(
            SolveTelemetry(
                hintsUsed = SolveTelemetry.UNACCOUNTED_ASSIST,
                undosUsed = SolveTelemetry.UNACCOUNTED_ASSIST,
                lastPegOnTarget = true,
            ),
        )
    }

    @Test
    fun coercedPassesInRangeValuesAndTheFinishFlagThrough() {
        val telemetry = SolveTelemetry.coerced(hintsUsed = 4, undosUsed = 9, lastPegOnTarget = true)

        assertThat(telemetry).isEqualTo(SolveTelemetry(hintsUsed = 4, undosUsed = 9, lastPegOnTarget = true))
    }

    @Test
    fun coercedPreservesAnImplausiblyLargeForgedCountRatherThanRefusingIt() {
        // T-12-06: the resolution BOUNDS, it does not police. A forged high assist count only
        // ever costs the forger an achievement; refusing it would cost an honest player
        // their recorded solve.
        val telemetry = SolveTelemetry.coerced(hintsUsed = 9999, undosUsed = 9999, lastPegOnTarget = false)

        assertThat(telemetry.hintsUsed).isEqualTo(9999)
        assertThat(telemetry.undosUsed).isEqualTo(9999)
    }

    @Test
    fun coercedOfZeroYieldsTheSameValueAsNone() {
        // The other direction, stated so it cannot be "fixed" by mistake: D-09 locks the
        // ABSENT case at zero, which DOES grant purity. That is accepted, not an oversight
        // — only the forged negative above is resolved against the forger.
        val telemetry = SolveTelemetry.coerced(hintsUsed = 0, undosUsed = 0, lastPegOnTarget = false)

        assertThat(telemetry).isEqualTo(SolveTelemetry.NONE)
    }
}
