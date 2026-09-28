package com.app.pegsolitaire.ui.game

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.app.pegsolitaire.ui.theme.AppTheme

/*
 * Previews of the gameplay screen (rules/compose.md: every public composable has light and dark
 * previews). A file of their own because `GameScreen.kt` already holds as many functions as
 * detekt allows a file, every one of them a cue or a surface the screen hosts.
 */

/**
 * The gameplay screen on the design baseline's 360x640dp window in both themes, mid-Challenge:
 * the status strip at its D-05d sizes over the board, and the dock in the looks a fresh board
 * gives it — Undo Unavailable, Hint Available, Show-moves On, Restart Available (§4.4.1).
 */
@Preview(name = "Gameplay · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(
    name = "Gameplay · dark",
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun GameScreenPreview() {
    AppTheme {
        GameScreen(
            state = GameUiState.initial().copy(elapsedSeconds = PREVIEW_ELAPSED_SECONDS),
            onCellTap = {},
            onRestart = {},
            onConfirmRestart = {},
            onDismissRestart = {},
            onAnimationFinished = {},
            onToggleHints = {},
            onUndo = {},
            onHint = {},
            onAcceptAdOffer = {},
            onDismissAdOffer = {},
            onHome = {},
            onNavigateBack = {},
            challengeLevel = PREVIEW_CHALLENGE_LEVEL,
        )
    }
}

/** The design baseline §4.3.1 budgets against, a representative clock and a four-digit level. */
private const val PREVIEW_WIDTH_DP = 360
private const val PREVIEW_HEIGHT_DP = 640
private const val PREVIEW_ELAPSED_SECONDS = 488
private const val PREVIEW_CHALLENGE_LEVEL = 1234
