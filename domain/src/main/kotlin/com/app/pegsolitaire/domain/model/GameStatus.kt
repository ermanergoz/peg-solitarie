package com.app.pegsolitaire.domain.model

/**
 * Lifecycle status of a game.
 *
 * - [Playing]: at least one legal move remains.
 * - [Won]: the board is solved (a single peg remains).
 * - [Stuck]: no legal moves remain but more than one peg is left.
 *
 * Win/lose copy tiers (D-05) live in the UI layer; the domain only reports status.
 */
enum class GameStatus {
    Playing,
    Won,
    Stuck,
}
