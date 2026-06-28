package com.app.pegsolitaire.domain.model

/**
 * A single peg-solitaire jump: the peg at [from] jumps over the peg at [over]
 * and lands in the empty hole at [to].
 *
 * By construction (see `RulesEngine.legalMoves`) the three positions are
 * colinear, orthogonal, and evenly spaced (spacing 2). The domain only ever
 * produces [Move]s that are members of the legal-move set for a given board.
 */
data class Move(
    val from: Position,
    val over: Position,
    val to: Position,
)
