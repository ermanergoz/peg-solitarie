package com.app.pegsolitaire.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.ui.components.BoardCanvas
import com.app.pegsolitaire.ui.game.GameUiState

/*
 * Home's small decorative board art (ux-spec §4.2, D-03): the Classic mode tile's footprint and
 * the Classic Continue chip's thumbnail of the player's saved position (D-03p). The hero's peg
 * cluster that used to live here is withdrawn (D-03l), and so is the stage that held a constant
 * board in the slack (D-03p): the thumbnail is now the only board Home draws in a slab.
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

/** Semantics tag of the Classic Continue chip's thumbnail slab — its only semantics (§4.2.7, criterion 15ab). */
const val HOME_RESUME_THUMB_TEST_TAG = "home_resume_thumb"

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

/**
 * The Classic Continue chip's thumbnail (ux-spec §4.2.7, D-03p): the player's **saved position** — the
 * board as they left it, carried in `HomeUiState.classicResumeBoard` — drawn by [StaticBoardArt] on a
 * miniature board slab.
 *
 * The slab is a plain `surfaceContainerHigh` square of `home_resume_thumb_size` in `shapes.extraSmall`,
 * the colour the board's dimples are drawn for, and a board on a slab reads as a board. It takes **no
 * finish** — no glow, no gloss, no rim — because nothing glossy lies over a board (backdrop spec §3.5);
 * it is not a finished container, and BD-17's sample of its blocked corner proves it plain. The board is
 * inset `spacing_xs`, so it is 48dp on the 56dp slab: 6.9dp cells on a 7-wide board, a pattern of what is
 * left rather than a cell-by-cell read, which the chip's spoken state completes.
 *
 * Decorative: the slab carries a test tag and nothing else, and the read-only canvas attaches no
 * semantics, so it is no TalkBack stop and describes nothing (§11.1, D-TBa item 9). The chip it sits in
 * says the count instead. It has no motion of its own: it enters with its chip (§10).
 *
 * @param board the saved position to draw, exactly as `HomeUiState` carries it.
 * @param modifier layout modifier; the slab sets its own size.
 */
@Composable
internal fun ResumeThumbnail(
    board: Board,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(dimensionResource(R.dimen.home_resume_thumb_size))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.extraSmall)
                .testTag(HOME_RESUME_THUMB_TEST_TAG),
    ) {
        StaticBoardArt(
            board = board,
            modifier = Modifier.fillMaxSize().padding(dimensionResource(R.dimen.spacing_xs)),
        )
    }
}

/** The Classic mode tile's trailing art: the smallest shipped footprint, at tile scale. */
internal val ClassicTileArtBoard: Board get() = Boards.english33Initial
