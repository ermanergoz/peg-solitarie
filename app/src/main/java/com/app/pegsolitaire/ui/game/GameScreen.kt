package com.app.pegsolitaire.ui.game

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.app.pegsolitaire.R
import com.app.pegsolitaire.audio.JumpSoundEffect
import com.app.pegsolitaire.audio.LocalSoundPlayer
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.audio.withClickSound
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.components.BoardCanvas
import com.app.pegsolitaire.ui.components.ClaySnackbarHost
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish
import com.app.pegsolitaire.ui.theme.ctaGradientBrush

/** Test tag of the board-state node carrying the [PegCountKey]/[GameStatusKey]/[LegalTargetCountKey] semantics. */
const val GAME_STATE_TEST_TAG = "game_state"

/**
 * Test tag of the board's clay slab — the surface whose edge the board keeps once each system bar
 * is applied once (ux-spec §4.3.1, criterion 18c). A tag, not a description: the slab announces
 * nothing.
 */
const val GAME_BOARD_SLAB_TEST_TAG = "game_board_slab"

/** Semantics: pegs currently on the board — lets the flow test assert a jump removed one peg. */
val PegCountKey = SemanticsPropertyKey<Int>("PegCount")

/** Semantics: the [GameStatus] name — lets the flow test assert a terminal status. */
val GameStatusKey = SemanticsPropertyKey<String>("GameStatus")

/** Semantics: number of highlighted legal destinations — lets the flow test assert selection. */
val LegalTargetCountKey = SemanticsPropertyKey<Int>("LegalTargetCount")

private var SemanticsPropertyReceiver.pegCountValue by PegCountKey
private var SemanticsPropertyReceiver.gameStatusValue by GameStatusKey
private var SemanticsPropertyReceiver.legalTargetCountValue by LegalTargetCountKey

