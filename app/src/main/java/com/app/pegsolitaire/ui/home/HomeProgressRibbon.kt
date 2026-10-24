package com.app.pegsolitaire.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Constraints
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.ui.theme.clayEntrance
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.goldGradientFill
import com.app.pegsolitaire.ui.theme.withTabularFigures
import kotlin.math.max

/** Semantics tag the tests use to locate the progress ribbon — never its copy (§13). */
const val HOME_PROGRESS_RIBBON_TEST_TAG = "home_progress_ribbon"

/**
 * Band 2 — the progress ribbon: three equal-weight clay pills, Stars · Boards · Level (§4.2).
 *
 * All three values come from state the ViewModel already collects, and the Boards pill's
 * **denominator is the Classic catalogue's own size** — a compile-time fact, never a stored
 * count, so no stored row and no tutorial board joining `CLASSIC_SPECS` can move a shipped
 * number (D-03c, §5 prohibition 5).
 *
 * The ribbon is **additive, and nothing here is an oracle** (§4.2.1): the Challenge tile's own
 * level chip remains the cloud-save device test's readout and the Level pill is a second view
 * of the same field, and the Stars pill is a **total** that explicitly does not replace the
 * Classic catalogue's per-board pips. If a pill and a tile ever disagree, the tile is right.
 *
 * The pills are **not tappable** (§5): they are status, and making them navigate would put
 * three more competing targets on the screen the founder already found busy.
 *
 * ## Three shapes, one per ribbon, and none costs a readout (§4.2.3 D-03k, §4.2.6 D-03o)
 *
 * [shape] is decided by `HomeBands` from the band set and the measured labels — never here — and
 * every pill takes it:
 *
 * - [RibbonShape.Full]: glyph, then label over value, wherever the band set is full.
 * - [RibbonShape.Compact]: the **fourth** compact step, taken on the narrow class together with the
 *   other three and never partially. It drops the decorative glyph and puts the label beside the
 *   value on one line.
 * - [RibbonShape.Stacked]: once any label no longer fits half its pill on the narrow class — at a
 *   large font scale — the label on its own line and the value centred under it, no glyph: the full
 *   pill without its glyph.
 *
 * In every shape: three pills, three labels, three values, the same equal widths, the same clay
 * surface, the same entrance order, still not tappable, and **one** `clearAndSetSemantics` node per
 * pill speaking the same sentence, byte for byte. The glyph is `contentDescription = null` decoration
 * inside that node, so it carries no information at all — TalkBack cannot tell the shapes apart, and
 * criteria 15m and 15w are the tests that say so.
 *
 * **No label and no value ever breaks** (§4.2.6 rule 1). Each is one line and never wraps: every
 * label is one word and every value one token, and the shape rule is what guarantees they fit.
 *
 * §4.2.2's first arithmetic called this band unyieldable at "~60dp inside a 64dp minimum". It
 * omitted the 20dp glyph and one `spacing_xs` and read a *minimum* as a cap: the pill composes
 * ≥ 84dp at default font scale and measured **116dp** at 360dp width. The ~44dp of decoration and
 * stacking inside that is exactly what this yields, and nothing else in the band moves.
 *
 * The band is **not** dropped, though that would have closed the no-scroll arithmetic: 8dp is not
 * a margin, it would make criterion 11's three pills false on the emptiest-looking phones, and the
 * Level pill is the ladder position's above-the-fold witness once the Challenge tile's chip is
 * below the fold (§4.2.3).
 *
 * @param shape the ribbon's shape, every pill's (§4.2.3 part 2, §4.2.6).
 */
