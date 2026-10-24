package com.app.pegsolitaire.ui.game

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.app.pegsolitaire.R
import kotlinx.coroutines.delay

/**
 * The wrong tap's screen-reader sentence (D-06c, `docs/ux/invalid-tap-feedback.md` §3.6 and §6),
 * for [wrongTapAnnouncement] to put on the gameplay board: W1 "Empty hole. Pick up a peg first.",
 * W2 "Already holding a peg. Tap it again to put it down.", W3 "Can't jump there." — or `null`
 * while there is nothing to say.
 *
 * - **Every wrong tap is spoken once, an identical repeat included.** A live region speaks only
 *   when its content changes, so every other ordinal carries a trailing zero-width space, which no
 *   speech engine reads, and two equal sentences in a row still differ.
 * - **No focus stop is left behind.** The sentence is withdrawn after [ANNOUNCEMENT_HOLD_MS], long
 *   after the platform has dispatched the change, so a later swipe onto the board reads nothing.
 * - **Once per wrong tap** through [NewOrdinalEffect]: never for an ordinal a recreated screen
 *   carries (WT-6).
 * - Spoken only while a screen reader runs, and nothing changes for anyone else. A live region,
 *   not `View.announceForAccessibility`, which API 36 deprecates.
 *
 * Gameplay only, like the reject haptic: the tutorial attaches no live region (D-01j(2)). The kind
 * is read from the board and the selection at the tap, which a wrong tap changes neither of.
 */
@Composable
internal fun rememberWrongTapAnnouncement(state: GameUiState): String? {
    val resources = LocalResources.current
    var announcement by remember { mutableStateOf<String?>(null) }
    NewOrdinalEffect(state.rejectedTapTick) {
        val cell = state.rejectedTapCell ?: return@NewOrdinalEffect
        val sentence = resources.getString(WrongTapKind.of(state.board, state.selected, cell).sentence())
        // Even and odd ordinals differ by the unspoken mark, so consecutive equal sentences differ.
        announcement = if (state.rejectedTapTick % 2 == 0) sentence else sentence + REPEAT_MARK
        delay(ANNOUNCEMENT_HOLD_MS)
        announcement = null
    }
    return announcement
}

/**
 * Makes this node — the gameplay board itself, so the screen gains no node and keeps its traversal
 * (§11) — a **polite** live region, carrying [announcement] while there is one. Polite: it waits
 * for speech in progress and never interrupts it.
 */
internal fun Modifier.wrongTapAnnouncement(announcement: String?): Modifier =
    semantics {
        liveRegion = LiveRegionMode.Polite
        if (announcement != null) contentDescription = announcement
    }

/** §6's sentence for each wrong tap. */
@StringRes
private fun WrongTapKind.sentence(): Int =
    when (this) {
        WrongTapKind.EmptyHole -> R.string.wrong_tap_empty_hole
        WrongTapKind.OtherPeg -> R.string.wrong_tap_other_peg
        WrongTapKind.NotADestination -> R.string.wrong_tap_not_a_destination
    }

/**
 * How long a sentence stays on the board: well past the 100 ms in which Compose batches a semantics
 * change into the event a screen reader speaks from, and short enough that a swipe onto the board a
 * moment later finds nothing to re-read.
 */
private const val ANNOUNCEMENT_HOLD_MS = 1_000L

/** U+200B, a zero-width space: unspoken and unseen, but a change of content. */
private const val REPEAT_MARK = "\u200B"
