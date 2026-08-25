package com.app.pegsolitaire.ui.game

import androidx.compose.runtime.Immutable
import com.app.pegsolitaire.domain.model.Position

/**
 * A declarative description of a single jump animation (D-07 / D-08).
 *
 * The ViewModel sets this AFTER the move has already been committed to domain
 * state (animate-from-committed-state, RESEARCH Q5), so the [Board] in
 * [GameUiState] is always the post-move board and this object is a pure render
 * instruction for the Canvas:
 *  - the jumping peg slides [from] → [to] (straight line, D-07);
 *  - the jumped-over peg at [removed] fades + shrinks out (D-08).
 *
 * [id] is the move ordinal (`moveHistory.size`); it is the animation key — a new
 * move produces a new [id], cleanly restarting the `Animatable`s.
 */
@Immutable
data class MoveTransition(
    val from: Position,
    val to: Position,
    val removed: Position,
    val id: Int,
)
