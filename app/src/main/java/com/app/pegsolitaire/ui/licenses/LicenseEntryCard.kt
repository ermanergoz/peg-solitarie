package com.app.pegsolitaire.ui.licenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish

/**
 * One clay card of the Licenses list (spec §3.1), the exact peer of a Settings row: the entry's
 * title over its lines, with no `maxLines` anywhere, so the card grows at any font scale.
 *
 * **Tappable** when [onClick] is set: a decorative chevron, the shared squish, and a `clickable`
 * with `Role.Button` and [clickLabel] ("Read the license"), so the role and the label carry by ear
 * what the chevron and the squish carry by eye (LS-06, LS-13). **Inert** when [onClick] is `null`:
 * neither, and no click action at all. Either way the card merges its own descendants (brief A1.1,
 * spec LS-06 as amended), so it is one TalkBack stop read title first; the merge is the card's,
 * not the click's. An address line is plain text inside that one stop, never a link (LS-05) and not
 * in a `SelectionContainer`: that container is focusable, and on TalkBack 16 it split the address
 * into a stop of its own (device QA loop 2, F-1; spec §5.1, LS-AC06).
 */
@Composable
internal fun LicenseEntryCard(
    entry: LicenseEntry,
    onClick: (() -> Unit)?,
    clickLabel: String?,
    modifier: Modifier = Modifier,
) {
    LicenceCard(onClick = onClick, clickLabel = clickLabel, modifier = modifier) {
        Text(
            text = stringResource(entry.title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        entry.lines.forEach { line -> LicenseLineText(line) }
    }
}

/**
 * The clay card chrome the Licenses entries and the Google notices rows share (spec §3.1, §4A):
 * `surfaceContainerHigh`, `shapes.large`, the clay shadow, at least `settings_row_min_height`. A
 * tappable card ([onClick] set) adds the squish, a `clickable` with `Role.Button` and [clickLabel],
 * and a decorative chevron; an inert one adds none of them. Every card merges its descendants into
 * one node, with any [semantics] of the caller's on that same node (a row's collection position).
 */
@Composable
internal fun LicenceCard(
    onClick: (() -> Unit)?,
    clickLabel: String?,
    modifier: Modifier = Modifier,
    semantics: SemanticsPropertyReceiver.() -> Unit = {},
    lines: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier =
            modifier
                .clayShadow(dimensionResource(R.dimen.elevation_card), MaterialTheme.shapes.large)
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.settings_row_min_height))
                .then(
                    if (onClick != null) {
                        Modifier
                            .claySquish()
                            .clickable(role = Role.Button, onClickLabel = clickLabel, onClick = onClick)
                    } else {
                        Modifier
                    },
                ).semantics(mergeDescendants = true, properties = semantics),
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
                content = lines,
            )
            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(dimensionResource(R.dimen.settings_row_icon_size)),
                )
            }
        }
    }
}

/** One line under an entry's title (`bodyMedium`, `onSurfaceVariant`). */
@Composable
private fun LicenseLineText(line: LicenseLine) {
    val style = MaterialTheme.typography.bodyMedium
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    when (line) {
        is LicenseLine.Plain -> Text(text = stringResource(line.text), style = style, color = color)
        is LicenseLine.SoundCount ->
            Text(
                text = pluralStringResource(R.plurals.licenses_sound_effects, line.count, line.count),
                style = style,
                color = color,
            )
        is LicenseLine.Address -> Text(text = stringResource(line.text), style = style, color = color)
    }
}