@Composable
internal fun HomeProgressRibbon(
    shape: RibbonShape,
    totalStars: Int?,
    classicBoardsCleared: Int?,
    challengeCurrentLevel: Int,
) {
    Row(
        modifier =
            Modifier
                .clayEntrance(RIBBON_ENTRANCE_ORDER)
                .fillMaxWidth()
                .testTag(HOME_PROGRESS_RIBBON_TEST_TAG),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_sm)),
    ) {
        StatPill(
            glyph = Icons.Filled.Star,
            gilded = true,
            shape = shape,
            label = stringResource(R.string.home_stat_stars_label),
            value = totalStars?.toString(),
            modifier = Modifier.weight(CONTENT_WEIGHT),
        )
        StatPill(
            glyph = rememberClassicGlyph(),
            gilded = false,
            shape = shape,
            label = stringResource(R.string.home_stat_boards_label),
            value =
                classicBoardsCleared?.let { cleared ->
                    stringResource(R.string.home_stat_boards_value, cleared, Boards.CLASSIC_SPECS.size)
                },
            modifier = Modifier.weight(CONTENT_WEIGHT),
        )
        StatPill(
            glyph = rememberChallengeGlyph(),
            gilded = false,
            shape = shape,
            // The ladder position is always known — it has a persisted fail-open default — so
            // this pill never shows the placeholder. Copy is "Level N", the NEXT level to play,
            // deliberately off-by-one from the highest level banked. Do not reconcile the two.
            label = stringResource(R.string.home_stat_level_label),
            value = challengeCurrentLevel.toString(),
            modifier = Modifier.weight(CONTENT_WEIGHT),
        )
    }
}

/**
 * One progress pill: a `surfaceContainerHigh` clay surface holding a glyph, the label and the
 * value.
 *
 * ## Unknown renders as a placeholder, never as zero (D-03c/D-03h)
 *
 * A `null` [value] paints `stat_value_unknown` ("—") and announces `stat_value_unknown_desc`,
 * so the em dash is never spoken as punctuation. "Not yet read" and "the read failed" arrive
 * here as the same `null` **by construction** — the ViewModel maps a failed read to it — which
 * is only safe because a genuine `0` still renders as "0": zero is a real answer for a
 * brand-new player, and a player with forty stars must never be told they have none.
 *
 * ## One node reading label-then-value, sized from its label
 *
 * The pill speaks **once**, as "Stars, 7" — or "Stars, not yet available" — and never as a bare
 * number. `clearAndSetSemantics` is used rather than `mergeDescendants` on purpose: a merged
 * node that carried both a `contentDescription` and its children's text is read by TalkBack as
 * the description alone, which would silently drop the label from exactly the pill that needs
 * it most (the unknown one). Clearing and setting one description makes the spoken sentence a
 * property of this composable rather than of a merge policy.
 *
 * It carries **no click action** — the absence is the contract (§5), not an omission. The width
 * comes from the caller's equal `weight`, so a value arriving later cannot reflow the row.
 *
 * ## Three shapes, one sentence
 *
 * [shape] selects the pill's layout. The description is computed **before** the branch and the
 * label and the value are the same two leaves in every shape ([PillLabel], [PillValue]), so the
 * spoken sentence and the rendered copy cannot drift between them — the same reason `HomeHero`'s
 * two shapes share `HeroTitle` and `SettingsGear`. The minimum height is the only other thing that
 * differs, and both are floors: neither text is ever truncated or scaled down, so a font scale that
 * does not fit grows the pill, then the band, and the column scrolls (criterion 14).
 */
@Composable
private fun StatPill(
    glyph: ImageVector,
    gilded: Boolean,
    shape: RibbonShape,
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
) {
    val spokenValue = value ?: stringResource(R.string.stat_value_unknown_desc)
    val description = stringResource(R.string.home_stat_pill_desc, label, spokenValue)
    val minHeight =
        dimensionResource(
            if (shape == RibbonShape.Full) {
                R.dimen.home_stat_pill_min_height
            } else {
                R.dimen.home_stat_pill_compact_min_height
            },
        )
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier =
            modifier
                .clayShadow(dimensionResource(R.dimen.elevation_card), MaterialTheme.shapes.medium)
                .heightIn(min = minHeight)
                .clearAndSetSemantics { contentDescription = description },
    ) {
        when (shape) {
            RibbonShape.Full -> FullPillContent(glyph = glyph, gilded = gilded, label = label, value = value)
            RibbonShape.Compact -> CompactPillContent(label = label, value = value)
            RibbonShape.Stacked -> StackedPillContent(label = label, value = value)
        }
    }
}

