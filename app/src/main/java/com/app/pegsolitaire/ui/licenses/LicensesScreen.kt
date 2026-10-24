package com.app.pegsolitaire.ui.licenses

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.app.pegsolitaire.R
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.audio.withClickSound
import com.app.pegsolitaire.ui.settings.SettingsSectionHeader
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.clayEntrance

/** The Licenses list's test tag: the node that carries the scroll actions (brief A1.6). */
internal const val LICENSES_LIST_TAG = "licenses_list"

/**
 * The Licenses screen (spec §3; tech design §3.6): the catalogue's four sections over clay entry
 * cards, under the intro.
 *
 * Stateless and fed the static catalogue — it has no ViewModel (LS-09, tech design §3.1's recorded
 * exception): no data source, no async work, no event beyond navigation. Its only state is the
 * list's saveable scroll position, so the position survives the round trip to a text (LS-AC02).
 * Items are keyed by entry id; the section headers reuse Settings' header, headings included.
 *
 * A tappable entry plays the forward cue and opens its texts by **id** ([onOpenEntry]) or the
 * notices list ([onOpenGoogleNotices]); an inert entry does nothing. The entrance stagger covers
 * the first screenful ([LicensesUi.STAGGERED_ROWS]); later items, composed as they scroll in, enter
 * plainly rather than after a long delay.
 */
@Composable
fun LicensesScreen(
    sections: List<LicenseSection>,
    onOpenEntry: (String) -> Unit,
    onOpenGoogleNotices: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { LicensesTopBar(title = stringResource(R.string.licenses_title), onBack = onBack) },
    ) { contentPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                state = rememberLazyListState(),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = dimensionResource(R.dimen.content_max_width))
                        .testTag(LICENSES_LIST_TAG),
                contentPadding = PaddingValues(dimensionResource(R.dimen.spacing_md)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
            ) {
                licensesItems(sections, onOpenEntry, onOpenGoogleNotices)
            }
        }
    }
}

/** The intro, then each section's header over its entries, numbering the entrance in list order. */
private fun LazyListScope.licensesItems(
    sections: List<LicenseSection>,
    onOpenEntry: (String) -> Unit,
    onOpenGoogleNotices: () -> Unit,
) {
    var order = 0
    val introOrder = order++
    item(key = INTRO_KEY) {
        Text(
            text = stringResource(R.string.licenses_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.staggered(introOrder),
        )
    }
    sections.forEach { section ->
        val headerOrder = order++
        item(key = "$HEADER_KEY_PREFIX${section.title}") {
            SettingsSectionHeader(title = stringResource(section.title), entranceOrder = headerOrder)
        }
        section.entries.forEach { entry ->
            val entryOrder = order++
            item(key = entry.id) {
                LicenseEntryItem(entry, onOpenEntry, onOpenGoogleNotices, Modifier.staggered(entryOrder))
            }
        }
    }
}

/** One entry card, wired to what its target opens, with the forward cue at this call site (D-09). */
@Composable
private fun LicenseEntryItem(
    entry: LicenseEntry,
    onOpenEntry: (String) -> Unit,
    onOpenGoogleNotices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openEntry = remember(entry.id, onOpenEntry) { { onOpenEntry(entry.id) } }
    when (entry.opens) {
        is LicenseEntryTarget.Texts ->
            LicenseEntryCard(
                entry = entry,
                onClick = withClickSound(SoundCue.ButtonForward, openEntry),
                clickLabel = stringResource(R.string.licenses_read_action),
                modifier = modifier,
            )
        LicenseEntryTarget.GoogleNotices ->
            LicenseEntryCard(
                entry = entry,
                onClick = withClickSound(SoundCue.ButtonForward, onOpenGoogleNotices),
                clickLabel = stringResource(R.string.licenses_google_notices_action),
                modifier = modifier,
            )
        null -> LicenseEntryCard(entry = entry, onClick = null, clickLabel = null, modifier = modifier)
    }
}

/** The clay entrance for the first screenful only; later items enter plainly. */
@Composable
private fun Modifier.staggered(order: Int): Modifier =
    if (order < LicensesUi.STAGGERED_ROWS) clayEntrance(order) else this

private const val INTRO_KEY = "intro"
private const val HEADER_KEY_PREFIX = "header-"

@Preview(name = "Licenses · light", showBackground = true)
@Preview(name = "Licenses · dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LicensesScreenPreview() {
    AppTheme {
        LicensesScreen(sections = LicenseCatalog.sections, onOpenEntry = {}, onOpenGoogleNotices = {}, onBack = {})
    }
}
