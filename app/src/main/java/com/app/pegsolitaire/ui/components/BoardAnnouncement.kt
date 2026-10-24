package com.app.pegsolitaire.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.game.GameUiState
import com.app.pegsolitaire.ui.game.NewOrdinalEffect
import com.app.pegsolitaire.ui.game.WrongTapKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * How long a line stays on the board's channel: well past the 100 ms in which Compose batches a
 * semantics change into the event a screen reader speaks from, and short enough that a swipe onto
 * the board a moment later finds nothing to re-read (D-06c's hold, kept by D-TBa item 2).
 */
internal const val ANNOUNCEMENT_HOLD_MS = 1_000L

/** U+200B, a zero-width space: unspoken and unseen, but a change of content. */
internal const val REPEAT_MARK = "​"

/**
 * The board's one polite channel (D-TB as amended by D-TBa items 2 and 4, `docs/ux-spec.md` §11.1,
 * "Every change is spoken once"), owned by the screen that hosts the board.
 *
 * - **One channel per board.** Every board event goes through [announce]; the canvas carries
 *   [current] as its live region's text and chooses no wording. The rescue banner and the Undecided
 *   notice keep their own regions and never pass through here.
 * - **A newer line replaces one not yet spoken**, inside this channel only: [current] is simply the
 *   latest line.
 * - **An identical repeat is a new event.** A live region speaks only when its content changes, so a
 *   line equal to the last one alternates a trailing [REPEAT_MARK], which no speech engine reads.
 * - **No focus stop is left behind.** Each line is withdrawn after [ANNOUNCEMENT_HOLD_MS], and a
 *   withdrawal applies only to the line it was scheduled for, so a second event inside the hold is
 *   never cut short by the first one's clear.
 *
 * Live regions only: `View.announceForAccessibility` is deprecated from API 36 and is not used.
 */
@Stable
internal class BoardAnnouncer(
    private val scope: CoroutineScope,
) {
    /** The line the board's live region carries right now, or `null` while it has nothing to say. */
    var current: String? by mutableStateOf(null)
        private set

    private var lastLine: String? = null
    private var lastMarked = false
    private var generation = 0

    /** Puts [line] on the channel, replacing anything not yet spoken. */
    fun announce(line: String) {
        val marked = line == lastLine && !lastMarked
        lastLine = line
        lastMarked = marked
        current = if (marked) line + REPEAT_MARK else line
        val announced = ++generation
        scope.launch {
            delay(ANNOUNCEMENT_HOLD_MS)
            if (generation == announced) current = null
        }
    }
}

/** The board's channel for this screen, with the screen's lifetime. */
@Composable
internal fun rememberBoardAnnouncer(): BoardAnnouncer {
    val scope = rememberCoroutineScope()
    return remember(scope) { BoardAnnouncer(scope) }
}

/**
 * Makes this node — the board's canvas, so the screen gains no node — a **polite** live region
 * carrying [announcement] while there is one. Polite: it waits for speech in progress.
 */
internal fun Modifier.boardAnnouncement(announcement: String?): Modifier =
    semantics {
        liveRegion = LiveRegionMode.Polite
        if (announcement != null) contentDescription = announcement
    }

/**
 * Every line the gameplay board speaks (§11.1's table): the three shared with the tutorial, then
 * undo, restart and the revealed hint. Composed in that order, so when one state update brings two
 * events the later source's line is the newer one, and it is the one left on the channel.
 */
@Composable
internal fun GameplayBoardAnnouncements(
    state: GameUiState,
    announcer: BoardAnnouncer,
) {
    val format = rememberBoardSpeechFormat()
    SelectionAnnouncements(state, announcer)
    RejectedTapAnnouncements(state, announcer)
    JumpAnnouncements(state, announcer)
    OrdinalLine(state.undoTick, announcer) {
        state.undoneMove?.let { format.announcement(BoardEvent.Undone(it.from, it.to, state.board.pegCount)) }
    }
    OrdinalLine(state.restartTick, announcer) { format.announcement(BoardEvent.Restarted(state.board.pegCount)) }
    // A revealed hint is a new non-null pair; an Undecided or lost search reveals none (D-TBa item 4).
    val hint = state.hintSource?.let { from -> state.hintTarget?.let { to -> from to to } }
    ChangeEffect(hint) { _, now ->
        if (now != null) announcer.announce(format.announcement(BoardEvent.Hinted(now.first, now.second)))
    }
}

/**
 * A selection ("Selected row 2, column 4, 1 jump.") or a put-down ("Peg put down, row 2, column 4."),
 * on either board. A put-down is the selection cleared with nothing else moving: not by a jump, an
 * undo or a restart (each changes the board or its own ordinal), not by a rejected tap, and not by a
 * coach escalation that releases a held peg, which speaks only its hint line (§11.1).
 */
@Composable
internal fun SelectionAnnouncements(
    board: GameUiState,
    announcer: BoardAnnouncer,
) {
    val format = rememberBoardSpeechFormat()
    val now =
        SelectionView(
            board.selected,
            board.board,
            board.hintSource,
            board.hintTarget,
            board.rejectedTapTick,
            board.restartTick,
        )
    ChangeEffect(now) { before, after ->
        val samePosition = before.copy(selected = after.selected) == after
        when {
            !samePosition -> Unit
            before.selected == null && after.selected != null ->
                announcer.announce(
                    format.announcement(
                        BoardEvent.Selected(after.selected, BoardSpeechModel.jumpsFrom(after.board, after.selected)),
                    ),
                )
            before.selected != null && after.selected == null ->
                announcer.announce(format.announcement(BoardEvent.PutDown(before.selected)))
        }
    }
}

/** A refused tap, in D-TB's neutral line for its kind, once per tap (D-TBa items 2 and 3). */
@Composable
internal fun RejectedTapAnnouncements(
    board: GameUiState,
    announcer: BoardAnnouncer,
) {
    val format = rememberBoardSpeechFormat()
    OrdinalLine(board.rejectedTapTick, announcer) {
        board.rejectedTapCell?.let { cell ->
            format.announcement(BoardEvent.Rejected(WrongTapKind.of(board.board, board.selected, cell)))
        }
    }
}

/** A jump, by tap or by action, as its animation begins: from, to, the peg removed, the pegs left. */
@Composable
internal fun JumpAnnouncements(
    board: GameUiState,
    announcer: BoardAnnouncer,
) {
    val format = rememberBoardSpeechFormat()
    ChangeEffect(board.animating) { _, now ->
        if (now != null) {
            announcer.announce(
                format.announcement(BoardEvent.Jumped(now.from, now.to, now.removed, board.board.pegCount)),
            )
        }
    }
}

/**
 * Announces the line [line] gives each time [ordinal] rises past the last value seen — never for the
 * value first seen, so a screen composed again over a carried ordinal speaks nothing ([NewOrdinalEffect]).
 */
@Composable
internal fun OrdinalLine(
    ordinal: Int,
    announcer: BoardAnnouncer,
    line: () -> String?,
) {
    NewOrdinalEffect(ordinal) { line()?.let(announcer::announce) }
}

/**
 * Runs [onChange] with the value before and after each time [value] changes — never for the value
 * the composition first sees, so a screen composed again over the same state speaks nothing.
 */
@Composable
internal fun <T> ChangeEffect(
    value: T,
    onChange: suspend (before: T, now: T) -> Unit,
) {
    val seen = remember { LastSeen(value) }
    val latest by rememberUpdatedState(onChange)
    LaunchedEffect(value) {
        val before = seen.value
        seen.value = value
        if (before != value) latest(before, value)
    }
}

/** The last value a [ChangeEffect] saw. Plain state: read and written only inside the effect. */
private class LastSeen<T>(
    var value: T,
)

/** What a selection change is judged against: the selection and everything that can clear it. */
private data class SelectionView(
    val selected: Position?,
    val board: Board,
    val hintSource: Position?,
    val hintTarget: Position?,
    val rejectedTapTick: Int,
    val restartTick: Int,
)