/** The pill with room for its glyph: glyph over label over value, centred (§4.2 band 2). */
@Composable
private fun FullPillContent(
    glyph: ImageVector,
    gilded: Boolean,
    label: String,
    value: String?,
) {
    Column(
        modifier = Modifier.padding(dimensionResource(R.dimen.spacing_sm)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
    ) {
        Icon(
            imageVector = glyph,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .then(if (gilded) Modifier.goldGradientFill() else Modifier)
                    .size(dimensionResource(R.dimen.home_stat_pill_glyph_size)),
        )
        PillLabel(label)
        PillValue(value)
    }
}

/**
 * The pill on the narrow class: label beside value, one line, no glyph (§4.2.3 part 2), centred as
 * a pair in the pill's content.
 *
 * **The label is never squeezed** (§4.2.6 rule 4). Both texts are measured at their own width, and
 * while the label, a `spacing_xs` gap and the value fit the pill's content they sit side by side.
 * A value too wide for what the label leaves — on today's numbers, a six-digit total between about
 * 1.07× and 1.12× font scale on a 360dp phone — moves under its label, in this pill alone: the one
 * case in which a value decides a pill's shape. The ribbon stacks as a whole long before a label
 * could fail to fit, because `HomeBands` reads the labels against half the pill ([ribbonShapeFor]).
 */
@Composable
private fun CompactPillContent(
    label: String,
    value: String?,
) {
    val gap = dimensionResource(R.dimen.spacing_xs)
    Layout(
        content = {
            PillLabel(label)
            PillValue(value)
        },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_sm)),
    ) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val (labelPlaceable, valuePlaceable) = measurables.map { it.measure(Constraints()) }
        val width = constraints.maxWidth
        val pairWidth = labelPlaceable.width + gapPx + valuePlaceable.width
        if (pairWidth <= width) {
            val height = max(max(labelPlaceable.height, valuePlaceable.height), constraints.minHeight)
            layout(width, height) {
                val start = Alignment.CenterHorizontally.align(pairWidth, width, layoutDirection)
                labelPlaceable.placeRelative(start, Alignment.CenterVertically.align(labelPlaceable.height, height))
                valuePlaceable.placeRelative(
                    start + labelPlaceable.width + gapPx,
                    Alignment.CenterVertically.align(valuePlaceable.height, height),
                )
            }
        } else {
            val stackHeight = labelPlaceable.height + gapPx + valuePlaceable.height
            val height = max(stackHeight, constraints.minHeight)
            layout(width, height) {
                val top = Alignment.CenterVertically.align(stackHeight, height)
                labelPlaceable.placeRelative(
                    Alignment.CenterHorizontally.align(labelPlaceable.width, width, layoutDirection),
                    top,
                )
                valuePlaceable.placeRelative(
                    Alignment.CenterHorizontally.align(valuePlaceable.width, width, layoutDirection),
                    top + labelPlaceable.height + gapPx,
                )
            }
        }
    }
}

/**
 * The narrow-class pill once any label no longer fits half its pill (§4.2.6 rule 2): the label on
 * its own line and the value centred under it, `spacing_xs` between them, `spacing_sm` padding and
 * no glyph — the full pill without its glyph. All three pills take it together, so they are one
 * height, the tallest's.
 */
@Composable
private fun StackedPillContent(
    label: String,
    value: String?,
) {
    Column(
        modifier = Modifier.padding(dimensionResource(R.dimen.spacing_sm)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
    ) {
        PillLabel(label)
        PillValue(value)
    }
}

/**
 * The pill's label — the same leaf in every shape, so no two can render it differently. One line,
 * never wrapped: a label is one word, and the ribbon's shape is what guarantees it fits (§4.2.6).
 */
@Composable
private fun PillLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = ONE_LINE,
        softWrap = false,
    )
}

/**
 * The pill's value, tabular so a changing digit cannot shift it, and one line, never wrapped, as
 * the label is. Placeholder rule unchanged.
 */
@Composable
private fun PillValue(value: String?) {
    Text(
        text = value ?: stringResource(R.string.stat_value_unknown),
        style = MaterialTheme.typography.titleMedium.withTabularFigures(),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = ONE_LINE,
        softWrap = false,
    )
}

/** A pill's label and its value are each one line (§4.2.6 rule 1). */
private const val ONE_LINE = 1
