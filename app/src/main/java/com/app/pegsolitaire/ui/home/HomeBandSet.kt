package com.app.pegsolitaire.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import com.app.pegsolitaire.R

/**
 * Which band set Home takes (§4.2.2 D-03i, §4.2.3 D-03k).
 *
 * The decision belongs to one place, and this is it: `HomeBands` reads the space available to it
 * and applies the set — the band spacing itself, and the ribbon's shape ([ribbonShapeFor]) — so **no
 * band decides its own shape**. That is why `HomeProgressRibbon` takes a [RibbonShape] rather than
 * reading a threshold itself: two readers of the same threshold are two places for it to drift.
 *
 * Since D-03l the set no longer touches the hero: the hero is one row on every window, and the
 * stage beneath it sizes itself from its own slot, so no threshold decides it (§4.2.4). The
 * thresholds are kept at their values although their derivation assumed the old 172dp hero — the
 * full set is now shorter, so they are conservative, and re-deriving them is a later tuning
 * decision that no founder note asked for.
 *
 * It is decided from the available space and from **nothing else**. In particular the font scale
 * is never read: at 200% on a tall phone the set stays full and the column scrolls (criterion
 * 15i), because a layout that changes shape when the player changes their font size is worse than
 * one that scrolls.
 *
 * @property compact the compact band set: `spacing_sm` between bands and around the column, and
 *   on the narrow class the compact pill. All of it or none of it.
 * @property narrow the narrow window class — below `home_compact_threshold_width` of available
 *   width, where every band wraps and the arithmetic changes.
 */
@Immutable
internal data class HomeBandSet(
    val compact: Boolean,
    val narrow: Boolean,
) {
    /**
     * The compact **pill** — the fourth compact step (§4.2.3 part 2): no decorative glyph, label
     * beside value on one line.
     *
     * It is the one step gated on the width as well as the height, and it is taken *with* the
     * other three and never partially. The conjunction is not redundant: it says in code what
     * §4.2.3 says in prose, so a future window that is narrow but tall enough for the full set
     * gets a full ribbon with it rather than a compact ribbon under a full hero.
     */
    val compactPills: Boolean get() = compact && narrow
}

/**
 * Reads the two-axis compact threshold.
 *
 * Below `home_compact_threshold_width` the height threshold is
 * `home_compact_threshold_height_narrow` (880dp); at or above it, `home_compact_threshold_height`
 * (720dp) as D-03i derived it. Both are the full band set's own both-chips requirement at that
 * width, rounded onto the grid — the reason they differ is that the bands **wrap** at 360dp, which
 * is the ~156dp §4.2.2's first arithmetic missed by summing minimums.
 *
 * Its stated consequence: **at 360dp width Home is always compact**, since no shipping 360dp-wide
 * phone is 880dp tall. That is the intent — the wrap is a property of the width, not the height,
 * so a 360x800dp phone needs the compact set exactly as much as a 360x640dp one.
 *
 * @param availableWidth the `BoxWithConstraints` `maxWidth`, i.e. after the `Scaffold` has
 *   subtracted its content padding.
 * @param availableHeight the same for the height, which is why a tall navigation bar makes a
 *   device behave as the shorter device it effectively is.
 */
@Composable
internal fun homeBandSetFor(
    availableWidth: Dp,
    availableHeight: Dp,
): HomeBandSet {
    val narrow = availableWidth < dimensionResource(R.dimen.home_compact_threshold_width)
    val compactThreshold =
        dimensionResource(
            if (narrow) {
                R.dimen.home_compact_threshold_height_narrow
            } else {
                R.dimen.home_compact_threshold_height
            },
        )
    return HomeBandSet(compact = availableHeight < compactThreshold, narrow = narrow)
}

/**
 * The progress ribbon's shape, one for all three pills (§4.2 band 2, §4.2.3, §4.2.6).
 */
internal enum class RibbonShape {
    /** Glyph, then label over value: wherever the band set is full. */
    Full,

    /** Label beside value on one line, no glyph: the narrow class while every label fits half its pill. */
    Compact,

    /** Label over value, no glyph: the narrow class once any label no longer fits half its pill. */
    Stacked,
}

/**
 * Reads the ribbon's shape (D-03o, §4.2.6): the full pill unless the band set takes the compact pill
 * ([HomeBandSet.compactPills]); then the compact pill — D-03k's label beside value — while every
 * label, measured, fits in **half** its pill's content width, and the stacked pill once any one does
 * not, for all three pills alike.
 *
 * The labels decide, not the values: a value is "—" at first paint and a number a moment later, so a
 * shape read from the labels cannot change when the progress arrives (criterion 15f). The font scale
 * does not decide it either (§4.2.2): the labels are measured in the faces, the density and the font
 * scale the ribbon draws them in, so the shape follows what the text needs on this window.
 *
 * The pill's content width follows from the width this `BoxWithConstraints` already has: the ribbon
 * is the column's width less its padding, three equal pills share it less the two `spacing_sm` gaps,
 * and each pill's content is its width less its `spacing_sm` padding. The three labels are measured
 * once each, on Home's first frame, inside 15e's measured delta.
 *
 * @param bandSet the band set Home takes.
 * @param availableWidth the `BoxWithConstraints` `maxWidth`.
 * @param bandSpacing the band set's spacing, which is also the column's padding.
 */
@Composable
internal fun ribbonShapeFor(
    bandSet: HomeBandSet,
    availableWidth: Dp,
    bandSpacing: Dp,
): RibbonShape {
    val labels =
        listOf(
            stringResource(R.string.home_stat_stars_label),
            stringResource(R.string.home_stat_boards_label),
            stringResource(R.string.home_stat_level_label),
        )
    val style = MaterialTheme.typography.labelMedium
    val measurer = rememberTextMeasurer(cacheSize = RIBBON_PILLS)
    val pillSpacing = dimensionResource(R.dimen.spacing_sm)
    val ribbonWidth = minOf(availableWidth, dimensionResource(R.dimen.content_max_width)) - bandSpacing * 2
    val pillContentWidth = (ribbonWidth - pillSpacing * (RIBBON_PILLS - 1)) / RIBBON_PILLS - pillSpacing * 2
    val halfPx = with(LocalDensity.current) { (pillContentWidth / 2).toPx() }
    return when {
        !bandSet.compactPills -> RibbonShape.Full
        labels.any { label -> measurer.measure(label, style).size.width > halfPx } -> RibbonShape.Stacked
        else -> RibbonShape.Compact
    }
}

/** The ribbon's pills: Stars, Boards and Level. */
private const val RIBBON_PILLS = 3
