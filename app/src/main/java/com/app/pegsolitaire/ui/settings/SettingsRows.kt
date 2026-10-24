package com.app.pegsolitaire.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.clayEntrance
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish

/**
 * One setting row: a soft-shadowed `surfaceContainerHigh` clay [Card] holding the [label]
 * + [supporting] copy beside a playful [Switch] (violet track when on, clay-neutral when
 * off, with the shared squish on the thumb). Fully hoisted — the row holds no state of its
 * own; the switch renders [checked] and reports every flip through [onCheckedChange]. The
 * switch carries [label] as its content description so screen readers announce which
 * setting it controls rather than a bare "switch". Rows enter with the shared clay
 * stagger ([entranceOrder]).
 */
@Composable
internal fun SettingsToggleRow(
    label: String,
    supporting: String,
    checked: Boolean,
    entranceOrder: Int,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier =
            Modifier
                .clayEntrance(entranceOrder)
                .clayShadow(dimensionResource(R.dimen.elevation_card), MaterialTheme.shapes.large)
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.settings_row_min_height)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.spacing_md)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                modifier =
                    Modifier
                        .claySquish()
                        .semantics { contentDescription = label },
            )
        }
    }
}

/**
 * One section label ("General" / "Privacy"), in the app's shipped caption style
 * (`labelLarge` / `onSurfaceVariant`, as `ResultsScreen`'s metric labels use).
 *
 * It joins the [clayEntrance] cascade rather than sitting outside it, so the stagger still
 * reads strictly top-to-bottom: a header that appeared instantly above rows that fade in
 * would break the one visual rule the ordering exists to keep.
 *
 * It is a heading (spec LS-AC01 as amended, brief A1.4), so TalkBack's heading navigation moves
 * section to section — on Settings and on the Licenses screen, which reuses this header.
 */
@Composable
internal fun SettingsSectionHeader(
    title: String,
    entranceOrder: Int,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .clayEntrance(entranceOrder)
                .padding(horizontal = dimensionResource(R.dimen.spacing_sm))
                .semantics { heading() },
    )
}

/**
 * One link row: the exact peer of [SettingsToggleRow] — same clay [Card] chrome, same inner
 * padding, typography and entrance stagger — differing in exactly one way: an optional
 * [trailingIcon] slot replaces the [Switch].
 *
 * The whole card is the tap target (`clickable` + [claySquish]), so the row clears the 48dp
 * minimum by a wide margin via `settings_row_min_height`; no icon-sized target is
 * introduced. Fully hoisted: the row owns no state and decides nothing — [onClick] is
 * whatever the screen passed.
 *
 * A `null` [onClick] makes the row **inert**, not merely unhandled (WR-06): neither
 * `clickable` nor [claySquish] is applied, so there is no click action for TalkBack to
 * announce as "double-tap to activate", no stop in keyboard/D-pad focus traversal, and no
 * press animation for a tap that would do nothing. An `onClick = {}` would leave all three
 * in place — an actionable control with no action, which is an accessibility defect. The
 * signed-in Play Games row (D-06) is the one caller.
 *
 * The inert branch still merges the card's descendants (brief A1.1, spec LS-06 as amended): a
 * `clickable` merges a tappable row by itself, and without the explicit merge an inert one was
 * read as two stops, label then supporting line. It adds no role and no action.
 *
 * [trailingIcon] is `null` for the manage-choices row on purpose. The UMP form opens
 * in-app while the policy leaves for a browser, so the glyph's presence or absence is what
 * makes the two rows honestly distinguishable at a glance rather than by label alone.
 */
@Composable
internal fun SettingsLinkRow(
    label: String,
    supporting: String,
    entranceOrder: Int,
    trailingIcon: (@Composable () -> Unit)?,
    onClick: (() -> Unit)?,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier =
            Modifier
                .clayEntrance(entranceOrder)
                .clayShadow(dimensionResource(R.dimen.elevation_card), MaterialTheme.shapes.large)
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.settings_row_min_height))
                .then(
                    if (onClick != null) {
                        Modifier.claySquish().clickable(onClick = onClick)
                    } else {
                        Modifier.semantics(mergeDescendants = true) {}
                    },
                ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.spacing_md)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            trailingIcon?.invoke()
        }
    }
}
