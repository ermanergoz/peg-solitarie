package com.app.pegsolitaire.ui.results

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.ui.theme.AppTheme

/*
 * Previews of the Results screen (rules/compose.md: every public composable has light and dark
 * previews), in their own file as the gameplay screen's are.
 */

/**
 * A one-star Challenge clear on the design baseline's 360x640dp window in both themes (ux-spec §4.6,
 * D-SB-1/D-SB-2): "Cleared!", one filled star and two hollow, the next-star line naming the next
 * band up, and Next level as the one gradient action.
 */
@Preview(name = "Results, Challenge one-star clear · light", widthDp = PREVIEW_WIDTH_DP, heightDp = PREVIEW_HEIGHT_DP)
@Preview(
    name = "Results, Challenge one-star clear · dark",
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ResultsScreenClearedPreview() {
    AppTheme {
        ResultsScreen(
            state =
                ResultsUiState(
                    stars = PREVIEW_ONE_STAR,
                    pegsRemaining = PREVIEW_PEGS_LEFT,
                    elapsedSeconds = PREVIEW_ELAPSED_SECONDS,
                    score = PREVIEW_SCORE,
                    headlineTier = ResultsHeadlineTier.CLEARED,
                    replayId = "",
                    challengeLevel = PREVIEW_CHALLENGE_LEVEL,
                ),
            onReplay = {},
            onCatalog = {},
            onHome = {},
            onNextLevel = {},
            onRetry = {},
            onNavigateBack = {},
        )
    }
}

/**
 * A three-star Classic result in both themes (ux-spec D-RC, §4.7): "Perfect!", three gold stars, the
 * gold rays from the star row behind the column. The preview is a still frame; the confetti falls
 * only on a live result whose celebration latch is set.
 */
@Preview(
    name = "Results, Classic three stars with rays · light",
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
)
@Preview(
    name = "Results, Classic three stars with rays · dark",
    widthDp = PREVIEW_WIDTH_DP,
    heightDp = PREVIEW_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun ResultsScreenPerfectPreview() {
    AppTheme {
        ResultsScreen(
            state =
                ResultsUiState(
                    stars = PREVIEW_THREE_STARS,
                    pegsRemaining = PREVIEW_ONE_PEG,
                    elapsedSeconds = PREVIEW_ELAPSED_SECONDS,
                    score = PREVIEW_SCORE,
                    headlineTier = ResultsHeadlineTier.PERFECT,
                    replayId = ClassicLevelIds.ENGLISH33,
                ),
            onReplay = {},
            onCatalog = {},
            onHome = {},
            onNextLevel = {},
            onRetry = {},
            onNavigateBack = {},
        )
    }
}

/** The design baseline, and a one-star clear as level 3 deals it: three pegs left. */
private const val PREVIEW_WIDTH_DP = 360
private const val PREVIEW_HEIGHT_DP = 640
private const val PREVIEW_ONE_STAR = 1
private const val PREVIEW_PEGS_LEFT = 3
private const val PREVIEW_ELAPSED_SECONDS = 47
private const val PREVIEW_SCORE = 2_940
private const val PREVIEW_CHALLENGE_LEVEL = 3

/** A perfect Classic finish: three stars, one peg left. */
private const val PREVIEW_THREE_STARS = 3
private const val PREVIEW_ONE_PEG = 1
