package com.app.pegsolitaire.ui.game

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.clayEntrance
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.rememberAnimationsEnabled
import kotlinx.coroutines.delay

/** Test tag of the Undecided notice, in the idiom of [NOWIN_BANNER_TEST_TAG]. */
const val HINT_UNDECIDED_NOTICE_TEST_TAG = "hint_undecided_notice"

/**
 * The Undecided notice (ux-spec §4.4.2: D-04f, as amended by D-04g): what the player sees when a
 * Hint search stopped with no move and no proof. A quiet pill on the app's neutral message
 * surface, with the Hint lightbulb at its start, in the rescue banner's slot — never the banner,
 * which now means a proven dead end.
 *
 * A stateless leaf: it renders [message] and reports a put-away through [onDismiss]. When it shows
 * and for how long belong to its host, [UndecidedNoticeSlot].
 *
 * **A tap on it puts it away and reaches nothing else** (D-04g). Its own tap detector makes it
 * the topmost hit under the touch, so a peg the player cannot see beneath it is never picked up;
 * a touch anywhere else on the board plays as before. It is a plain container rather than an M3
 * `Surface` — a `Surface` would add a traversal group and an input owner of its own — and its tap
 * is a gesture rather than a `clickable`, because the notice has no click action and no role: it
 * is a status message, and TalkBack gets the same choice as a `dismiss` action, the idiom of M3's
 * own `SnackbarHost`.
 *
 * **One accessibility node, spoken politely** (criteria 23c, 23d): it merges its children, so the
 * one stop reads its text; the glyph is decorative; and it is a polite live region, so TalkBack
 * reads it after whatever it is saying, without moving focus off the Hint control.
 *
 * At least `hint_notice_min_height` (48dp) tall, so a short notice is still a fair target. When
 * that floor makes room, the glyph and the text are centred in it together; otherwise the glyph
 * sits on the text's first line.
 *
 * @param message the resolved `hint_undecided_message`.
 * @param onDismiss puts the notice away: a tap on it, or TalkBack's Dismiss.
 * @param modifier layout modifier for the notice.
 */
@Composable
fun HintUndecidedNotice(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val shape = MaterialTheme.shapes.extraLarge
    Row(
        modifier =
            modifier
                .widthIn(max = dimensionResource(R.dimen.board_max_size))
                .heightIn(min = dimensionResource(R.dimen.hint_notice_min_height))
                .clayShadow(dimensionResource(R.dimen.elevation_card), shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, shape)
                .pointerInput(Unit) { detectTapGestures(onTap = { currentOnDismiss() }) }
                .semantics(mergeDescendants = true) {
                    liveRegion = LiveRegionMode.Polite
                    dismiss {
                        currentOnDismiss()
                        true
                    }
                }.testTag(HINT_UNDECIDED_NOTICE_TEST_TAG)
                // Content padding, inside the pill: the floor, the shape and the tap area above
                // are the whole notice's.
                .padding(
                    horizontal = dimensionResource(R.dimen.spacing_md),
                    vertical = dimensionResource(R.dimen.spacing_sm),
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_sm)),
        ) {
            Icon(
                imageVector = HintIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(dimensionResource(R.dimen.hint_notice_glyph_size)),
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * The Undecided notice's place on the gameplay screen: it shows [HintUndecidedNotice] once for
 * each raise of [raised] — `GameUiState.hintUndecided` — and owns everything about it that is
 * the screen's rather than the ViewModel's (ux-spec §4.4.2, §7 R-7). Nothing is reported back.
 *
 * - **How long it stays.** `GameAnim.HINT_NOTICE_DISPLAY_MS`, lengthened by the player's
 *   accessibility timeout ("Time to take action") through the same call M3's `SnackbarHost`
 *   makes, and the plain 5 s when there is no accessibility manager. A tap or a Dismiss ends it
 *   at any time, a lengthened display included (D-04g item 4).
 * - **Never twice for one outcome.** Whether the current raise has ended — timed out or put away
 *   — is remembered across recreation, and forgotten when [raised] turns false, so a notice that
 *   has gone does not come back on recomposition, a configuration change or a resume, while a
 *   later Undecided is a new raise that shows afresh (criterion 23g).
 * - **Every raise is a node of its own.** The notice is keyed on a count of raises, so a raise
 *   that lands while the last notice is still fading out is a new node — which is what makes the
 *   live region speak again, even with the same words (criterion 23d).
 * - **Motion.** In: the house entrance, `clayEntrance(0)`. Out: a fade in place over
 *   `GameAnim.HINT_NOTICE_EXIT_MS`. With animations off it appears settled and goes in one frame,
 *   and its time is unchanged: that is reading time, not decoration.
 *
 * @param raised whether the last Hint search ended Undecided on the board on screen.
 * @param modifier the slot's placement in its host (the board region's bottom centre).
 */
@Composable
internal fun UndecidedNoticeSlot(
    raised: Boolean,
    modifier: Modifier = Modifier,
) {
    val raiseKey = rememberRaiseKey(raised)
    var ended by rememberSaveable(raised) { mutableStateOf(false) }
    val showing = raised && !ended
    val accessibilityManager = LocalAccessibilityManager.current
    if (showing) {
        LaunchedEffect(raiseKey) {
            delay(accessibilityManager.noticeDisplayMillis())
            ended = true
        }
    }
    val message = stringResource(R.string.hint_undecided_message)
    val animationsEnabled = rememberAnimationsEnabled()
    key(raiseKey) {
        if (animationsEnabled) {
            AnimatedVisibility(
                visible = showing,
                modifier = modifier,
                enter = EnterTransition.None,
                exit = fadeOut(tween(durationMillis = GameAnim.HINT_NOTICE_EXIT_MS)),
            ) {
                HintUndecidedNotice(
                    message = message,
                    onDismiss = { ended = true },
                    modifier = Modifier.clayEntrance(0),
                )
            }
        } else if (showing) {
            HintUndecidedNotice(message = message, onDismiss = { ended = true }, modifier = modifier)
        }
    }
}

/**
 * A key that changes each time [raised] turns true, and only then: a lowered notice keeps its key
 * so that its exit can run, and the next raise gets a new one.
 */
@Composable
private fun rememberRaiseKey(raised: Boolean): Int {
    val counter = remember { RaiseCounter() }
    return remember(raised) { counter.next(raised) }
}

/** Counts the raises a slot has seen. */
private class RaiseCounter {
    private var raises = 0

    /** The key for [raised]: a new count for a raise, the current one for a lowering. */
    fun next(raised: Boolean): Int {
        if (raised) raises++
        return raises
    }
}

/**
 * How long the notice stays when left alone: the player's recommended timeout for a message with
 * an icon and text and no controls, or the plain display time when there is no manager (§4.4.2).
 */
private fun AccessibilityManager?.noticeDisplayMillis(): Long {
    val displayMillis = GameAnim.HINT_NOTICE_DISPLAY_MS.toLong()
    return this?.calculateRecommendedTimeoutMillis(
        originalTimeoutMillis = displayMillis,
        containsIcons = true,
        containsText = true,
        containsControls = false,
    ) ?: displayMillis
}

/** The notice in both themes, over the board slab's colour, as it sits on the screen. */
@Preview(name = "Undecided notice · light")
@Preview(name = "Undecided notice · dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HintUndecidedNoticePreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            HintUndecidedNotice(
                message = stringResource(R.string.hint_undecided_message),
                onDismiss = {},
                modifier = Modifier.padding(dimensionResource(R.dimen.spacing_md)),
            )
        }
    }
}
