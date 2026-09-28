package com.app.pegsolitaire.ui.game

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * A peg with two trailing destination dots, as a locally-defined [ImageVector] — the
 * Show-possible-moves toggle's icon (ux-spec D-04b).
 *
 * It **replaces the core set's information glyph**, whose meaning collided head-on: the same
 * drawing was simultaneously "help" on Home (deleted by D-02) and "show possible moves" here, so
 * two unrelated affordances in one app were drawn identically. Criterion 20 is the check.
 *
 * The shape is literally what the feature draws — a peg, then the dots a legal jump could land
 * on — which is the strongest form a toggle glyph can take: it depicts its own effect rather
 * than standing for it. The core icon set has no visibility or eye glyph that would not mean
 * "preview", and `material-icons-extended` stays out of the build.
 *
 * The [androidx.compose.material3.Icon] composable supplies the tint, so the path is filled
 * opaque black and recoloured at draw time. Path data is a string constant, following
 * [UndoIcon] and [HintIcon], so this file needs **no `MagicNumber` suppression**.
 */
val ShowMovesIcon: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "ShowMoves",
            defaultWidth = ICON_SIZE_DP.dp,
            defaultHeight = ICON_SIZE_DP.dp,
            viewportWidth = ICON_VIEWPORT,
            viewportHeight = ICON_VIEWPORT,
            // The peg leads and the dots trail, so the glyph has a reading direction and must
            // flip under RTL — exactly like the undo arrow, and unlike the symmetric bulb.
            autoMirror = true,
        ).addPath(
            pathData = addPathNodes(SHOW_MOVES_PATH),
            fill = SolidColor(Color.Black),
        ).build()
}

/** The glyph is authored on a 24dp square canvas, like every Material symbol. */
private const val ICON_SIZE_DP = 24f

/** The vector's coordinate space edge (matches [ICON_SIZE_DP]). */
private const val ICON_VIEWPORT = 24f

/**
 * SVG path of the peg and its two destination dots, as three circles on the vertical midline.
 *
 * Each circle is a move-to plus two half-arcs, the standard way to write one in path data. The
 * radii step down — the peg, then a nearer dot, then a further, smaller one — so the glyph reads
 * as direction at a glance rather than as three equal bullets.
 */
private const val SHOW_MOVES_PATH =
    "M7,12m-5,0a5,5 0 1,0 10,0a5,5 0 1,0 -10,0 " +
        "M15.5,12m-2.5,0a2.5,2.5 0 1,0 5,0a2.5,2.5 0 1,0 -5,0 " +
        "M21,12m-2,0a2,2 0 1,0 4,0a2,2 0 1,0 -4,0"
