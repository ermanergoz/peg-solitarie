package com.app.pegsolitaire.ui.game

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.app.pegsolitaire.ui.backdrop.BackdropDensity
import com.app.pegsolitaire.ui.backdrop.BackdropPreviewHost
import com.app.pegsolitaire.ui.theme.AppTheme

/*
 * Previews of the gameplay screen (rules/compose.md: every public composable has light and dark
 * previews). A file of their own because `GameScreen.kt` already holds as many functions as
 * detekt allows a file, every one of them a cue or a surface the screen hosts. The screen's
 * `Scaffold` is transparent, so each preview draws it over the game screen's backdrop as the host
 * draws it, through `BackdropPreviewHost` at the Quiet density with the clock fixed (BD-25).
 */

/**
 * The gameplay screen on the design baseline's 360x640dp window in both themes, mid-Challenge, over
 * its backdrop: the status strip at its D-05d sizes over the board, and the dock in the looks a fresh
 * board gives it — Undo Unavailable, Hint Available, Show-moves On, Restart Available (§4.4.1).
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
        BackdropPreviewHost(BackdropDensity.Quiet) {
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
}

/**
 * The same screen with the Undecided notice up in the banner's slot (ux-spec §4.4.2, D-04f): the
 * last Hint search stopped with no move and no proof, so the notice, not the rescue banner, sits
 * above the dock.
 */
@Preview(name = "Gameplay, Undecided notice · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(
    name = "Gameplay, Undecided notice · dark",
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun GameScreenUndecidedNoticePreview() {
    AppTheme {
        BackdropPreviewHost(BackdropDensity.Quiet) {
            GameScreen(
                state = GameUiState.initial().copy(elapsedSeconds = PREVIEW_ELAPSED_SECONDS, hintUndecided = true),
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
            )
        }
    }
}

/** The design baseline §4.3.1 budgets against, a representative clock and a four-digit level. */
private const val PREVIEW_WIDTH_DP = 360
private const val PREVIEW_HEIGHT_DP = 640
private const val PREVIEW_ELAPSED_SECONDS = 488
private const val PREVIEW_CHALLENGE_LEVEL = 1234
