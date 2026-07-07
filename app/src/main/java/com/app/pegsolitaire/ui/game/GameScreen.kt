package com.app.pegsolitaire.ui.game

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.GameStatus
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.components.BoardCanvas

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
 * @param onNavigateBack leaves the gameplay screen (top-bar back nav).
 * @param modifier layout modifier for the screen.
 * @param challengeLevel the Challenge level number when this is a Challenge game (drives
 *   the "Level N" top-bar title, S2/D-03), or `null` for Classic (app-name title). This
 *   is a pure presentation flag — it reaches only the title slot, never the board logic.
 */
@Suppress("LongMethod") // Scaffold + top-bar + board panel + confirm dialog; the Hint action is one line over.
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
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    challengeLevel: Int? = null,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier =
                        Modifier
                            .widthIn(max = dimensionResource(R.dimen.board_max_size))
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
        }
    }

    if (state.confirmRestart) {
        RestartConfirmDialog(
            onConfirm = onConfirmRestart,
            onDismiss = onDismissRestart,
        )
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
            IconButton(onClick = onNavigateBack) {
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
 * the value is announced as the elapsed time rather than a bare number.
 */
@Composable
private fun ElapsedTimeLabel(elapsedSeconds: Int) {
    val description = stringResource(R.string.elapsed_time)
    Text(
        text = GameTimerFormat.format(elapsedSeconds),
        style = MaterialTheme.typography.titleMedium,
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

/** The mid-game restart confirmation (D-06): confirm discards progress, dismiss keeps playing. */
@Composable
private fun RestartConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
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
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.restart_board),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.keep_playing),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    )
}
