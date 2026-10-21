package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.ui.components.BoardCanvas
import com.app.pegsolitaire.ui.game.GameUiState

/*
 * Home's small decorative board art (ux-spec §4.2, D-03): the Classic mode tile's footprint and
 * the Classic Continue chip's thumbnail of the saved board. The hero's peg cluster that used to
 * live here is withdrawn (D-03l); the board the slack now holds is the stage, in `HomeStage.kt`.
 *
 * Why it is `BoardCanvas` and not an asset. Every one of these is the shipped board renderer in
 * `interactive = false` mode, which is what `BoardPreview` already does for the catalogue cards.
 * That keeps them VECTOR GEOMETRY WITH NO IMAGE DECODE, no bitmap and no asset load on the
 * cold-start critical path — the hard rule §6.5 sets for Home's first frame, and the reason none
 * of them needed a drawable. They are also pegs drawn by the same code the player will tap, so
 * the art cannot drift from the game's own look the way a hand-drawn illustration would.
 *
 * Why it is a file of its own. `ModeGlyphs.kt` owns Home's `ImageVector` glyphs — the 2x2 grid
 * and the lightning bolt — and these are not glyphs: they are board states rendered through the
 * canvas. Keeping the two kinds apart is what stops either file becoming "Home's pictures".
 *
 * Everything here is DECORATIVE: each carries no content description (`BoardCanvas` attaches
 * none), because the tile or chip around it is already labelled and a screen reader announcing a
 * board footprint would add noise, not information (§11 "Labels").
 */

/**
 * A static board footprint, rendered read-only.
 *
 * `interactive = false` suppresses tap emission, the selection ring, the legal-destination
 * dots and every animation (`BoardCanvas`' catalogue-preview mode), so this is a pure draw:
 * the resting pegs and the recessed empty holes, and nothing that could react to a touch the
 * parent surface owns.
 *
 * @param board the footprint to draw — a mode tile's board, or the exact board a saved Classic
 *   game sits on (carried in `HomeUiState`, never looked up here).
 * @param modifier layout modifier; the caller sizes it from a Home art token.
 */
@Composable
internal fun StaticBoardArt(
    board: Board,
    modifier: Modifier = Modifier,
) {
    BoardCanvas(
        state = GameUiState.initial(board = board),
        onCellTap = {},
        onAnimationFinished = {},
        modifier = modifier,
        interactive = false,
    )
}

/** The Classic mode tile's trailing art: the smallest shipped footprint, at tile scale. */
internal val ClassicTileArtBoard: Board get() = Boards.english33Initial
