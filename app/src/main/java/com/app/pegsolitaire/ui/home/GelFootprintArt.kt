package com.app.pegsolitaire.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Cell
import com.app.pegsolitaire.ui.components.BoardGeometry
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.GelColors

/**
 * The Classic gel tile's trailing art (backdrop spec §3.5.2, the tech-lead's A4): the same
 * footprint `StaticBoardArt` drew, flat and white on the gel — pegs `GelArtPeg`, holes `GelArtHole`,
 * with no sphere shading, specular or dimple. One draw node that reads `BoardGeometry` for the cell
 * centres and its `BoardMetrics` radius ratios, and edits neither. Decorative: no semantics, as
 * `StaticBoardArt` carried none.
 */
@Composable
internal fun GelFootprintArt(
    board: Board,
    modifier: Modifier = Modifier,
) {
    Spacer(
        modifier =
            modifier.drawWithCache {
                val geometry = BoardGeometry.forCanvas(board.spec, size)
                val cells = board.spec.inPlay
                onDrawBehind {
                    for (i in cells.indices) {
                        val pegged = board.cellAt(cells[i]) == Cell.Peg
                        drawCircle(
                            color = if (pegged) GelColors.artPeg else GelColors.artHole,
                            radius = if (pegged) geometry.pegRadius else geometry.holeRadius,
                            center = geometry.center(cells[i]),
                        )
                    }
                }
            },
    )
}

/** The footprint on the Classic gel, light and dark (it is the same in both: the gel is). */
@Preview(name = "Gel footprint · light")
@Preview(name = "Gel footprint · dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GelFootprintArtPreview() {
    AppTheme {
        GelFootprintArt(
            board = ClassicTileArtBoard,
            modifier = Modifier.size(dimensionResource(R.dimen.home_tile_art_size)),
        )
    }
}