/**
 * The stateless playable-board screen (Route/Screen/Leaf middle layer).
 *
 * Renders [state] and forwards events; owns no game logic (state hoisting).
 *
 * ## The chrome, after D-04/D-05
 *
 * There is **no Material top app bar** (criterion 16). An M3 [Scaffold] hosts a column of three
 * things: the [GameStatusStrip] (back, context title, live `mm:ss` timer — status only), the
 * centred `surfaceContainerHigh` board panel containing the [BoardCanvas], and the
 * [GameActionDock] (Undo · Hint · Show-moves · Restart) pinned below it in the thumb zone. The
 * board takes the space the two leave, so the dock is on screen at every viewport height and
 * font scale — a dock the player has to scroll to would be worse than the bar it replaced.
 *
 * **Each system bar is applied exactly once** (D-05d, ux-spec §4.3.1, criterion 18b): by the
 * `Scaffold`'s content padding, which this column applies. Neither the strip nor the dock applies
 * an inset of its own — both used to, which hung an empty status-bar-tall band above the strip and
 * took 72dp from the board on a 360x640dp phone.
 *
 * Nothing the player can *use* was removed: all five controls, the context-aware title, the
 * timer label and every handler signature this composable takes are the ones it had. Reaching a
 * terminal [GameStatus] no longer raises an in-screen dialog — the ViewModel emits a
 * one-shot finish event and [GameRoute] navigates to Results (D-05); only the
 * mid-game confirm [AlertDialog] (D-06) still overlays, when [GameUiState.confirmRestart].
 *
 * When the board is a rescuable no-win/stuck position ([GameUiState.showRescueBanner],
 * ASST-03/D-06/D-08) an inline, non-blocking [NoWinBanner] surfaces over the still-visible,
 * still-interactive board (Undo · Restart · Home, D-09) — a Stuck board is rescued via Undo
 * rather than auto-navigating; only a Won game routes to Results.
 *
 * When the last Hint search ended Undecided ([GameUiState.hintUndecided], GD-HT-1) the
 * [HintUndecidedNotice] takes the banner's slot instead — never the banner, which now means a
 * proven dead end. Its display time, its put-away and its "never twice" memory are the
 * screen's ([UndecidedNoticeSlot]); nothing about them is reported back to the ViewModel.
 *
 * The assist funnel reaches the player through exactly two surfaces here, both driven purely
 * from hoisted state: the modal [AdOfferDialog] whenever [GameUiState.pendingAdOffer] is set
 * (ADS-01) — modal is what makes "declining leaves the game exactly as it was" literally
 * true, because the board is inert behind the scrim — and the [ClaySnackbarHost] carrying the
 * D-11 "ad unavailable, refilled for free" message (ADS-03). The rescue banner deliberately
 * carries **no** ad affordance: its Undo routes into the same [onUndo] funnel that raises the
 * dialog at zero, which is UI-SPEC §4's "one allowance, one dialog, no second offer surface".
 *
 * Minimal test semantics ([PegCountKey]/[GameStatusKey]/[LegalTargetCountKey]) are
 * attached to the [GAME_STATE_TEST_TAG] node so the end-to-end flow test asserts on
 * state deterministically rather than on pixels.
 *
 * @param state the immutable UI state to render.
 * @param onCellTap forwards a tapped board [Position] to the ViewModel.
 * @param onRestart the dock's restart intent (the VM decides confirm vs. immediate reset).
 * @param onConfirmRestart confirms a restart (mid-game confirm dialog).
 * @param onDismissRestart dismisses the mid-game restart confirmation.
 * @param onAnimationFinished signals the jump slide completed.
 * @param onToggleHints toggles the persisted "Show possible moves" legal-destination dots.
 * @param onUndo undoes the last move (ASST-02); the control disables when nothing to undo.
 * @param onHint requests a winning-move hint (ASST-01); the control shows a thinking
 *   spinner + disables while [GameUiState.solverThinking] (D-10).
 * @param onAcceptAdOffer the player agreed to watch a rewarded ad for more of the offered
 *   assist (ADS-01); the dialog stays up, locked, while [GameUiState.adOfferLoading].
 * @param onDismissAdOffer the player declined the offer, tapped the scrim or pressed back —
 *   all three are the same event, and none of them charges or changes anything (D-06).
 * @param onHome leaves to the Home root from the rescue banner (D-09); wired to the same
 *   destination as the status strip's back control ([onNavigateBack]).
 * @param onNavigateBack leaves the gameplay screen (the status strip's back chip).
 * @param modifier layout modifier for the screen.
 * @param challengeLevel the Challenge level number when this is a Challenge game (drives
 *   the "Level N" strip title, S2/D-03), or `null` for Classic (app-name title). This
 *   is a pure presentation flag — it reaches only the title slot, never the board logic.
 *
 * The `LongMethod` suppression below rests on the list of every surface this composable hosts,
 * the last paragraph of this KDoc, and that list is the point of it: the screen must not silently
 * absorb another surface without the reader of that list finding out. Amend the list, one line
 * per surface, rather than growing under it, and never add a second suppression or a detekt
 * baseline entry in its place.
 *
 * Surfaces hosted, one per line:
 * - the [Scaffold];
 * - the status strip, [GameStatusStrip];
 * - the snackbar host, [ClaySnackbarHost];
 * - the board panel, [BoardCanvas], with the wrong-tap answer and its live region (D-06, D-06c);
 * - the Undecided notice, [UndecidedNoticeSlot];
 * - the action dock, [GameActionDock];
 * - the restart confirmation, `RestartConfirmDialog`;
 * - the ad offer, [AdOfferDialog].
 */
