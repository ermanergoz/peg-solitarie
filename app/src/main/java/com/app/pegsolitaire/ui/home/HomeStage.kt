package com.app.pegsolitaire.ui.home

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.game.GameUiState

/*
 * Home's stage (ux-spec §4.2.4, D-03l): the decoration that fills the slack beneath the hero row
 * with a clay board showing a game in progress.
 */

/**
 * The board state the stage draws: English-33 in mid-game, 14 pegs scattered over all four arms.
 *
 * It is a **constant**, never the player's saved board or anything read at start-up, so it cannot
 * contradict a readout and costs Home's first frame nothing but one board draw (D-03g). It is
 * reachable from the English start by legal play — the first 18 jumps of a winning line — and
 * `HomeStagePositionTest` proves that by replay; nothing replays it here.
 *
 * `GameUiState.initial` gives it no selected peg, no legal targets, no hint and no jump in flight,
 * so nothing on it pulses, pops or slides on any frame (criterion 15q, §10).
 */
internal val HomeStageBoardState: GameUiState by lazy {
    GameUiState.initial(
        board =
            STAGE_PEGS.fold(Board(spec = Boards.English33, pegs = 0L)) { board, position ->
                board.withPeg(position)
            },
    )
}

/** The stage position's pegs, row by row. */
private val STAGE_PEGS: List<Position> =
    listOf(
        Position(row = 0, col = 4),
        Position(row = 1, col = 2),
        Position(row = 1, col = 3),
        Position(row = 2, col = 0),
        Position(row = 2, col = 1),
        Position(row = 2, col = 4),
        Position(row = 2, col = 5),
        Position(row = 2, col = 6),
        Position(row = 3, col = 6),
        Position(row = 4, col = 1),
        Position(row = 4, col = 2),
        Position(row = 4, col = 4),
        Position(row = 5, col = 4),
        Position(row = 6, col = 2),
    )
