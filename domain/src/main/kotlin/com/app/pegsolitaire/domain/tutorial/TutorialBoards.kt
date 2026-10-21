package com.app.pegsolitaire.domain.tutorial

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Position

/**
 * Scoped layout constants for the two tutorial footprints.
 *
 * No literal dimension leaks into the board declarations (QUAL-03), mirroring the
 * `English33Layout` / `European37Layout` precedent in the board model.
 */
private object TutorialLayout {
    /** T1's footprint is a single row of this many cells. */
    const val STRIP_LENGTH = 3

    /**
     * T2's and T3's shared footprint is a square lattice of this many rows and
     * columns — the centre block of the English cross, so the eye is prepared for
     * the catalogue silhouette without the tutorial inheriting catalogue geometry.
     */
    const val SQUARE_DIMENSION = 3

    /** The 1x3 strip: `(0,0) (0,1) (0,2)`. */
    val strip: List<Position> = List(STRIP_LENGTH) { col -> Position(row = 0, col = col) }

    /** The full 3x3 square: all nine cells in play. */
    val square: List<Position> =
        buildList {
            for (row in 0 until SQUARE_DIMENSION) {
                for (col in 0 until SQUARE_DIMENSION) {
                    add(Position(row = row, col = col))
                }
            }
        }
}

/**
 * The three bespoke teaching miniatures the tutorial runs, in play order
 * (ux-spec §4.1; board content ruled by
 * `.planning/reports/phase-15/game-designer-tutorial-boards.md`).
 *
 * These are **teaching miniatures, not Classic geometry**, and they must never be
 * added to the shipped Classic footprint catalogue in `Boards`: that list is the
 * Classic catalogue and the denominator of the Home "Boards" pill, so a teaching
 * board joining it would change a number the player reads on Home and would put
 * three miniatures on the catalogue screen. Nor could a Classic board serve in
 * their place: none of them is dead-end-free — the real game designs for the stuck
 * state and the rescue banner exists for it — and the smallest is 33 holes in a 7x7
 * box, which at the tutorial's 280dp board slab is 40dp per cell, below the spec's
 * own 48dp touch-target floor.
 *
 * Every one of the three is proven **dead-end-free to one peg** by
 * `TutorialBoardsTest.everyLegalLineOfEveryTutorialBoardEndsWithExactlyOnePeg`,
 * which enumerates every reachable position: that is what makes "a wrong tap is
 * never a failure" (D-01c) a property of the layouts rather than of a screen
 * refusing taps. All three carry no [BoardSpec.target] and the default one-peg
 * finish, so a step is complete on the same test for all three — the board holds
 * one peg.
 */
object TutorialBoards {
    /**
     * **T1 — tap a peg, tap the hole two cells away.** A 1x3 strip with pegs at
     * `(0,0)` and `(0,1)`: exactly one legal move, and it is horizontal, so the
     * only thing the player *can* do is the thing being taught.
     */
    val T1: Board =
        boardOf(
            inPlay = TutorialLayout.strip,
            pegs = listOf(Position(row = 0, col = 0), Position(row = 0, col = 1)),
        )

    /**
     * **T2 — the same jump works on the other axis.** The full 3x3 with pegs at
     * `(0,1)` and `(1,1)`: exactly one legal move, and it is vertical. The
     * footprint is the whole square rather than a plus so the four diagonal holes
     * and the two horizontally-adjacent ones are all visibly in play and all
     * visibly receive no destination dot — which is what earns the step's "never
     * diagonally" caption.
     */
    val T2: Board =
        boardOf(
            inPlay = TutorialLayout.square,
            pegs = listOf(Position(row = 0, col = 1), Position(row = 1, col = 1)),
        )

    /**
     * **T3 — the goal is to leave one peg.** T2's footprint, now filled to five
     * pegs: three legal openings, four maximal lines, every one of them four moves
     * long and every one of them ending on one peg in the same cell. The player
     * learns that their own choices are safe.
     */
    val T3: Board =
        boardOf(
            inPlay = TutorialLayout.square,
            pegs =
                listOf(
                    Position(row = 0, col = 0),
                    Position(row = 0, col = 1),
                    Position(row = 1, col = 0),
                    Position(row = 1, col = 1),
                    Position(row = 1, col = 2),
                ),
        )

    /**
     * The three starting boards in the order the tutorial plays them — T1, T2, T3.
     * The step machine indexes this list, so the order is contract.
     */
    val ALL: List<Board> = listOf(T1, T2, T3)
}

/** A starting board over [inPlay] with a peg on each of [pegs] and no target hole. */
private fun boardOf(
    inPlay: List<Position>,
    pegs: List<Position>,
): Board {
    val spec = BoardSpec(inPlay = inPlay, target = null)
    return pegs.fold(Board(spec = spec, pegs = 0L)) { board, position -> board.withPeg(position) }
}