@Suppress("LongMethod") // one screen hosts every surface its KDoc lists
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    state: GameUiState,
    onCellTap: (Position) -> Unit,
    onRestart: () -> Unit,
    onConfirmRestart: () -> Unit,
    onDismissRestart: () -> Unit,
    onAnimationFinished: () -> Unit,
    onToggleHints: () -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onAcceptAdOffer: () -> Unit,
    onDismissAdOffer: () -> Unit,
    onHome: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    challengeLevel: Int? = null,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val wrongTapAnnouncement = rememberWrongTapAnnouncement(state)

    JumpHapticEffect(animatingMoveId = state.animating?.id, hapticsOn = state.hapticsOn)
    JumpSoundEffect(transition = state.animating)
    SelectionSound(selectionTick = state.selectionTick)
    DeselectionSound(deselectionTick = state.deselectionTick)
    UndoSound(undoTick = state.undoTick)
    InvalidTapSound(rejectedTapTick = state.rejectedTapTick)
    WrongTapHaptic(rejectedTapTick = state.rejectedTapTick, hapticsOn = state.hapticsOn)
    HintSound(hintSource = state.hintSource)
    WinSound(status = state.status)
    AdMessageSnackbarEffect(
        adMessage = state.adMessage,
        adMessageTick = state.adMessageTick,
        hostState = snackbarHostState,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { ClaySnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
        ) {
            GameStatusStrip(
                elapsedSeconds = state.elapsedSeconds,
                challengeLevel = challengeLevel,
                onNavigateBack = onNavigateBack,
            )
            Box(
                modifier =
                    Modifier
                        // The board takes whatever the strip and the dock leave, so both stay
                        // on screen at every viewport height — the dock in particular, which
                        // is the whole point of moving the actions into the thumb zone.
                        .weight(BOARD_WEIGHT)
                        .fillMaxWidth()
                        .padding(dimensionResource(R.dimen.board_screen_margin))
                        .testTag(GAME_STATE_TEST_TAG)
                        .semantics {
                            pegCountValue = state.board.pegCount
                            gameStatusValue = state.status.name
                            legalTargetCountValue = state.legalTargets.size
                        },
                contentAlignment = Alignment.Center,
            ) {
                if (state.isLoading) {
                    LoadingIndicator()
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier =
                            Modifier
                                .clayShadow(
                                    dimensionResource(R.dimen.elevation_card),
                                    MaterialTheme.shapes.extraLarge,
                                ).widthIn(max = dimensionResource(R.dimen.board_max_size))
                                .fillMaxWidth()
                                .aspectRatio(BoardMetrics.BOARD_ASPECT_RATIO)
                                .testTag(GAME_BOARD_SLAB_TEST_TAG),
                    ) {
                        BoardCanvas(
                            state = state,
                            onCellTap = onCellTap,
                            onAnimationFinished = onAnimationFinished,
                            modifier =
                                Modifier
                                    .padding(dimensionResource(R.dimen.spacing_md))
                                    // D-06c: the board itself is the polite live region; no new node.
                                    .wrongTapAnnouncement(wrongTapAnnouncement),
                            // D-06: the gameplay board corrects a wrong tap on the tapped cell.
                            wrongTapCorrection = true,
                        )
                    }
                }
                // The rescue banner overlays the still-interactive board (non-blocking, D-08):
                // it sits in the same Box so taps still reach the board beneath it. Only a
                // no-win/stuck position raises it; a Won game routes to Results instead.
                //
                // Its slot is shared with the Undecided notice (D-04f), one occupant at a time:
                // a proof outranks an Undecided, so the notice's slot is composed only while no
                // banner is up (criterion 23h). What the notice does once raised is its own.
                if (state.showRescueBanner) {
                    NoWinBanner(
                        message =
                            stringResource(
                                if (state.noWinningMove) R.string.nowin_message else R.string.stuck_message,
                            ),
                        onUndo = onUndo,
                        onRestart = onRestart,
                        onHome = onHome,
                        modifier =
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth(),
                    )
                } else {
                    UndecidedNoticeSlot(
                        raised = state.hintUndecided,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
            GameActionDock(
                showHints = state.showHints,
                canUndo = state.canUndo,
                solverThinking = state.solverThinking,
                onUndo = onUndo,
                onHint = onHint,
                onToggleHints = onToggleHints,
                onRestart = onRestart,
                modifier = Modifier.padding(dimensionResource(R.dimen.spacing_md)),
            )
        }
    }

    if (state.confirmRestart) {
        RestartConfirmDialog(
            onConfirm = onConfirmRestart,
            onDismiss = onDismissRestart,
        )
    }

    // ADS-01: the one and only offer surface, gated exactly like the confirm dialog above.
    // It is modal, so the board behind it is inert — that is what makes declining leave the
    // game literally untouched rather than merely intending to (T-09-39).
    state.pendingAdOffer?.let { reason ->
        AdOfferDialog(
            reason = reason,
            loading = state.adOfferLoading,
            onAccept = onAcceptAdOffer,
            onDismiss = onDismissAdOffer,
        )
    }
}

/**
 * Plays the pick-up cue whenever a peg is selected (SOUND-02).
 *
 * Selection only — putting a peg back down takes [DeselectionSound]. D-05a originally had
 * both halves of the gesture share this cue; that half was reversed on 2026-08-28 because the
 * two events sounded the same. What survives is that a deselect is a *successful* action, not
 * a rejection, and must never fire the invalid-tap cue.
 *
 * Keyed through [NewOrdinalEffect], the second half of the phantom-cue fix whose first half is
 * the ordinal carry-forward in `GameViewModel`. The carry-forward stops a state rebuild from
 * changing the key; [NewOrdinalEffect] never fires for the ordinal a composition first sees, so
 * neither a genuinely fresh state at 0 nor a screen recreated with a carried ordinal (a rotation)
 * plays a pick-up made before it (sound-designer F-1).
 */
@Composable
private fun SelectionSound(selectionTick: Int) {
    val sound = LocalSoundPlayer.current
    NewOrdinalEffect(selectionTick) { sound.play(SoundCue.Select) }
}

/**
 * Plays the put-down cue once per re-tap deselect (SOUND-02, D-05a as amended).
 *
 * [SoundCue.Deselect] is the [SoundCue.Select] pop reversed and pitched down a whole tone, so
 * the put-down reads as the same sound family as the pick-up without reading as the same
 * event. It is a successful action, so this is deliberately *not* the invalid-tap cue.
 * Keyed through [NewOrdinalEffect] as [SelectionSound] is.
 */
@Composable
private fun DeselectionSound(deselectionTick: Int) {
    val sound = LocalSoundPlayer.current
    NewOrdinalEffect(deselectionTick) { sound.play(SoundCue.Deselect) }
}

/**
 * Plays the undo cue once per undo that actually stepped the board back (SOUND-02).
 *
 * D-09 reads Undo as an in-game *assist* control, not a navigational CTA, so it keeps its own
 * distinct event cue rather than the button click. The ordinal only advances past both of
 * `onUndo`'s guards, so a declined grant or an empty history stays silent. Keyed through
 * [NewOrdinalEffect] as [SelectionSound] is.
 */
@Composable
private fun UndoSound(undoTick: Int) {
    val sound = LocalSoundPlayer.current
    NewOrdinalEffect(undoTick) { sound.play(SoundCue.Undo) }
}

/**
 * Plays the quiet invalid-tap cue on a tap that genuinely changed nothing (SOUND-02).
 *
 * Reserved for real rejections: an empty or out-of-play cell with no selection, or an illegal
 * destination while a peg is selected. A deselect is not one of those (D-05a) — it takes
 * [DeselectionSound]. Keyed through [NewOrdinalEffect] as [SelectionSound] is, like the wrong
 * tap's haptic and announcement, so after a rotation the cue never sounds alone for a tap made
 * before it.
 */
@Composable
private fun InvalidTapSound(rejectedTapTick: Int) {
    val sound = LocalSoundPlayer.current
    NewOrdinalEffect(rejectedTapTick) { sound.play(SoundCue.InvalidTap) }
}

/**
 * The wrong tap's reject haptic (D-06b, `docs/ux/invalid-tap-feedback.md` §3.4): one
 * [HapticFeedbackType.Reject] per wrong tap, at the tap — the platform's own `REJECT`, two short
 * pulses — and never on a selection, a put-down or a jump. Gated by [wrongTapHapticFires] on
 * `hapticsOn` and API 30, checked here in our own code; no ignore-global-setting flag, as for the
 * jump. Nothing else gates it: with Sound off or the ringer on Vibrate the tap still buzzes.
 *
 * Private to the gameplay screen, beside [InvalidTapSound], so the tutorial — which answers a
 * wrong tap with its redirect alone (D-01j(2), tech-lead Ruling 3) — cannot call it. Keyed through
 * [NewOrdinalEffect], as the tick cues are, so a screen recomposed from scratch with a carried
 * ordinal (a rotation) does not buzz for a tap made before it.
 */
@Composable
private fun WrongTapHaptic(
    rejectedTapTick: Int,
    hapticsOn: Boolean,
) {
    val haptics = LocalHapticFeedback.current
    NewOrdinalEffect(rejectedTapTick) {
        if (wrongTapHapticFires(hapticsOn, Build.VERSION.SDK_INT)) {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
        }
    }
}

/**
 * Plays the hint cue as a revealed move appears on the board (SOUND-02).
 *
 * Keyed on [GameUiState.hintSource], which is set only when the search resolves to a move and
 * is cleared by any jump or undo — so a new reveal is always a new key. Like Undo, Hint is an
 * assist control under D-09 and keeps its own event cue rather than the navigational click.
 */
@Composable
private fun HintSound(hintSource: Position?) {
    val sound = LocalSoundPlayer.current
    LaunchedEffect(hintSource) {
        if (hintSource != null) sound.play(SoundCue.Hint)
    }
}

/**
 * Plays the win stinger the moment the winning move lands on the board (SOUND-03, D-14).
 *
 * The first beat of D-14's two-beat celebration is deliberately here and not on Results: the
 * payoff belongs to the jump that earned it. Firing it from the game screen means it starts
 * while this composition is still alive, a frame or two before the navigation to Results —
 * and `SoundPool` playback is fire-and-forget, so the stinger survives that navigation and
 * rings out over the Results entrance, where the star pops become the second beat.
 */
@Composable
private fun WinSound(status: GameStatus) {
    val sound = LocalSoundPlayer.current
    LaunchedEffect(status) {
        if (status == GameStatus.Won) sound.play(SoundCue.Win)
    }
}

/**
 * Shows the one-shot "ad unavailable — refilled for free" message once per emission (ADS-03,
 * D-11), in the [ClaySnackbarHost] the screen's `Scaffold` hosts.
 *
 * It is the fifth instance of this file's `Int`-tick idiom and reads against
 * [SelectionSound]/[UndoSound] for exactly that reason. A tick rather than a one-shot
 * `SharedFlow` because it survives configuration change without replay bugs and does not add
 * a second eventing mechanism to a ViewModel that has none; and a tick rather than a bare
 * nullable id because the same message shown twice in a row leaves no distinguishable state
 * trace at all.
 *
 * The `> 0` guard is required, not defensive: it is the second half of the phantom-cue fix
 * whose first half is the ordinal carry-forward in `GameViewModel`. The carry-forward stops a
 * board install from changing the key; this guard stops a genuinely fresh state at 0 from
 * firing on first composition. Drop it and the player is told an ad was unavailable on the
 * screen-entry frame, for an ad they never asked for (T-09-40).
 *
 * [SnackbarDuration.Short] and no action label: by the time this shows, D-11 has already
 * refilled the assist, so there is nothing left for the player to do.
 *
 * The id is resolved with [stringResource] **in composition** rather than through
 * `LocalContext.current.getString(...)` inside the effect. `LocalContext` reads are not
 * invalidated by a `Configuration` change, so the latter can hand a stale-locale string to the
 * snackbar — the pinned Compose BOM fails the build on it (`LocalContextGetResourceValueCall`).
 * The effect still keys on the ordinal alone, so a locale change re-resolves the text without
 * re-showing a message the player has already been shown.
 *
 * @param adMessage the `@StringRes` id of the message to show, or `null` when none was raised.
 * @param adMessageTick the monotonic ordinal that makes each emission a distinct key.
 * @param hostState the snackbar queue hoisted by the screen's `Scaffold`.
 */
@Composable
private fun AdMessageSnackbarEffect(
    @StringRes adMessage: Int?,
    adMessageTick: Int,
    hostState: SnackbarHostState,
) {
    val message = adMessage?.let { stringResource(it) }
    LaunchedEffect(adMessageTick) {
        if (adMessageTick > 0 && message != null) {
            hostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }
}

/**
 * The centered progress indicator shown while a Challenge board is generated off the
 * main thread (CR-01). Carries the `loading_level` content description so screen
 * readers announce the wait; it replaces the board panel until the state settles.
 */
@Composable
private fun LoadingIndicator() {
    val description = stringResource(R.string.loading_level)
    CircularProgressIndicator(
        modifier = Modifier.semantics { contentDescription = description },
    )
}

/**
 * The mid-game restart confirmation (D-06): confirm discards progress, dismiss keeps
 * playing. A chunky extraLarge clay dialog whose confirm action rides the CtaGradient
 * (the one accent) over a quiet "Keep Playing" clay chip. The action labels are the
 * unchanged `restart_board` / `keep_playing` copy the flow test drives.
 *
 * It is also the only control pair in the app that voices both button cues (D-11): confirm is a
 * forward/commit action, dismiss backs out, and the two must be audibly different so the player
 * can hear which way they went without reading the label.
 */
@Composable
private fun RestartConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shape = MaterialTheme.shapes.extraLarge
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = shape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = dimensionResource(R.dimen.elevation_dialog),
        title = {
            Text(
                text = stringResource(R.string.restart_confirm_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.restart_confirm_body),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            val buttonShape = MaterialTheme.shapes.medium
            // SOUND-04 / D-09 / D-11: the dialog is the one place both button cues meet — confirm
            // commits (bright forward click), dismiss backs out (the softer, darker one). The cues
            // are attached per callsite and NEVER to Modifier.claySquish(), which is also on this
            // screen's back nav, hint toggle and Restart control — all of which D-09 keeps silent,
            // and Undo/Hint already carry their own SOUND-02 event cues. Exclusion list: the
            // withClickSound KDoc.
            TextButton(
                onClick = withClickSound(SoundCue.ButtonForward, onConfirm),
                shape = buttonShape,
                modifier =
                    Modifier
                        .claySquish()
                        .background(brush = ctaGradientBrush(), shape = buttonShape),
            ) {
                Text(
                    text = stringResource(R.string.restart_board),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = withClickSound(SoundCue.ButtonDismiss, onDismiss),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.claySquish(),
            ) {
                Text(
                    text = stringResource(R.string.keep_playing),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    )
}

/** The board takes the height the status strip and the action dock leave between them. */
private const val BOARD_WEIGHT = 1f
