package com.app.pegsolitaire.domain.model

/**
 * Lifecycle status of a game.
 *
 * - [Playing]: at least one legal move remains, so the game is not over (a finish cannot be
 *   banked while a jump is still possible).
 * - [Won]: no legal move remains and the board passed its mode's finish test. In Classic that
 *   is `pegCount <= BoardSpec.minimumPegs` — one peg on four boards, a proven two on
 *   Diamond-41. In Challenge it is `ChallengeClearRule`: one to three pegs, with three to one
 *   stars, all of which clear the level. The name is kept across both modes; in Challenge it
 *   reads "cleared".
 * - [Stuck]: no legal move remains and more pegs are left than the mode's finish test admits —
 *   above the board's floor in Classic, four or more in Challenge. Rescuable by undo or restart.
 *
 * Win/lose copy tiers (D-05) live in the UI layer; the domain only reports status.
 */
enum class GameStatus {
    Playing,
    Won,
    Stuck,
}
