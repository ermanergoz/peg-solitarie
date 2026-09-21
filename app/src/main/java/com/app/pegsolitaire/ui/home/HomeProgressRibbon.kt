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
 */
@Composable
internal fun HomeProgressRibbon(
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
            label = stringResource(R.string.home_stat_stars_label),
            value = totalStars?.toString(),
            modifier = Modifier.weight(CONTENT_WEIGHT),
        )
        StatPill(
            glyph = rememberClassicGlyph(),
            gilded = false,
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
 */
@Composable
private fun StatPill(
    glyph: ImageVector,
    gilded: Boolean,
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
) {
    val spokenValue = value ?: stringResource(R.string.stat_value_unknown_desc)
    val description = stringResource(R.string.home_stat_pill_desc, label, spokenValue)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier =
            modifier
                .clayShadow(dimensionResource(R.dimen.elevation_card), MaterialTheme.shapes.medium)
                .heightIn(min = dimensionResource(R.dimen.home_stat_pill_min_height))
                .clearAndSetSemantics { contentDescription = description },
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
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value ?: stringResource(R.string.stat_value_unknown),
                style = MaterialTheme.typography.titleMedium.withTabularFigures(),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
