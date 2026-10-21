package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import com.app.pegsolitaire.R

/**
 * Which band set Home takes (§4.2.2 D-03i, §4.2.3 D-03k).
 *
 * The decision belongs to one place, and this is it: `HomeBands` reads the space available to it
 * and applies the set — the band spacing itself, and a flag for the ribbon — so **no band decides
 * its own shape**. That is why `HomeProgressRibbon` takes a `Boolean` rather than reading a
 * threshold itself: two readers of the same threshold are two places for it to drift.
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
