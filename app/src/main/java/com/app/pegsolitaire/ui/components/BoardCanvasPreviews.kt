package com.app.pegsolitaire.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.ui.game.GameUiState
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.BoardMarkPalette

/*
 * Previews of the board's marks in both palettes, light and dark (rules/compose.md;
 * docs/ux/colour-vision.md §3.4, CV-AC15): English-33 mid-game with a held peg and its legal
 * targets, and Wiegleb-45 — the smallest cells — with a held peg and a revealed hint. A file of its
 * own so `BoardCanvas.kt` stays one render unit.
 */

@Preview(name = "English-33, standard · light", widthDp = BOARD_PREVIEW_DP, heightDp = BOARD_PREVIEW_DP)
@Preview(
    name = "English-33, standard · dark",
    widthDp = BOARD_PREVIEW_DP,
    heightDp = BOARD_PREVIEW_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun EnglishStandardPreview() {
    PaletteBoard(BoardMarkPalette.Standard, heldPegOn(Boards.english33Initial))
}

@Preview(name = "English-33, colour-blind · light", widthDp = BOARD_PREVIEW_DP, heightDp = BOARD_PREVIEW_DP)
@Preview(
    name = "English-33, colour-blind · dark",
    widthDp = BOARD_PREVIEW_DP,
    heightDp = BOARD_PREVIEW_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun EnglishColorblindPreview() {
    PaletteBoard(BoardMarkPalette.Colorblind, heldPegOn(Boards.english33Initial))
}

@Preview(name = "Wiegleb-45, standard · light", widthDp = BOARD_PREVIEW_DP, heightDp = BOARD_PREVIEW_DP)
@Preview(
    name = "Wiegleb-45, standard · dark",
    widthDp = BOARD_PREVIEW_DP,
    heightDp = BOARD_PREVIEW_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun WieglebStandardPreview() {
    PaletteBoard(BoardMarkPalette.Standard, heldPegAndHintOn(Boards.wiegleb45Initial))
}

@Preview(name = "Wiegleb-45, colour-blind · light", widthDp = BOARD_PREVIEW_DP, heightDp = BOARD_PREVIEW_DP)
@Preview(
    name = "Wiegleb-45, colour-blind · dark",
    widthDp = BOARD_PREVIEW_DP,
    heightDp = BOARD_PREVIEW_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun WieglebColorblindPreview() {
    PaletteBoard(BoardMarkPalette.Colorblind, heldPegAndHintOn(Boards.wiegleb45Initial))
}

/** The interactive board on its slab colour, in [palette]. */
@Composable
private fun PaletteBoard(
    palette: BoardMarkPalette,
    state: GameUiState,
) {
    AppTheme(boardMarkPalette = palette) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            BoardCanvas(
                state = state,
                onCellTap = {},
                onAnimationFinished = {},
                modifier = Modifier.padding(dimensionResource(R.dimen.spacing_md)),
            )
        }
    }
}

/** One opening move into [start], then a peg held with its legal targets showing. */
private fun heldPegOn(start: Board): GameUiState {
    val board = RulesEngine.applyMove(start, RulesEngine.legalMoves(start).first())
    val held = RulesEngine.legalMoves(board).first().from
    return GameUiState.initial(board).copy(
        selected = held,
        legalTargets = RulesEngine.legalMoves(board).filter { it.from == held }.map { it.to },
        showHints = true,
    )
}

/** [heldPegOn], plus a revealed hint on the last legal move that leaves from another peg. */
private fun heldPegAndHintOn(start: Board): GameUiState {
    val held = heldPegOn(start)
    val hint = RulesEngine.legalMoves(held.board).last { it.from != held.selected }
    return held.copy(hintSource = hint.from, hintTarget = hint.to)
}

/** A square preview, the board's own aspect. */
private const val BOARD_PREVIEW_DP = 360
