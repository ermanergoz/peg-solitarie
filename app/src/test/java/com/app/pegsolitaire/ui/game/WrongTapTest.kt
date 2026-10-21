package com.app.pegsolitaire.ui.game

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Position
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Which wrong tap a refused tap was, and which redirect answers it
 * (`docs/ux/invalid-tap-feedback.md` §1.1, §3.1 and §4).
 *
 * The kind is derived from the board and the selection at the tap — a wrong tap changes neither
 * (§3.7) — so the signal need not carry it. The redirect is §3.1's table for the game and §4's for
 * the tutorial, which differ only in that the tutorial forces Show-moves on (D-01b): one decision
 * serves both. Runner-free.
 */
class WrongTapTest {
    @Test
    fun aTapWithNothingHeldIsAnEmptyHoleTap() {
        assertThat(WrongTapKind.of(STRIP, held = null, tapped = HOLE)).isEqualTo(WrongTapKind.EmptyHole)
    }

    @Test
    fun aTapOnAnotherPegWhileOneIsHeldIsAnOtherPegTap() {
        assertThat(WrongTapKind.of(STRIP, held = HELD, tapped = OTHER_PEG)).isEqualTo(WrongTapKind.OtherPeg)
    }

    @Test
    fun aTapOnAHoleWhileOneIsHeldIsANotADestinationTap() {
        assertThat(WrongTapKind.of(STRIP, held = HELD, tapped = FAR_HOLE)).isEqualTo(WrongTapKind.NotADestination)
    }

    @Test
    fun eachWrongTapGetsTheRedirectItsTablePrescribes() {
        val rows =
            listOf(
                Row(
                    "W1: nothing is held, so nothing to point at",
                    WrongTapKind.EmptyHole,
                    want = WrongTapRedirect.None,
                ),
                Row("W2: the held peg is the answer", WrongTapKind.OtherPeg, want = WrongTapRedirect.RingNudge),
                Row(
                    "W2 even with Show-moves off",
                    WrongTapKind.OtherPeg,
                    showHints = false,
                    want = WrongTapRedirect.RingNudge,
                ),
                Row(
                    "W3: these are the places it can go",
                    WrongTapKind.NotADestination,
                    want = WrongTapRedirect.DotRePop,
                ),
                Row(
                    "W3 with Show-moves off: no free hint",
                    WrongTapKind.NotADestination,
                    showHints = false,
                    want = WrongTapRedirect.RingNudge,
                ),
                Row(
                    "W3 holding a peg that cannot move: no dot to pop",
                    WrongTapKind.NotADestination,
                    destinations = 0,
                    want = WrongTapRedirect.RingNudge,
                ),
            )

        rows.forEach { row ->
            assertWithMessage(row.name)
                .that(wrongTapRedirectFor(row.kind, row.showHints, row.destinations, animationsEnabled = true))
                .isEqualTo(row.want)
        }
    }

    @Test
    fun withAnimationsOffNoWrongTapIsRedirected() {
        // §3.2: the ring and the dots are already drawn and still, and a ring that blinked
        // thicker would be a flash.
        WrongTapKind.entries.forEach { kind ->
            assertWithMessage("$kind")
                .that(wrongTapRedirectFor(kind, showHints = true, heldDestinations = 1, animationsEnabled = false))
                .isEqualTo(WrongTapRedirect.None)
        }
    }

    private data class Row(
        val name: String,
        val kind: WrongTapKind,
        val showHints: Boolean = true,
        val destinations: Int = 1,
        val want: WrongTapRedirect,
    )

    private companion object {
        /** A 1x5 strip with pegs at cols 0, 1 and 3: the held peg at 0 can only land on 2. */
        val STRIP: Board =
            Board(spec = BoardSpec(inPlay = (0 until 5).map { Position(row = 0, col = it) }, target = null), pegs = 0L)
                .withPeg(Position(row = 0, col = 0))
                .withPeg(Position(row = 0, col = 1))
                .withPeg(Position(row = 0, col = 3))

        val HELD = Position(row = 0, col = 0)
        val OTHER_PEG = Position(row = 0, col = 3)
        val HOLE = Position(row = 0, col = 2)
        val FAR_HOLE = Position(row = 0, col = 4)
    }
}
