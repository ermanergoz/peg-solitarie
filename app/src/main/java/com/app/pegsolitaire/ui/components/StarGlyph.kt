package com.app.pegsolitaire.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.twotone.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.unit.Dp
import com.app.pegsolitaire.ui.game.recipe
import com.app.pegsolitaire.ui.theme.LocalBoardMarks
import com.app.pegsolitaire.ui.theme.gradientFill

/**
 * A star in the active palette (docs/ux/colour-vision.md §3.4, CV-10, CV-13a): earned — filled with
 * the palette's gradient and, when the palette has one, outlined over it — or unearned, in the
 * palette's outline colour. In both palettes the unearned star is [StarOutline], hollow, so filled
 * against hollow reads without colour (CV-13a); the standard earned star is today's gold glyph,
 * with no outline. Decorative: it adds no semantics, so every row that holds stars keeps speaking
 * for itself (`EarnedStarCountKey`, the catalogue's and the tutorial's descriptions).
 *
 * @param earned whether this star is earned.
 * @param size the star's edge.
 * @param modifier the star's modifier (a pop-in scale, for instance).
 */
@Composable
fun StarGlyph(
    earned: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val marks = LocalBoardMarks.current
    Box(modifier.size(size)) {
        if (earned) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.matchParentSize().gradientFill(Brush.linearGradient(marks.earnedStarFill)),
            )
            marks.earnedStarOutline?.let { outline ->
                Icon(StarOutline, contentDescription = null, tint = outline, modifier = Modifier.matchParentSize())
            }
        } else {
            Icon(
                imageVector = if (marks.palette.recipe.unearnedStarHollow) StarOutline else Icons.Outlined.Star,
                contentDescription = null,
                tint = marks.unearnedStarOutline,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/**
 * The hollow star: the opaque layer of the core set's `Icons.TwoTone.Star`, which is Material's
 * `star_border` artwork — an outer star with the inner one cut out. `material-icons-core` ships no
 * hollow star of its own (its `Outlined.Star` is a filled shape, the glyph unearned stars used
 * before CV-13a), so this is built once from the dependency's vector rather than copied into the
 * repo, and drawn at the icon's own stroke weight, scaled with the size.
 */
internal val StarOutline: ImageVector by lazy {
    val twoTone = Icons.TwoTone.Star
    val outline = twoTone.root.filterIsInstance<VectorPath>().single { it.fillAlpha == OPAQUE }
    ImageVector
        .Builder(
            name = "StarOutline",
            defaultWidth = twoTone.defaultWidth,
            defaultHeight = twoTone.defaultHeight,
            viewportWidth = twoTone.viewportWidth,
            viewportHeight = twoTone.viewportHeight,
        ).addPath(pathData = outline.pathData, pathFillType = outline.pathFillType, fill = SolidColor(Color.Black))
        .build()
}

/** The two-tone star's outline layer is the one drawn at full alpha; its tone layer is at 0.3. */
private const val OPAQUE = 1f
