// The screen and its private, single-purpose parts: the top bar, its four labels/indicators,
// the confirm dialog, and the nine one-shot feedback effects (one haptic + seven sound cues +
// the ad-message snackbar). Each is a handful of lines and each exists only for this screen;
// splitting them across files to satisfy a per-file count would scatter the screen's own
// composition for no reader's benefit. The cue effects in particular are deliberately adjacent
// to JumpHaptic — they are clones of its idiom and are read against it.
@file:Suppress("TooManyFunctions")

package com.app.pegsolitaire.ui.game

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.app.pegsolitaire.audio.LocalSoundPlayer
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.audio.SoundParams
import com.app.pegsolitaire.audio.withClickSound
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.components.BoardCanvas
import com.app.pegsolitaire.ui.components.ClaySnackbarHost
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish
import com.app.pegsolitaire.ui.theme.ctaGradientBrush
import com.app.pegsolitaire.ui.theme.withTabularFigures
import kotlinx.coroutines.delay

/** Test tag of the board-state node carrying the [PegCountKey]/[GameStatusKey]/[LegalTargetCountKey] semantics. */
const val GAME_STATE_TEST_TAG = "game_state"

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
 * Renders [state] and forwards events; owns no game logic (state hoisting). An M3
 * [Scaffold] hosts a [TopAppBar] (a back nav, the title, the live `mm:ss` elapsed
 * timer D-04, and the [RestartControl] D-06 action) over a centered
 * `surfaceContainerHigh` board panel containing the [BoardCanvas]. Reaching a
 * terminal [GameStatus] no longer raises an in-screen dialog — the ViewModel emits a
 * one-shot finish event and [GameRoute] navigates to Results (D-05); only the
 * mid-game confirm [AlertDialog] (D-06) still overlays, when [GameUiState.confirmRestart].
 *
 * When the board is a rescuable no-win/stuck position ([GameUiState.showRescueBanner],
 * ASST-03/D-06/D-08) an inline, non-blocking [NoWinBanner] surfaces over the still-visible,
 * still-interactive board (Undo · Restart · Home, D-09) — a Stuck board is rescued via Undo
 * rather than auto-navigating; only a Won game routes to Results.
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
 * @param onRestart top-bar restart intent (VM decides confirm vs. immediate reset).
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
 *   destination as the top-bar back nav ([onNavigateBack]).
 * @param onNavigateBack leaves the gameplay screen (top-bar back nav).
 * @param modifier layout modifier for the screen.
 * @param challengeLevel the Challenge level number when this is a Challenge game (drives
 *   the "Level N" top-bar title, S2/D-03), or `null` for Classic (app-name title). This
 *   is a pure presentation flag — it reaches only the title slot, never the board logic.
 *
 * The `LongMethod` suppression below carries an inline list of every surface this composable
 * hosts, and that list is the point of it: the screen must not silently absorb another surface
 * without the reader of that line finding out. Amend the list rather than growing under it,
 * and never add a second suppression or a detekt baseline entry in its place.
 */
@Suppress("LongMethod") // Scaffold + top bar + snackbar host + board panel + confirm and ad-offer dialogs.
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

    JumpHaptic(animatingMoveId = state.animating?.id, hapticsOn = state.hapticsOn)
    JumpSound(transition = state.animating)
    SelectionSound(selectionTick = state.selectionTick)
    DeselectionSound(deselectionTick = state.deselectionTick)
    UndoSound(undoTick = state.undoTick)
    InvalidTapSound(rejectedTapTick = state.rejectedTapTick)
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
        topBar = {
            GameTopBar(
                elapsedSeconds = state.elapsedSeconds,
                showHints = state.showHints,
                canUndo = state.canUndo,
                solverThinking = state.solverThinking,
                challengeLevel = challengeLevel,
                onRestart = onRestart,
                onToggleHints = onToggleHints,
                onUndo = onUndo,
                onHint = onHint,
                onNavigateBack = onNavigateBack,
            )
        },
    ) { contentPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
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
                            .aspectRatio(BoardMetrics.BOARD_ASPECT_RATIO),
                ) {
                    BoardCanvas(
                        state = state,
                        onCellTap = onCellTap,
                        onAnimationFinished = onAnimationFinished,
                        modifier = Modifier.padding(dimensionResource(R.dimen.spacing_md)),
                    )
                }
            }
            // The rescue banner overlays the still-interactive board (non-blocking, D-08):
            // it sits in the same Box so taps still reach the board beneath it. Only a
            // no-win/stuck position raises it; a Won game routes to Results instead.
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
            }
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
 * Fires one light haptic tick as each jump animation begins (SET-02 / D-15).
 *
 * Haptic emission is a Compose UI concern, so it lives here rather than in the ViewModel:
 * the screen reads the already-collected [GameUiState.hapticsOn] gate (it never touches
 * `SettingsRepository`) and the effect is keyed on the [MoveTransition.id] move ordinal —
 * so it fires exactly once per jump, not once per recomposition. When the setting is off
 * nothing fires and gameplay is otherwise identical.
 *
 * [HapticFeedbackType.ContextClick] is the lightest type in the pinned Compose BOM that is
 * available across the whole supported range (`CONTEXT_CLICK`, API 23 ≤ minSdk 24) — the
 * newer `SegmentTick`/`GestureEnd` constants map to API-34-only platform constants and
 * would be silent on most devices, and `LongPress` is far too heavy for a peg hop.
 *
 * @param animatingMoveId the in-flight jump's move ordinal, or `null` when settled.
 * @param hapticsOn the persisted haptics gate carried by the UI state.
 */
