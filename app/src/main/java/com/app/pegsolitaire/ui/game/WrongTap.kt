package com.app.pegsolitaire.ui.game

import android.os.Build
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Cell
import com.app.pegsolitaire.domain.model.Position

/**
 * The three taps the board refuses (D-06d, `docs/ux/invalid-tap-feedback.md` §1.1). A put-down,
 * a jump and a selection — even of a peg that cannot move — are successful actions, and a tap that
 * lands on no in-play cell is not an event at all, so these three are the complete set.
 */
enum class WrongTapKind {
    /** W1: an empty hole while nothing is held. */
    EmptyHole,

    /** W2: a different peg while one is held — the locked tap rule keeps the held one. */
    OtherPeg,

    /** W3: an empty hole that is not a destination of the held peg. */
    NotADestination,
    ;

    companion object {
        /**
         * The kind of the wrong tap on [tapped], read from the board and the selection at the
         * moment of the tap. A wrong tap changes neither (§3.7), so the signal carries only the
         * ordinal and the cell, and the kind is derived here when it is needed.
         */
        fun of(
            board: Board,
            held: Position?,
            tapped: Position,
        ): WrongTapKind =
            when {
                held == null -> EmptyHole
                board.cellAt(tapped) == Cell.Peg -> OtherPeg
                else -> NotADestination
            }
    }
}

/** What points the player at the next tap while a peg is held (§3.1). */
enum class WrongTapRedirect {
    /** No redirect: nothing is held (W1), or animations are off. */
    None,

    /** The held peg's selection ring grows and returns: "you are still holding this one". */
    RingNudge,

    /** The held peg's destination dots replay their pop-in: "these are the places it can go". */
    DotRePop,
}

/**
 * The redirect for a wrong tap of [kind], §3.1's table for the game and §4's for the tutorial,
 * which forces Show-moves on (D-01b) and so needs no table of its own.
 *
 * - **W1** points nowhere: nothing is held, and pointing at a movable peg would be a hint.
 * - **W2** nudges the ring, because the held peg is the answer (§4.1 reason 3).
 * - **W3** re-pops the dots, unless Show-moves is off — re-popping dots the player has hidden
 *   would turn a wrong tap into a free hint — or the held peg has no destination to pop.
 * - **With animations off nothing redirects** (§3.2): the ring and the dots are already drawn and
 *   still, and a ring that blinked thicker would be a flash.
 *
 * @param heldDestinations how many destination dots the held peg has.
 */
internal fun wrongTapRedirectFor(
    kind: WrongTapKind,
    showHints: Boolean,
    heldDestinations: Int,
    animationsEnabled: Boolean,
): WrongTapRedirect =
    when {
        !animationsEnabled || kind == WrongTapKind.EmptyHole -> WrongTapRedirect.None
        kind == WrongTapKind.NotADestination && showHints && heldDestinations > 0 -> WrongTapRedirect.DotRePop
        else -> WrongTapRedirect.RingNudge
    }

/**
 * Whether a wrong tap is felt (D-06b, `docs/ux/invalid-tap-feedback.md` §3.4): only while Haptics is
 * on, and only from API 30 ([Build.VERSION_CODES.R]), where Compose's `HapticFeedbackType.Reject`
 * reaches the platform's own `REJECT`.
 *
 * The API check is load-bearing (tech-lead Ruling 4): below 30 the pinned androidx.core rewrites
 * `REJECT` to the heavy `LONG_PRESS`, which §3.4 forbids, so the gate holds here, in our own code,
 * and a wrong tap below API 30 is felt by nothing. The same `hapticsOn` gates the jump haptic, and
 * nothing else gates either: not Sound, not the ringer (silent-mode design §1.7).
 *
 * @param sdkInt the running platform's API level; the screen passes `Build.VERSION.SDK_INT`.
 */
internal fun wrongTapHapticFires(
    hapticsOn: Boolean,
    sdkInt: Int,
): Boolean = hapticsOn && sdkInt >= Build.VERSION_CODES.R
