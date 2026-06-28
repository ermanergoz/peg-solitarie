package com.app.pegsolitaire.ui.game

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
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
 * [Scaffold] hosts a [TopAppBar] (title + the [RestartControl] D-06 action) over a
 * centered `surfaceContainerHigh` board panel containing the [BoardCanvas]. The
 * [GameOverDialog] (D-05) overlays on a terminal [GameStatus]; the mid-game confirm
 * [AlertDialog] (D-06) overlays when [GameUiState.confirmRestart] is set.
 *
 * Minimal test semantics ([PegCountKey]/[GameStatusKey]/[LegalTargetCountKey]) are
 * attached to the [GAME_STATE_TEST_TAG] node so the end-to-end flow test asserts on
 * state deterministically rather than on pixels.
 *
 * @param state the immutable UI state to render.
 * @param onCellTap forwards a tapped board [Position] to the ViewModel.
 * @param onRestart top-bar restart intent (VM decides confirm vs. immediate reset).
 * @param onConfirmRestart confirms a restart (mid-game confirm or game-over "Play Again").
 * @param onDismissRestart dismisses the mid-game restart confirmation.
 * @param onAnimationFinished signals the jump slide completed.
 * @param modifier layout modifier for the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    state: GameUiState,
    onCellTap: (Position) -> Unit,
    onRestart: () -> Unit,
    onConfirmRestart: () -> Unit,
    onDismissRestart: () -> Unit,
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = { RestartControl(onRestart = onRestart) },
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

    GameOverOverlay(
        state = state,
        onPlayAgain = onConfirmRestart,
    )

    if (state.confirmRestart) {
        RestartConfirmDialog(
            onConfirm = onConfirmRestart,
            onDismiss = onDismissRestart,
        )
    }
}

/**
 * Shows the [GameOverDialog] on a terminal status, with a transient "View Board"
 * dismissal. Game-over is replayable [GameStatus] (it survives rotation), but the
 * player may hide the overlay to inspect the final board; that hide is ephemeral
 * view state, reset whenever the game identity ([GameUiState.status]/board) changes.
 */
@Composable
private fun GameOverOverlay(
    state: GameUiState,
    onPlayAgain: () -> Unit,
) {
    val isGameOver = state.status == GameStatus.Won || state.status == GameStatus.Stuck
    var dismissed by remember(state.status, state.board) { mutableStateOf(false) }
    if (isGameOver && !dismissed) {
        GameOverDialog(
            status = state.status,
            pegCount = state.board.pegCount,
            onPlayAgain = onPlayAgain,
            onViewBoard = { dismissed = true },
        )
    }
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
