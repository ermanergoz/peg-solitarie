package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.components.BoardCanvas
import com.app.pegsolitaire.ui.game.GameUiState

/*
 * Home's decorative board art (ux-spec §4.2, D-03): the hero's peg cluster, the Classic mode
 * tile's footprint, and the Classic Continue chip's thumbnail of the saved board.
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
 * none), because the band, chip or tile around it is already labelled and a screen reader
 * announcing a board footprint would add noise, not information (§11 "Labels").
 */

/**
 * The hero's static peg cluster (**Q-901 option 1**, the founder's answer).
 *
 * Five candy pegs in a plus, drawn at rest: no drifting, no pulse and no ambient motion of its
 * own, so the two existing `HomeBlobs` remain the only moving things on the screen and nothing
 * runs an animation on the app's first frame (D-03g). Option 2's drifting mini-board was
 * rejected for exactly that; option 3 — the gradient title alone — is step 3 of §6.5's
 * degradation path, so if the C1 measurement ever forces it, removing this one call is the
 * whole change.
 *
 * It is deliberately **not** a Classic footprint: the hero is ornament, and reusing a real
 * board here would imply it is the board the player is about to enter.
 */
@Composable
internal fun HomeHeroArt(modifier: Modifier = Modifier) {
    StaticBoardArt(board = HERO_CLUSTER, modifier = modifier)
}

/**
 * A static board footprint, rendered read-only.
 *
 * `interactive = false` suppresses tap emission, the selection ring, the legal-destination
 * dots and every animation (`BoardCanvas`' catalogue-preview mode), so this is a pure draw:
 * the resting pegs and the recessed empty holes, and nothing that could react to a touch the
 * parent surface owns.
 *
 * @param board the footprint to draw — the hero cluster, a mode tile's board, or the exact
 *   board a saved Classic game sits on (carried in `HomeUiState`, never looked up here).
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

/**
 * The hero cluster's five in-play cells, a plus inside a 3x3 box.
 *
 * `target = null`: this spec is never played, solved or scored — it exists only to give
 * `BoardGeometry` a footprint to lay out, and a target would imply a puzzle.
 */
private val HERO_CLUSTER_SPEC =
    BoardSpec(
        inPlay =
            listOf(
                Position(row = 0, col = 1),
                Position(row = 1, col = 0),
                Position(row = 1, col = 1),
                Position(row = 1, col = 2),
                Position(row = 2, col = 1),
            ),
        target = null,
    )

/** All five cells hold a peg, so the hero reads as a cluster of pegs rather than a puzzle. */
private val HERO_CLUSTER: Board =
    HERO_CLUSTER_SPEC.inPlay.fold(Board(spec = HERO_CLUSTER_SPEC, pegs = 0L)) { board, position ->
        board.withPeg(position)
    }