@Composable
private fun JumpHaptic(
    animatingMoveId: Int?,
    hapticsOn: Boolean,
) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(animatingMoveId) {
        if (animatingMoveId != null && hapticsOn) {
            haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
        }
    }
}

/**
 * Plays the pitched jump cue as each jump lands, then layers the peg-removal cue under it
 * (SOUND-01/SOUND-02).
 *
 * Keyed on [MoveTransition.id] — the same move ordinal [JumpHaptic] uses — so it fires
 * exactly once per jump rather than once per recomposition. The pitch comes from
 * [MoveTransition.runIndex], the same-peg ladder step (D-05/D-06); because that index is
 * derived from the move history rather than stored, undoing and replaying a jump replays
 * its original pitch (D-08) with nothing to keep in sync here.
 *
 * Every jump removes exactly one peg, so the two cues always co-occur; the removal trails
 * by [SoundParams.REMOVAL_LAYER_DELAY_MS] so it reads as a texture layer beneath the pitched
 * pop instead of a second competing hit.
 *
 * **The seven sound cues take no enable/disable gate parameter, and that asymmetry with
 * [JumpHaptic] is deliberate.** `hapticsOn` is carried in [GameUiState] because a haptic can
 * only ever be blocked *before* it fires. Audio cannot: SOUND-05 requires silencing one-shots
 * that are already in flight, which only the object holding the live stream ids can do. The
 * audio gate therefore lives inside the player implementation, which subscribes to the
 * persisted setting itself. Do not "fix" this by copying the `hapticsOn` plumbing — a UI-state
 * flag can block future calls and nothing else, and adding one would give the mute two
 * disagreeing sources of truth.
 */
