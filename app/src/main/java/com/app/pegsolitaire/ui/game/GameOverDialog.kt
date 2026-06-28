package com.app.pegsolitaire.ui.game

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.GameStatus

/** Upper bound (inclusive) of the "so close" tier — at/above this is "no moves left". */
private const val NEAR_MISS_MAX_PEGS = 3

/**
 * The status-tiered game-over overlay (D-05).
 *
 * A Material 3 [AlertDialog] whose headline + body are chosen from the terminal
 * [status] and [pegCount] per the Copywriting Contract:
 *  - [GameStatus.Won] (a single peg) → "Perfect!".
 *  - [GameStatus.Stuck] with 2–3 pegs → "So close!" + pegs-remaining.
 *  - [GameStatus.Stuck] with 4+ pegs → "No moves left" + pegs-remaining + retry.
 *
 * The primary action "Play Again" ([onPlayAgain]) restarts with no extra confirm
 * (the player is already at an end-state); the neutral "View Board" ([onViewBoard])
 * hides the overlay so the final board can be inspected. All copy is a string
 * resource; the count is a plural-aware resource.
 *
 * @param status the terminal lifecycle status (must be [GameStatus.Won]/[GameStatus.Stuck]).
 * @param pegCount the pegs left on the board, selecting the tier + filling the copy.
 * @param onPlayAgain restarts the board (primary action).
 * @param onViewBoard hides the overlay without resetting (dismiss action).
 */
@Composable
fun GameOverDialog(
    status: GameStatus,
    pegCount: Int,
    onPlayAgain: () -> Unit,
    onViewBoard: () -> Unit,
) {
    val isPerfect = status == GameStatus.Won
    val isNearMiss = !isPerfect && pegCount <= NEAR_MISS_MAX_PEGS

    val headline =
        when {
            isPerfect -> stringResource(R.string.game_over_perfect_headline)
            isNearMiss -> stringResource(R.string.game_over_near_headline)
            else -> stringResource(R.string.game_over_stuck_headline)
        }
    val body =
        when {
            isPerfect -> stringResource(R.string.game_over_perfect_body)
            isNearMiss -> pluralStringResource(R.plurals.pegs_remaining, pegCount, pegCount)
            else -> pluralStringResource(R.plurals.pegs_remaining_retry, pegCount, pegCount)
        }

    AlertDialog(
        onDismissRequest = onViewBoard,
        title = { Text(text = headline, style = MaterialTheme.typography.headlineSmall) },
        text = { Text(text = body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = onPlayAgain) {
                Text(
                    text = stringResource(R.string.play_again),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onViewBoard) {
                Text(
                    text = stringResource(R.string.view_board),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    )
}
