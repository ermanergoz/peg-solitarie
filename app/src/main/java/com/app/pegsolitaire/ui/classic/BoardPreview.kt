package com.app.pegsolitaire.ui.classic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.ui.components.BoardCanvas
import com.app.pegsolitaire.ui.game.GameUiState
import androidx.compose.material.icons.outlined.Star as StarBorder

/**
 * The static, read-only board footprint shown on a Classic catalog card (D-11),
 * optionally under the player's earned-star pips (D-10).
 *
 * The footprint itself is a thin stateless wrapper over the Phase-1 [BoardCanvas] in
 * `interactive = false` mode: it renders the [board]'s resting pegs (`secondary`) and
 * the recessed start-empty hole (`surfaceContainerLowest` + `outlineVariant` ring) only
 * — no selection ring, no legal-destination dots, no animation, and **not tappable**
 * (the parent `Card` owns the tap). It is purely decorative (the card's name +
 * hole-count label it), so it carries no content description.
 *
 * The preview doubles as a free smoke test that each `BoardSpec` footprint (dims 7/8/9,
 * incl. the off-center Asymmetric-39) lays out correctly via the generalized
 * `BoardGeometry`.
 *
 * @param board the descriptor's `previewBoard` (the start state, so the start-empty
 *   hole shows recessed in the preview).
 * @param modifier layout modifier for the preview backing.
 * @param earnedStars 0-3 stars earned on this board, rendered as pips beneath the
 *   footprint; `null` (the default) omits the pip row entirely, which is what
 *   non-catalog uses such as the onboarding illustration want. The value is hoisted
 *   state — it comes from [ClassicBoardUiState], never from a repository read here.
 */
@Composable
fun BoardPreview(
    board: Board,
    modifier: Modifier = Modifier,
    earnedStars: Int? = null,
) {
    if (earnedStars == null) {
        BoardFootprint(board = board, modifier = modifier)
        return
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
    ) {
        BoardFootprint(board = board)
        EarnedStarPips(earned = earnedStars)
    }
}

/** The recessed square holding the read-only [BoardCanvas] footprint. */
@Composable
private fun BoardFootprint(
    board: Board,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        modifier = modifier.size(dimensionResource(R.dimen.catalog_preview_size)),
    ) {
        BoardCanvas(
            state = GameUiState.initial(board = board),
            onCellTap = {},
            onAnimationFinished = {},
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_sm)),
            interactive = false,
        )
    }
}

/**
 * The earned-star pips (D-10): the first [earned] of [TOTAL_STARS] filled
 * [Icons.Filled.Star] tinted `primary`, the rest outline [StarBorder] tinted
 * `outlineVariant` — the same filled/outline language as the Results star row, at pip
 * scale. The individual icons are decorative, so the row speaks the count once via
 * [clearAndSetSemantics] instead of announcing three anonymous stars.
 */
@Composable
private fun EarnedStarPips(earned: Int) {
    val description = stringResource(R.string.catalog_stars_earned, earned, TOTAL_STARS)
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        repeat(TOTAL_STARS) { index ->
            val isEarned = index < earned
            Icon(
                imageVector = if (isEarned) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint =
                    if (isEarned) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                modifier = Modifier.size(dimensionResource(R.dimen.catalog_star_pip_size)),
            )
        }
    }
}

/** The total number of pips rendered on a card (earned + unearned) — the 1-3 star scale (D-01). */
private const val TOTAL_STARS = 3
