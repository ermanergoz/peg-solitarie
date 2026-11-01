package com.app.pegsolitaire.ui.game

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * A lightbulb glyph as a locally-defined [ImageVector] — the Hint action's icon (ASST-01,
 * ux-spec D-04a).
 *
 * It **replaces the core set's magnifying glass**, which read as "search" or "zoom" rather than
 * "show me a winning move": that glyph is the single most conventional in software and it
 * conventionally means find-by-typing. Criterion 20 is the check.
 *
 * The core `material-icons-core` set — the only icon dependency this project ships, the heavy
 * `material-icons-extended` set being intentionally avoided — has no lightbulb, so the vector is
 * declared here instead of pulling that library in. Same reason and same shape as [UndoIcon].
 *
 * `autoMirror` is deliberately **off**: a bulb is symmetric and has no reading direction, unlike
 * the undo arrow. The [androidx.compose.material3.Icon] composable supplies the tint, so the
 * path is filled opaque black and recoloured at draw time.
 *
 * Path data is a string constant rather than a sequence of numeric path calls, following
 * [UndoIcon] — which is why this file needs **no `MagicNumber` suppression** at all, where
 * `ModeGlyphs.kt`'s builder-style glyphs each carry one.
 */
val HintIcon: ImageVector by lazy {
    ImageVector
        .Builder(
            name = "Hint",
            defaultWidth = ICON_SIZE_DP.dp,
            defaultHeight = ICON_SIZE_DP.dp,
            viewportWidth = ICON_VIEWPORT,
            viewportHeight = ICON_VIEWPORT,
        ).addPath(
            pathData = addPathNodes(HINT_PATH),
            fill = SolidColor(Color.Black),
        ).build()
}

/** The glyph is authored on a 24dp square canvas, like every Material symbol. */
private const val ICON_SIZE_DP = 24f

/** The vector's coordinate space edge (matches [ICON_SIZE_DP]). */
private const val ICON_VIEWPORT = 24f

/**
 * SVG path of the lightbulb: the bulb, then the two base bars beneath it.
 *
 * Three subpaths in one string — the bulb body with its neck, the wide base bar and the narrow
 * foot — so the glyph reads as a bulb at `game_action_glyph_size` (28dp) as well as at 24.
 */
private const val HINT_PATH =
    "M12,2C8.13,2 5,5.13 5,9c0,2.38 1.19,4.47 3,5.74V17h8v-2.26c1.81,-1.27 3,-3.36 " +
        "3,-5.74 0,-3.87 -3.13,-7 -7,-7z " +
        "M9,18h6v2H9z " +
        "M10,21h4v1h-4z"
