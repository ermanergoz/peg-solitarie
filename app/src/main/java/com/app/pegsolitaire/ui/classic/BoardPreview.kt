package com.app.pegsolitaire.ui.classic

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.ui.components.BoardCanvas
import com.app.pegsolitaire.ui.game.GameUiState

/**
 * The static, read-only board footprint shown on a Classic catalog card (D-11).
 *
 * A thin stateless wrapper over the Phase-1 [BoardCanvas] in `interactive = false`
 * mode: it renders the [board]'s resting pegs (`secondary`) and the recessed
 * start-empty hole (`surfaceContainerLowest` + `outlineVariant` ring) only — no
 * selection ring, no legal-destination dots, no animation, and **not tappable** (the
 * parent `Card` owns the tap). It is purely decorative (the card's name + hole-count
 * label it), so it carries no content description.
 *
 * The preview doubles as a free smoke test that each `BoardSpec` footprint (dims 7/8/9,
 * incl. the off-center Asymmetric-39) lays out correctly via the generalized
 * `BoardGeometry`.
 *
 * @param board the descriptor's `previewBoard` (the start state, so the start-empty
 *   hole shows recessed in the preview).
 * @param modifier layout modifier for the preview backing.
 */
@Composable
fun BoardPreview(
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
