package com.app.pegsolitaire.ui.game

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The Material "undo" curved-arrow glyph as a locally-defined [ImageVector].
 *
 * The core `material-icons-core` set (the only icon dependency this project ships — the
 * heavy `material-icons-extended` set is intentionally avoided, see `libs.versions.toml`)
 * has no undo glyph, so the single vector is declared here instead of pulling in the
 * extended library. `autoMirror = true` flips it under RTL, matching the auto-mirrored
 * back/undo affordances. The [Icon] composable supplies the actual tint, so the path is
 * filled opaque black and recolored at draw time.
 *
 * Path data is the standard 24dp Material `undo` symbol.
 */
val UndoIcon: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "Undo",
            defaultWidth = ICON_SIZE_DP.dp,
            defaultHeight = ICON_SIZE_DP.dp,
            viewportWidth = ICON_VIEWPORT,
            viewportHeight = ICON_VIEWPORT,
            autoMirror = true,
        ).addPath(
            pathData = addPathNodes(UNDO_PATH),
            fill = SolidColor(Color.Black),
        ).build()
}

/** The Material `undo` symbol is authored on a 24dp square canvas. */
private const val ICON_SIZE_DP = 24f

/** The vector's coordinate space edge (matches [ICON_SIZE_DP]). */
private const val ICON_VIEWPORT = 24f

/** SVG path of the standard Material `undo` (curved back-arrow) glyph. */
private const val UNDO_PATH =
    "M12.5,8c-2.65,0 -5.05,0.99 -6.9,2.6L2,7v9h9l-3.62,-3.62c1.39,-1.16 3.16,-1.88 " +
        "5.12,-1.88 3.54,0 6.55,2.31 7.6,5.5l2.37,-0.78C21.08,11.03 17.15,8 12.5,8z"
