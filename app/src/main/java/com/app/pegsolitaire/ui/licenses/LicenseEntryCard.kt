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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.tintedCardColor
import com.app.pegsolitaire.ui.theme.tintedCardShadow
import com.app.pegsolitaire.ui.theme.tintedCardSupportingColor
import com.app.pegsolitaire.ui.theme.claySquish

/**
 * A tappable card's chevron, tagged so a test can find which side of its card it sits on (spec
 * LS-16: the chrome mirrors). The chevron stays decorative: the tag adds no description and no stop.
 */
internal const val LICENCE_CARD_CHEVRON_TAG = "licence_card_chevron"

/**
 * One clay card of the Licenses list (spec §3.1), the exact peer of a Settings row: the entry's
 * title over its lines, with no `maxLines` anywhere, so the card grows at any font scale.
 *
 * **Tappable** when [onClick] is set: a decorative chevron, the shared squish, and a `clickable`
 * with `Role.Button` and [clickLabel] ("Read the license"), so the role and the label carry by ear
 * what the chevron and the squish carry by eye (LS-06, LS-13). **Inert** when [onClick] is `null`:
 * neither, and no click action at all. Either way the card merges its own descendants (brief A1.1,
 * spec LS-06 as amended), so it is one TalkBack stop; the merge is the card's, not the click's.
 * The title and the lines are resolved once, and that one list is both what the card shows and the
 * label it speaks, title first, whichever of its texts is on screen (the tech-lead's F-4 ruling,
 * R2). An address line is plain text inside that one stop, never a link (LS-05) and not in a
 * `SelectionContainer`: that container is focusable, and on TalkBack 16 it split the address into a
 * stop of its own (device QA loop 2, F-1; spec §5.1, LS-AC06).
 */
@Composable
internal fun LicenseEntryCard(
    entry: LicenseEntry,
    onClick: (() -> Unit)?,
    clickLabel: String?,
    modifier: Modifier = Modifier,
) {
    val texts = listOf(stringResource(entry.title)) + entry.lines.map { licenseLineText(it) }
    LicenceCard(
        onClick = onClick,
        clickLabel = clickLabel,
        label = texts.joinToString(LicensesUi.CARD_LABEL_SEPARATOR),
        modifier = modifier,
    ) {
        LicenceCardTexts(texts)
    }
}

/**
 * The clay card chrome the Licenses entries and the Google notices rows share (spec §3.1, §4A):
 * `surfaceContainerHigh`, `shapes.large`, the clay shadow, at least `settings_row_min_height`. A
 * tappable card ([onClick] set) adds the squish, a `clickable` with `Role.Button` and [clickLabel],
 * and a decorative chevron; an inert one adds none of them. Every card merges its descendants into
 * one node, with any [semantics] of the caller's on that same node (a row's collection position).
 *
 * That node speaks [label], the card's whole text, as its own description. Compose hands TalkBack
 * a merging node's description as the node's first child, with the card's bounds, so the card is
 * read whole, title first, even when its title is scrolled out of the list's viewport; its texts
 * are hidden from accessibility ([LicenceCardTexts]), so none is read twice (the tech-lead's F-4
 * ruling, R2).
 */
@Composable
internal fun LicenceCard(
    onClick: (() -> Unit)?,
    clickLabel: String?,
    label: String,
    modifier: Modifier = Modifier,
    semantics: SemanticsPropertyReceiver.() -> Unit = {},
    lines: @Composable ColumnScope.() -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = tintedCardColor()),
        modifier =
            modifier
                .tintedCardShadow(MaterialTheme.shapes.large)
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
                ).semantics(mergeDescendants = true) {
                    contentDescription = label
                    semantics()
                },
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
                    tint = tintedCardSupportingColor(),
                    modifier =
                        Modifier
                            .size(dimensionResource(R.dimen.settings_row_icon_size))
                            .testTag(LICENCE_CARD_CHEVRON_TAG),
                )
            }
        }
    }
}

/**
 * A licences card's texts, as one resolved list: the first is its title (`titleMedium`,
 * `onSurface`), the rest its lines (`bodyMedium`, `onSurfaceVariant`). Each is hidden from
 * accessibility services, because the card speaks the same list as its label ([LicenceCard]):
 * TalkBack reads the label once and never these texts again (the tech-lead's F-4 ruling, R2).
 * Shared by the Licenses entries and the Google notices rows.
 */
@Composable
internal fun LicenceCardTexts(texts: List<String>) {
    texts.forEachIndexed { index, text ->
        val isTitle = index == 0
        Text(
            text = text,
            style = if (isTitle) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (isTitle) MaterialTheme.colorScheme.onSurface else tintedCardSupportingColor(),
            modifier = Modifier.semantics { hideFromAccessibility() },
        )
    }
}

/** The text of one line under an entry's title, resolved from its resources. */
@Composable
private fun licenseLineText(line: LicenseLine): String =
    when (line) {
        is LicenseLine.Plain -> stringResource(line.text)
        is LicenseLine.SoundCount -> pluralStringResource(R.plurals.licenses_sound_effects, line.count, line.count)
        is LicenseLine.Address -> stringResource(line.text)
    }
