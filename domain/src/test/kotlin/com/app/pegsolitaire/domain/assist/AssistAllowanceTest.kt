package com.app.pegsolitaire.domain.assist

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Behaviour spec for the pure-domain [AssistAllowance] free-assist policy (ADS-02).
 *
 * Every expectation about a *limit* is written as [AssistAllowanceLimits.FREE_HINTS] or
 * [AssistAllowanceLimits.FREE_UNDOS], never as a bare `3`/`5` — D-02 says those two
 * numbers are named exactly once in the codebase, and a test literal that re-states one
 * would be a second definition that could silently drift from the first. Bare literals
 * used as arbitrary *stored* values (an in-range `4`, a forged `9999`) are fine: they are
 * fixtures, not restatements of the policy.
 */
class AssistAllowanceTest {
    @Test
    fun defaultAllowanceStartsAtBothFullLimits() {
        val allowance = AssistAllowance()

        assertThat(allowance.hintsRemaining).isEqualTo(AssistAllowanceLimits.FREE_HINTS)
        assertThat(allowance.undosRemaining).isEqualTo(AssistAllowanceLimits.FREE_UNDOS)
    }

    @Test
    fun remainingReturnsTheCounterMatchingTheKind() {
        val allowance = AssistAllowance(hintsRemaining = 1, undosRemaining = 2)

        assertThat(allowance.remaining(AssistKind.HINT)).isEqualTo(1)
        assertThat(allowance.remaining(AssistKind.UNDO)).isEqualTo(2)
    }

    @Test
    fun hasFreeIsTrueExactlyWhileTheCounterIsAboveZero() {
        val some = AssistAllowance(hintsRemaining = 1, undosRemaining = 0)

        assertThat(some.hasFree(AssistKind.HINT)).isTrue()
        assertThat(some.hasFree(AssistKind.UNDO)).isFalse()
    }

    @Test
    fun spendingAHintLeavesTheUndoCounterUntouched() {
        // D-01: the two counters are independent, not one shared pool.
        val spent = AssistAllowance().spend(AssistKind.HINT)

        assertThat(spent.hintsRemaining).isEqualTo(AssistAllowanceLimits.FREE_HINTS - 1)
        assertThat(spent.undosRemaining).isEqualTo(AssistAllowanceLimits.FREE_UNDOS)
    }

    @Test
    fun spendingAnUndoLeavesTheHintCounterUntouched() {
        val spent = AssistAllowance().spend(AssistKind.UNDO)

        assertThat(spent.undosRemaining).isEqualTo(AssistAllowanceLimits.FREE_UNDOS - 1)
        assertThat(spent.hintsRemaining).isEqualTo(AssistAllowanceLimits.FREE_HINTS)
    }

    @Test
    fun spendingAtZeroStaysAtZeroAndNeverGoesNegative() {
        // T-09-03: a negative counter would later read as "infinite free assists"
        // through a signed comparison, so spend() is total for every reachable state.
        val empty = AssistAllowance(hintsRemaining = 0, undosRemaining = 0)

        val hintTwice = empty.spend(AssistKind.HINT).spend(AssistKind.HINT)
        val undoTwice = empty.spend(AssistKind.UNDO).spend(AssistKind.UNDO)

        assertThat(hintTwice.hintsRemaining).isEqualTo(0)
        assertThat(undoTwice.undosRemaining).isEqualTo(0)
    }

    @Test
    fun refillRestoresTheFullLimitRegardlessOfCurrentValueAndLeavesTheOtherKindAlone() {
        // D-03: a watched ad refills the counter to FULL, it does not grant +1.
        val drained = AssistAllowance(hintsRemaining = 0, undosRemaining = 1)

        val refilled = drained.refill(AssistKind.HINT)

        assertThat(refilled.hintsRemaining).isEqualTo(AssistAllowanceLimits.FREE_HINTS)
        assertThat(refilled.undosRemaining).isEqualTo(1)
    }

    @Test
    fun refillingUndosRestoresTheFullLimitAndLeavesHintsAlone() {
        val drained = AssistAllowance(hintsRemaining = 2, undosRemaining = 0)

        val refilled = drained.refill(AssistKind.UNDO)

        assertThat(refilled.undosRemaining).isEqualTo(AssistAllowanceLimits.FREE_UNDOS)
        assertThat(refilled.hintsRemaining).isEqualTo(2)
    }

    @Test
    fun refillThenSpendLeavesOneBelowTheLimit() {
        // The Granted-path ordering GameViewModel uses: refill first, then consume.
        val empty = AssistAllowance(hintsRemaining = 0, undosRemaining = 0)

        val afterHint = empty.refill(AssistKind.HINT).spend(AssistKind.HINT)
        val afterUndo = empty.refill(AssistKind.UNDO).spend(AssistKind.UNDO)

        assertThat(afterHint.hintsRemaining).isEqualTo(AssistAllowanceLimits.FREE_HINTS - 1)
        assertThat(afterUndo.undosRemaining).isEqualTo(AssistAllowanceLimits.FREE_UNDOS - 1)
    }

    @Test
    fun absentStoredCountersFallOpenToAFullAllowance() {
        // D-04/D-17: null means "written before the allowance existed", not "zero left".
        val restored = AssistAllowance.fromStoredOrFull(hints = null, undos = null)

        assertThat(restored.hintsRemaining).isEqualTo(AssistAllowanceLimits.FREE_HINTS)
        assertThat(restored.undosRemaining).isEqualTo(AssistAllowanceLimits.FREE_UNDOS)
    }

    @Test
    fun storedZeroesArePreservedBecauseZeroIsInRange() {
        // A genuinely exhausted allowance must survive process death as exhausted;
        // fail-open applies to *invalid* values, never to valid ones.
        val restored = AssistAllowance.fromStoredOrFull(hints = 0, undos = 0)

        assertThat(restored.hintsRemaining).isEqualTo(0)
        assertThat(restored.undosRemaining).isEqualTo(0)
    }

    @Test
    fun outOfRangeStoredCounterIsRejectedPerKindWhileTheValidOneSurvives() {
        // T-09-01: a forged 9999 must never become 9999 free hints. It degrades to the
        // default allowance, and the sibling counter is judged on its own merits.
        val restored = AssistAllowance.fromStoredOrFull(hints = 9999, undos = 4)

        assertThat(restored.hintsRemaining).isEqualTo(AssistAllowanceLimits.FREE_HINTS)
        assertThat(restored.undosRemaining).isEqualTo(4)
    }

    @Test
    fun negativeStoredCountersFallOpenOnBothKinds() {
        val restored = AssistAllowance.fromStoredOrFull(hints = -1, undos = -1)

        assertThat(restored.hintsRemaining).isEqualTo(AssistAllowanceLimits.FREE_HINTS)
        assertThat(restored.undosRemaining).isEqualTo(AssistAllowanceLimits.FREE_UNDOS)
    }
}
