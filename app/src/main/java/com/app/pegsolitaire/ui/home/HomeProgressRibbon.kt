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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.ui.theme.clayEntrance
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.goldGradientFill
import com.app.pegsolitaire.ui.theme.withTabularFigures

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
 * ## The compact pill — the last yield, and it costs no readout (§4.2.3, D-03k)
 *
 * [compact] is the **fourth** compact step, taken on the narrow class together with the other
 * three and never partially, and decided by `HomeBands` from the width and height available to it
 * — never here. It drops the pill's decorative glyph and puts the label beside the value on one
 * line, which is the whole of it: three pills, three labels, three values, the same equal widths,
 * the same clay surface, the same entrance order, still not tappable, and **one**
 * `clearAndSetSemantics` node per pill speaking the same sentence, byte for byte. The glyph is
 * `contentDescription = null` decoration inside that node, so it carries no information at all —
 * TalkBack cannot tell the two shapes apart, and criterion 15m is the test that says so.
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
 * @param compact the compact pill shape — the narrow class only (§4.2.3 part 2).
 */
@Composable
internal fun HomeProgressRibbon(
    compact: Boolean,
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
            compact = compact,
            label = stringResource(R.string.home_stat_stars_label),
            value = totalStars?.toString(),
            modifier = Modifier.weight(CONTENT_WEIGHT),
        )
        StatPill(
            glyph = rememberClassicGlyph(),
            gilded = false,
            compact = compact,
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
            compact = compact,
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
 * ## Two shapes, one sentence
 *
 * [compact] selects the §4.2.3 compact pill. The description is computed **before** the branch and
 * the label and the value are the same two leaves in both shapes ([PillLabel], [PillValue]), so
 * the spoken sentence and the rendered copy cannot drift between them — the same reason
 * `HomeHero`'s two shapes share `HeroTitle` and `SettingsGear`. The minimum height is the only
 * other thing that differs, and both are floors: neither text is ever truncated or scaled down, so
 * a font scale that does not fit grows the pill, then the band, and the column scrolls
 * (criterion 14).
 */
@Composable
private fun StatPill(
    glyph: ImageVector,
    gilded: Boolean,
    compact: Boolean,
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
) {
    val spokenValue = value ?: stringResource(R.string.stat_value_unknown_desc)
    val description = stringResource(R.string.home_stat_pill_desc, label, spokenValue)
    val minHeight =
        dimensionResource(
            if (compact) {
                R.dimen.home_stat_pill_compact_min_height
            } else {
                R.dimen.home_stat_pill_min_height
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
        if (compact) {
            CompactPillContent(label = label, value = value)
        } else {
            FullPillContent(glyph = glyph, gilded = gilded, label = label, value = value)
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
 * The pill on the narrow class: label beside value, one line, no glyph (§4.2.3 part 2).
 *
 * **The label is the weighted child and the value is not**, which is what keeps the promise that
 * neither is truncated. A `Row` measures its unweighted children first, so the value takes its
 * natural width and can never be squeezed; the label then receives what is left and **wraps**
 * inside it, growing the pill and the band rather than clipping — `fill = false` so it asks for no
 * more than it needs, which is what lets the pair stay centred when there is room. That weight is
 * one share of a `Row`'s free *width* beside a fixed sibling, which is what `CONTENT_WEIGHT` is
 * for; §4.2.2 clause 4's ban on weighting is the band column's **vertical** axis and is untouched.
 */
@Composable
private fun CompactPillContent(
    label: String,
    value: String?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.spacing_sm)),
        horizontalArrangement =
            Arrangement.spacedBy(
                dimensionResource(R.dimen.spacing_xs),
                Alignment.CenterHorizontally,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PillLabel(label, modifier = Modifier.weight(CONTENT_WEIGHT, fill = false))
        PillValue(value)
    }
}

/** The pill's label — the same leaf in both shapes, so the two cannot render it differently. */
@Composable
private fun PillLabel(
    label: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

/** The pill's value, tabular so a changing digit cannot shift it. Placeholder rule unchanged. */
@Composable
private fun PillValue(value: String?) {
    Text(
        text = value ?: stringResource(R.string.stat_value_unknown),
        style = MaterialTheme.typography.titleMedium.withTabularFigures(),
        color = MaterialTheme.colorScheme.onSurface,
    )
}