@Composable
private fun JumpSound(transition: MoveTransition?) {
    val sound = LocalSoundPlayer.current
    LaunchedEffect(transition?.id) {
        if (transition != null) {
            sound.playJump(runIndex = transition.runIndex)
            delay(SoundParams.REMOVAL_LAYER_DELAY_MS.toLong())
            sound.play(SoundCue.PegRemove)
        }
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
 * The `> 0` guard is required, not defensive: it is the second half of the phantom-cue fix
 * whose first half is the ordinal carry-forward in `GameViewModel`. The carry-forward stops a
 * state rebuild from changing the key; this guard stops a genuinely fresh state at 0 from
 * firing on first composition.
 */
@Composable
private fun SelectionSound(selectionTick: Int) {
    val sound = LocalSoundPlayer.current
    LaunchedEffect(selectionTick) {
        if (selectionTick > 0) sound.play(SoundCue.Select)
    }
}

/**
 * Plays the put-down cue once per re-tap deselect (SOUND-02, D-05a as amended).
 *
 * [SoundCue.Deselect] is the [SoundCue.Select] pop reversed and pitched down a whole tone, so
 * the put-down reads as the same sound family as the pick-up without reading as the same
 * event. It is a successful action, so this is deliberately *not* the invalid-tap cue.
 * `> 0` guard as in [SelectionSound].
 */
@Composable
private fun DeselectionSound(deselectionTick: Int) {
    val sound = LocalSoundPlayer.current
    LaunchedEffect(deselectionTick) {
        if (deselectionTick > 0) sound.play(SoundCue.Deselect)
    }
}

/**
 * Plays the undo cue once per undo that actually stepped the board back (SOUND-02).
 *
 * D-09 reads Undo as an in-game *assist* control, not a navigational CTA, so it keeps its own
 * distinct event cue rather than the button click. The ordinal only advances past both of
 * `onUndo`'s guards, so a declined grant or an empty history stays silent. `> 0` guard as in
 * [SelectionSound].
 */
@Composable
private fun UndoSound(undoTick: Int) {
    val sound = LocalSoundPlayer.current
    LaunchedEffect(undoTick) {
        if (undoTick > 0) sound.play(SoundCue.Undo)
    }
}

/**
 * Plays the quiet invalid-tap cue on a tap that genuinely changed nothing (SOUND-02).
 *
 * Reserved for real rejections: an empty or out-of-play cell with no selection, or an illegal
 * destination while a peg is selected. A deselect is not one of those (D-05a) — it takes
 * [DeselectionSound]. `> 0` guard as in [SelectionSound].
 */
@Composable
private fun InvalidTapSound(rejectedTapTick: Int) {
    val sound = LocalSoundPlayer.current
    LaunchedEffect(rejectedTapTick) {
        if (rejectedTapTick > 0) sound.play(SoundCue.InvalidTap)
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
 * The gameplay top bar: a back nav, the context-aware title, the live `mm:ss` elapsed
 * timer (D-04), the [UndoControl] (ASST-02), the [HintControl] (ASST-01), the
 * [HintToggleControl], and the [RestartControl] (D-06). Stateless — every value/handler
 * is hoisted. The title slot is the only mode-aware element (S2): a non-null
 * [challengeLevel] renders the "Level N" indicator, else the app name; the actions row is
 * identical in both modes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GameTopBar(
    elapsedSeconds: Int,
    showHints: Boolean,
    canUndo: Boolean,
    solverThinking: Boolean,
    challengeLevel: Int?,
    onRestart: () -> Unit,
    onToggleHints: () -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    TopAppBar(
        navigationIcon = {
            IconButton(
                onClick = onNavigateBack,
                modifier =
                    Modifier
                        .claySquish()
                        .size(dimensionResource(R.dimen.top_bar_touch_target)),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back),
                    modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
                )
            }
        },
        title = { GameTopBarTitle(challengeLevel = challengeLevel) },
        actions = {
            ElapsedTimeLabel(elapsedSeconds = elapsedSeconds)
            UndoControl(canUndo = canUndo, onUndo = onUndo)
            HintControl(solverThinking = solverThinking, onHint = onHint)
            HintToggleControl(showHints = showHints, onToggleHints = onToggleHints)
            RestartControl(onRestart = onRestart)
        },
    )
}

/**
 * The context-aware gameplay title (S2 / D-03). In Challenge mode ([challengeLevel] set)
 * it renders the neutral "Level N" indicator (`titleLarge`, `onSurface` — deliberately
 * NOT accent per UI-SPEC S3) with a `challenge_level_indicator_desc` screen-reader
 * description; otherwise it renders the static app-name title. This is the sole
 * presentation delta — no `if (challenge)` reaches the board/selection/jump logic.
 */
@Composable
private fun GameTopBarTitle(challengeLevel: Int?) {
    if (challengeLevel != null) {
        val description = stringResource(R.string.challenge_level_indicator_desc, challengeLevel)
        Text(
            text = stringResource(R.string.challenge_level_indicator, challengeLevel),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { contentDescription = description },
        )
    } else {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

/**
 * The live `mm:ss` elapsed timer shown in the gameplay top bar (D-04).
 *
 * The zero-padded time is a computed clock string ([GameTimerFormat]), not a string
 * resource; the `elapsed_time` content description is attached for screen readers so
 * the value is announced as the elapsed time rather than a bare number. It uses
 * tabular figures so ticking digits never shift the label horizontally.
 */
@Composable
private fun ElapsedTimeLabel(elapsedSeconds: Int) {
    val description = stringResource(R.string.elapsed_time)
    Text(
        text = GameTimerFormat.format(elapsedSeconds),
        style = MaterialTheme.typography.titleMedium.withTabularFigures(),
        color = MaterialTheme.colorScheme.onSurface,
        modifier =
            Modifier
                .padding(end = dimensionResource(R.dimen.spacing_sm))
                .semantics { contentDescription = description },
    )
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
