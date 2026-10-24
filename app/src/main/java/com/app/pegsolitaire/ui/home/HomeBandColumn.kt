package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Home's band column (ux-spec §4.2.2, §4.2.5 D-03n): the hero row, the stage, the progress ribbon,
 * up to two Continue chips and the two mode tiles, top to bottom, with the footer's slack below
 * them — one measure policy that gives every band its own height and then splits what is left.
 *
 * ## Why a measure policy, and not two weights
 *
 * D-03i split the slack 3 : 1 between the stage and the footer with two `Column` weights. A weight
 * is a share of the slack the column has **now**, so every chip that arrived after first paint
 * took its room from both shares, and the mode tiles under it moved 22–24dp, beneath a thumb that
 * was already heading for them (QA's loop-3 F-1). D-03n fixes the footer to a share of the slack
 * the column has with **no** chip, S₀, and takes the chips' room from the stage alone — which no
 * pair of weights can say, because S₀ is not the slack the column has once a chip is in.
 *
 * ## The quantities, in px, all from this measure pass
 *
 * - Each band is measured at the column's width and its own height: the hero, the ribbon, both
 *   tiles, and each chip present.
 * - **S₀** = max(0, inner − hero − ribbon − both tiles − the five band gaps the column has with no
 *   chip), where `inner` is [availableHeightPx] less the column's padding above and below. It
 *   depends on the window, the font scale and those bands, never on the chips.
 * - **The footer with no chip** is [FOOTER_SHARE_OF_SLACK] of S₀, and **the stage with no chip**
 *   the rest of S₀, so the two add up to S₀ exactly, to the pixel.
 * - **R**, the chips' room: for each chip present, its height plus the band gap it brings.
 * - **The stage** = max(0, stage with no chip − R); **the footer** = min(footer with no chip,
 *   max(0, S₀ − R)).
 *
 * So the column is exactly `inner` tall while R ≤ S₀, and overflows by R − S₀ once it is not, and
 * the scroll above takes the rest (criteria 14, 15i, 15k). Tile 1's top edge sits at the hero, the
 * ribbon, three gaps and the no-chip stage below the top **whatever R is**, as long as R is no more
 * than the no-chip stage: the stage gives up exactly what the chips take, so the ribbon rides up on
 * its bottom edge and the tiles hold still (criterion 15s). Past that the tiles move by the
 * shortfall and no more (15t), because the stage is 0 and the footer pays the rest, down to 0.
 *
 * ## What it does not change
 *
 * The band order, the bands themselves and their entrance cascade (which moves a band by a
 * drawing offset, never by layout) are untouched; the stage is still the one child that holds a
 * drawing in space no band claims (§4.2.2 clause 4), no content band ever takes slack, and none
 * sits between the ribbon and the chips (clause 6). The column reports its own height, so nothing
 * in the modifier chain around it sets one (`HomeBands`).
 *
 * @param availableHeightPx the height `HomeBands`' `BoxWithConstraints` was given, in px — the
 *   viewport the column scrolls in, after the `Scaffold` has taken the system bars. Inside the
 *   scroll the column's own maximum height is infinite, so it is handed the viewport's.
 * @param bandSpacing the band set's spacing: between two bands, and the column's padding.
 * @param modifier the column's modifier chain, its padding included.
 * @param hero band 1, the hero row.
 * @param stage the stage, measured at exactly the height this policy gives it.
 * @param ribbon band 2, the progress ribbon.
 * @param chips band 3: the Continue chips present, top to bottom — none, one or two.
 * @param tiles band 4: the two mode tiles, top to bottom.
 */
@Composable
internal fun HomeBandColumn(
    availableHeightPx: Int,
    bandSpacing: Dp,
    modifier: Modifier,
    hero: @Composable () -> Unit,
    stage: @Composable () -> Unit,
    ribbon: @Composable () -> Unit,
    chips: @Composable () -> Unit,
    tiles: @Composable () -> Unit,
) {
    Layout(
        contents = listOf(hero, stage, ribbon, chips, tiles),
        modifier = modifier,
    ) { bands, constraints ->
        val gap = bandSpacing.roundToPx()
        val width = constraints.maxWidth
        val atColumnWidth = Constraints(maxWidth = width)
        val heroPlaceable = bands[HERO_SLOT].single().measure(atColumnWidth)
        val ribbonPlaceable = bands[RIBBON_SLOT].single().measure(atColumnWidth)
        val chipPlaceables = bands[CHIPS_SLOT].map { it.measure(atColumnWidth) }
        val (firstTile, secondTile) = bands[TILES_SLOT].map { it.measure(atColumnWidth) }

        val inner = availableHeightPx - 2 * gap
        val fixedBands =
            heroPlaceable.height + ribbonPlaceable.height + firstTile.height + secondTile.height +
                FIXED_BAND_GAPS * gap
        val slack = max(0, inner - fixedBands)
        val footerWithNoChip = (slack * FOOTER_SHARE_OF_SLACK).roundToInt()
        val stageWithNoChip = slack - footerWithNoChip
        val chipRooms = chipPlaceables.map { it.height + gap }
        val room = chipRooms.sum()
        val stageHeight = max(0, stageWithNoChip - room)
        val footer = min(footerWithNoChip, max(0, slack - room))
        val stagePlaceable = bands[STAGE_SLOT].single().measure(Constraints.fixed(width, stageHeight))

        layout(width, fixedBands + stageHeight + room + footer) {
            var y = 0
            heroPlaceable.place(0, y)
            y += heroPlaceable.height + gap
            stagePlaceable.place(0, y)
            y += stageHeight + gap
            ribbonPlaceable.place(0, y)
            y += ribbonPlaceable.height + gap
            chipPlaceables.forEachIndexed { index, chip ->
                chip.place(0, y)
                y += chipRooms[index]
            }
            firstTile.place(0, y)
            y += firstTile.height + gap
            secondTile.place(0, y)
        }
    }
}

/**
 * The footer's share of the no-chip slack S₀ (ux-spec §4.2.5, D-03n): one fifth, fixed for the
 * window, so the stage holds the other four fifths less the chips' room. §4.2.5 gives the three
 * reasons it is a fifth and not D-03i's quarter: it pins the mode tiles on every phone window
 * measured, a quarter failing by 5dp at 360x640dp with both chips; it keeps the founder's one-chip
 * Home where they approved it, the board 3.6dp smaller rather than 24dp; and it keeps the one-chip
 * stage a board down to the full set's 720dp threshold. A unitless ratio, so a scoped constant
 * rather than a `dimens.xml` token (§6.4); it replaces D-03i's two weights, which are not to be
 * rebuilt (§4.2.4).
 */
private const val FOOTER_SHARE_OF_SLACK = 0.2f

/**
 * The band gaps of the column with no chip: hero, stage, ribbon, two tiles and the footer are six
 * children, and the stage and the footer bring one gap each at any height, even 0. Each chip
 * present brings one more, which is counted in its room.
 */
private const val FIXED_BAND_GAPS = 5

/** Where each band's content sits in the column's slots, top to bottom. */
private const val HERO_SLOT = 0
private const val STAGE_SLOT = 1
private const val RIBBON_SLOT = 2
private const val CHIPS_SLOT = 3
private const val TILES_SLOT = 4
