package com.app.pegsolitaire.ui.licenses

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
 * The section headers reuse Settings' header, headings included.
 *
 * The body is a plain `Column` in `verticalScroll`, not a lazy list (spec LS-15; the tech-lead's
 * re-run ruling, F-4, departure T4): every item is composed, so every section header is in the
 * semantics tree at every scroll position, where a lazy list would leave an off-screen header out.
 * Being in that tree is necessary for TalkBack's heading navigation, not sufficient: Compose hands
 * accessibility services only the nodes on screen, so TalkBack reaches an off-screen header only by
 * scrolling the list (the tech-lead's F-4 ruling). The list is twenty-three static items, so
 * composing them all at open costs little.
 *
 * A tappable entry plays the forward cue and opens its texts by **id** ([onOpenEntry]) or the
 * notices list ([onOpenGoogleNotices]); an inert entry does nothing. The entrance stagger covers
 * the first screenful ([LicensesUi.STAGGERED_ROWS]); later items, composed at open but below the
 * fold, enter plainly rather than after a long delay.
 *
 * Every text reads in its own direction and hangs from the layout's start edge, the right in an RTL
 * layout, so a card keeps its mirrored shape (spec LS-16).
 */
@Composable
fun LicensesScreen(
    sections: List<LicenseSection>,
    onOpenEntry: (String) -> Unit,
    onOpenGoogleNotices: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProvideLicencesText(LicencesTextEdge.LayoutStart) {
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
                Column(
                    modifier =
                        Modifier
                            .widthIn(max = dimensionResource(R.dimen.content_max_width))
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(dimensionResource(R.dimen.spacing_md))
                            .testTag(LICENSES_LIST_TAG),
                    verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
                ) {
                    LicensesItems(sections, onOpenEntry, onOpenGoogleNotices)
                }
            }
        }
    }
}

/** The intro, then each section's header over its entries, numbering the entrance in list order. */
@Composable
private fun ColumnScope.LicensesItems(
    sections: List<LicenseSection>,
    onOpenEntry: (String) -> Unit,
    onOpenGoogleNotices: () -> Unit,
) {
    var order = 0
    Text(
        text = stringResource(R.string.licenses_intro),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.licencesEntrance(order++),
    )
    sections.forEach { section ->
        SettingsSectionHeader(
            title = stringResource(section.title),
            entranceOrder = LicensesUi.entranceOrderOf(order++),
        )
        section.entries.forEach { entry ->
            LicenseEntryItem(entry, onOpenEntry, onOpenGoogleNotices, Modifier.licencesEntrance(order++))
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

/**
 * The clay entrance of the licences lists' item at [position] (0 = the intro), for the first
 * [LicensesUi.STAGGERED_ROWS] items only; later items enter plainly (spec F-23). Shared by both lists.
 */
@Composable
internal fun Modifier.licencesEntrance(position: Int): Modifier =
    LicensesUi.entranceOrderOf(position)?.let { clayEntrance(it) } ?: this

@Preview(name = "Licenses · light", showBackground = true)
@Preview(name = "Licenses · dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LicensesScreenPreview() {
    AppTheme {
        LicensesScreen(sections = LicenseCatalog.sections, onOpenEntry = {}, onOpenGoogleNotices = {}, onBack = {})
    }
}
